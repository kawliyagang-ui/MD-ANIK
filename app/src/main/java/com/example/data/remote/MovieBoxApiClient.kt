package com.example.data.remote

import com.example.data.model.MediaItem
import com.example.data.model.MovieBoxDownloadBundle
import com.example.data.model.MovieBoxDownloadOption
import com.example.data.model.MovieBoxLanguageGroup
import com.example.data.model.MovieBoxSubject
import com.example.data.model.MovieBoxSubtitleOption
import com.example.data.model.PreferredDownloadQuality
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.TreeMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

object MovieBoxApiClient {
    const val DEFAULT_HOST = "https://api4sg.aoneroom.com"

    val MOBILE_BACKEND_HOSTS = listOf(
        "https://api4sg.aoneroom.com",
        "https://api6.aoneroom.com",
        "https://api.inmoviebox.com",
        "https://api3.aoneroom.com",
        "https://api7.aoneroom.com"
    )

    val H5_MIRROR_HOSTS = listOf(
        "https://h5.inmoviebox.com",
        "https://123movienow.cc",
        "https://fmoviesunblocked.net",
        "https://movieboxapp.in",
        "https://themoviebox.xyz",
        "https://movie-box.co",
        "https://moviebox.ph"
    )

    val MIRROR_HOSTS = MOBILE_BACKEND_HOSTS + listOf(
        "https://h5.inmoviebox.com",
        "https://123movienow.cc"
    )

    private val LEGACY_BROKEN_HOSTS = setOf(
        "https://h5-api.aoneroom.com",
        "https://h5.aoneroom.com",
        "https://moviebox.ng"
    )

    private val BAIT_URL_MARKERS = listOf(
        "b164fbfb4347792950bdfbfb563d39d9",
        "80da52722ee7de7a4303290af6783424",
        "2022/08/11/5970c1ffed2920c3b2d80da029fe3944",
        "fzmovies",
        "macdn.aoneroom.com",
        "/other/"
    )

    private const val GSLB_CONVERT_URL = "https://gslb.shalltry.com/gslb/domain/convert"
    private const val H5_UNLOCK_API_HOST = "https://h5-api.aoneroom.com"
    private const val VIDEODOWNLOADER_REFERER = MovieBoxDownloadOption.DEFAULT_REFERER
    private const val VIDEODOWNLOADER_ORIGIN = "https://videodownloader.site"
    private const val H5_UNLOCK_CLIENT_INFO = "{\"timezone\":\"Africa/Nairobi\"}"
    private const val H5_SESSION_CLIENT_INFO = "{\"timezone\":\"UTC\"}"
    private const val H5_DESKTOP_USER_AGENT = MovieBoxDownloadOption.DEFAULT_USER_AGENT

    private const val MOBILE_USER_AGENT =
        "com.community.oneroom/50020117 (Linux; U; Android 13; en_US; 23078RKD5C; Build/TQ2A.230405.003; Cronet/135.0.7012.3)"
    private const val HMAC_SECRET_BASE64 = "76iRl07s0xSN9jqmEWAt79EBJZulIQIsV64FZr2O"
    private val hmacSecretBytes: ByteArray by lazy {
        HMAC_SECRET_BASE64.decodeBase64()?.toByteArray()
            ?: HMAC_SECRET_BASE64.toByteArray(Charsets.UTF_8)
    }

    private val mobileClientInfoHeader: String by lazy {
        val randomBytes = ByteArray(8).also { SecureRandom().nextBytes(it) }
        val deviceId = randomBytes.joinToString("") { "%02x".format(Locale.ROOT, it) }
        val gaid = UUID.randomUUID().toString()
        "{\"package_name\":\"com.community.oneroom\",\"version_name\":\"4.0.01.0813.03\",\"version_code\":50020117,\"os\":\"android\",\"os_version\":\"13\",\"install_ch\":\"ps\",\"device_id\":\"$deviceId\",\"install_store\":\"ps\",\"gaid\":\"$gaid\",\"brand\":\"Redmi\",\"model\":\"23078RKD5C\",\"system_language\":\"en\",\"net\":\"NETWORK_WIFI\",\"region\":\"US\",\"timezone\":\"Asia/Kolkata\",\"sp_code\":\"40401\",\"X-Play-Mode\":\"2\"}"
    }

    private val JSON_MEDIA_TYPE = "application/json".toMediaType()

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val mapAdapter = moshi.adapter<Map<String, Any?>>(
        Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    )

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()
    private val h5TokenByHost = ConcurrentHashMap<String, String>()
    private val mobileTokenByHost = ConcurrentHashMap<String, String>()
    @Volatile
    private var cachedGslbHosts: List<String> = emptyList()
    @Volatile
    private var lastGslbRefreshMs: Long = 0L

    private val inMemoryCookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val hostCookies = cookieStore.getOrPut(url.host) { mutableListOf() }
            synchronized(hostCookies) {
                for (cookie in cookies) {
                    hostCookies.removeAll { it.name == cookie.name }
                    if (cookie.expiresAt >= System.currentTimeMillis()) {
                        hostCookies.add(cookie)
                    }
                    if (cookie.name == "token" && cookie.value.isNotBlank()) {
                        val origin = "${url.scheme}://${url.host}"
                        h5TokenByHost.putIfAbsent(origin, cookie.value)
                    }
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val hostCookies = cookieStore[url.host] ?: return emptyList()
            val now = System.currentTimeMillis()
            synchronized(hostCookies) {
                hostCookies.removeAll { it.expiresAt < now }
                return hostCookies.filter { it.matches(url) }
            }
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(inMemoryCookieJar)
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun normalizeHostUrl(raw: String?): String {
        val trimmed = raw?.trim().orEmpty().trimEnd('/')
        if (trimmed.isBlank()) return DEFAULT_HOST
        val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return withScheme.trimEnd('/')
    }

    fun upgradeLegacyHostIfNeeded(raw: String?): String {
        val normalized = normalizeHostUrl(raw)
        return if (normalized in LEGACY_BROKEN_HOSTS) DEFAULT_HOST else normalized
    }

    fun isMobileBackendHost(hostUrl: String): Boolean {
        val hostOnly = runCatching { URI(normalizeHostUrl(hostUrl)).host.orEmpty() }
            .getOrDefault("")
            .lowercase(Locale.ROOT)
        if (hostOnly.isBlank()) return false
        if (hostOnly.startsWith("h5.") || hostOnly.startsWith("h5-")) return false
        return hostOnly.endsWith("aoneroom.com") || hostOnly.endsWith("inmoviebox.com")
    }

    fun isBaitOrBlockedUrl(url: String): Boolean {
        val clean = url.trim().lowercase(Locale.ROOT)
        if (clean.isBlank()) return true
        return BAIT_URL_MARKERS.any { clean.contains(it) }
    }

    fun candidateHosts(customHost: String?): List<String> {
        val custom = customHost?.trim().orEmpty()
        val list = buildList {
            if (custom.isNotBlank()) {
                val normalized = normalizeHostUrl(custom)
                if (normalized !in LEGACY_BROKEN_HOSTS) {
                    add(normalized)
                }
            }
            addAll(MOBILE_BACKEND_HOSTS)
            addAll(cachedGslbHosts)
            addAll(H5_MIRROR_HOSTS)
            if (custom.isNotBlank()) {
                val normalized = normalizeHostUrl(custom)
                if (normalized in LEGACY_BROKEN_HOSTS) {
                    add(normalized)
                }
            }
        }
        return list.distinct()
    }

    fun computeClientToken(timestampMillis: Long): String {
        val reversedMd5 = md5Hex(timestampMillis.toString().reversed().toByteArray(Charsets.UTF_8))
        return "$timestampMillis,$reversedMd5"
    }

    fun computeTrSignature(
        method: String,
        url: String,
        body: String? = null,
        timestampMillis: Long
    ): String {
        val uri = runCatching { URI(url) }.getOrNull()
        val rawPath = uri?.rawPath?.takeIf { it.isNotBlank() } ?: run {
            val noScheme = url.substringAfter("://", url)
            val idx = noScheme.indexOf('/')
            if (idx >= 0) noScheme.substring(idx).substringBefore('?') else "/"
        }
        val rawQuery = uri?.rawQuery
            ?: url.substringAfter('?', "").takeIf { url.contains('?') && it.isNotBlank() }

        val canonicalPathAndQuery = if (!rawQuery.isNullOrBlank()) {
            val sortedParams = TreeMap<String, MutableList<String>>()
            for (pair in rawQuery.split('&')) {
                if (pair.isEmpty()) continue
                val eqIdx = pair.indexOf('=')
                val kRaw = if (eqIdx >= 0) pair.substring(0, eqIdx) else pair
                val vRaw = if (eqIdx >= 0) pair.substring(eqIdx + 1) else ""
                val k = runCatching { URLDecoder.decode(kRaw, "UTF-8") }.getOrDefault(kRaw)
                val v = runCatching { URLDecoder.decode(vRaw, "UTF-8") }.getOrDefault(vRaw)
                sortedParams.getOrPut(k) { mutableListOf() }.add(v)
            }
            val joined = buildList {
                for ((k, values) in sortedParams) {
                    for (v in values) {
                        add("$k=$v")
                    }
                }
            }.joinToString("&")
            if (joined.isNotEmpty()) "$rawPath?$joined" else rawPath
        } else {
            rawPath
        }

        val bodyBytes = if (!body.isNullOrEmpty()) body.toByteArray(Charsets.UTF_8) else ByteArray(0)
        val lenStr = if (bodyBytes.isNotEmpty()) bodyBytes.size.toString() else ""
        val bodyMd5Hex = if (bodyBytes.isNotEmpty()) {
            val capped = minOf(bodyBytes.size, 102_400)
            md5Hex(bodyBytes, 0, capped)
        } else {
            ""
        }

        val canonicalString = listOf(
            method.uppercase(Locale.ROOT),
            "application/json",
            "application/json",
            lenStr,
            timestampMillis.toString(),
            bodyMd5Hex,
            canonicalPathAndQuery
        ).joinToString("\n")

        val mac = Mac.getInstance("HmacMD5")
        mac.init(SecretKeySpec(hmacSecretBytes, "HmacMD5"))
        val digestBytes = mac.doFinal(canonicalString.toByteArray(Charsets.UTF_8))
        val sigBase64 = digestBytes.toByteString().base64()
        return "$timestampMillis|2|$sigBase64"
    }

    suspend fun searchSubjects(
        query: String,
        mediaType: String = "all",
        page: Int = 1,
        perPage: Int = 20,
        customHost: String = ""
    ): Result<List<MovieBoxSubject>> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Enter a movie or series title to search.")
            )
        }
        val subjectType = when {
            mediaType.equals("movie", ignoreCase = true) -> 1
            mediaType.equals("tv", ignoreCase = true) -> 2
            else -> 0
        }
        val payloadJson = mapAdapter.toJson(
            mapOf(
                "keyword" to cleanQuery,
                "page" to page.coerceAtLeast(1),
                "perPage" to perPage.coerceIn(1, 30),
                "subjectType" to subjectType
            )
        )
        val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
        var lastError: Throwable? = null

        refreshGslbHostsIfNeeded(force = false)

        for (host in candidateHosts(customHost)) {
            if (isMobileBackendHost(host)) {
                val mobileEndpoint = "$host/wefeed-mobile-bff/subject-api/search"
                runCatching {
                    val (code, body) = executeMobileSigned(
                        host = host,
                        url = mobileEndpoint,
                        postJson = payloadJson
                    )
                    if (code !in 200..299) {
                        throw IllegalStateException("HTTP $code from $host")
                    }
                    val parsed = parseSearchResponseJson(body, host)
                    if (parsed.isNotEmpty() || isSuccessfulApiPayload(body)) {
                        return@withContext Result.success(deduplicateSearchSubjects(parsed))
                    }
                }.onFailure { lastError = it }
                continue
            }

            val postEndpoints = listOf(
                "$host/wefeed-h5api-bff/subject/search",
                "$host/wefeed-h5-api/subject/search"
            )
            for (endpoint in postEndpoints) {
                runCatching {
                    val (code, body) = executeH5WithSession(
                        host = host,
                        url = endpoint,
                        referer = "$host/",
                        postJson = payloadJson
                    )
                    if (code !in 200..299) {
                        throw IllegalStateException("HTTP $code from $host")
                    }
                    val parsed = parseSearchResponseJson(body, host)
                    if (parsed.isNotEmpty() || isSuccessfulApiPayload(body)) {
                        return@withContext Result.success(deduplicateSearchSubjects(parsed))
                    }
                }.onFailure { lastError = it }
            }

            val getEndpoints = listOf(
                "$host/api/search?keyword=$encodedQuery&q=$encodedQuery&page=${page.coerceAtLeast(1)}",
                "$host/search?keyword=$encodedQuery"
            )
            for (endpoint in getEndpoints) {
                runCatching {
                    val (code, body) = executeH5WithSession(
                        host = host,
                        url = endpoint,
                        referer = "$host/"
                    )
                    if (code !in 200..299) {
                        throw IllegalStateException("HTTP $code from $host")
                    }
                    val parsed = parseSearchResponseJson(body, host)
                    if (parsed.isNotEmpty()) {
                        return@withContext Result.success(deduplicateSearchSubjects(parsed))
                    }
                }.onFailure { lastError = it }
            }
        }

        Result.failure(
            lastError ?: IllegalStateException(
                "Download server unreachable. Check your network and try again."
            )
        )
    }

    suspend fun resolveSubjectForMedia(
        media: MediaItem,
        customHost: String = ""
    ): Result<MovieBoxSubject> {
        val searchResult = searchSubjects(
            query = media.title,
            mediaType = media.mediaType,
            customHost = customHost
        )
        val candidates = searchResult.getOrNull().orEmpty()
        if (candidates.isNotEmpty()) {
            val best = pickBestSubjectCandidate(media, candidates)
            if (best != null) return Result.success(best)
        }

        if (!media.mediaType.equals("all", ignoreCase = true)) {
            val broadResult = searchSubjects(
                query = media.title,
                mediaType = "all",
                customHost = customHost
            )
            val broadCandidates = broadResult.getOrNull().orEmpty()
            if (broadCandidates.isNotEmpty()) {
                val best = pickBestSubjectCandidate(media, broadCandidates)
                if (best != null) return Result.success(best)
            }
            return broadResult.fold(
                onSuccess = {
                    Result.failure(NoSuchElementException("No downloadable source found for \"${media.title}\"."))
                },
                onFailure = { Result.failure(it) }
            )
        }

        return searchResult.fold(
            onSuccess = {
                Result.failure(NoSuchElementException("No downloadable source found for \"${media.title}\"."))
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun resolveMultiLanguageDownloadBundle(
        media: MediaItem,
        season: Int = 1,
        episode: Int = 1,
        customHost: String = ""
    ): Result<MovieBoxDownloadBundle> = withContext(Dispatchers.IO) {
        val primarySearch = searchSubjects(
            query = media.title,
            mediaType = media.mediaType,
            customHost = customHost
        )
        var candidates = primarySearch.getOrNull().orEmpty()
        if (candidates.isEmpty() && !media.mediaType.equals("all", ignoreCase = true)) {
            candidates = searchSubjects(
                query = media.title,
                mediaType = "all",
                customHost = customHost
            ).getOrNull().orEmpty()
        }

        if (candidates.isEmpty()) {
            val err = primarySearch.exceptionOrNull()
                ?: NoSuchElementException("No downloadable source found for \"${media.title}\".")
            return@withContext Result.failure(err)
        }

        val bestSubject = pickBestSubjectCandidate(media, candidates)
            ?: return@withContext Result.failure(
                NoSuchElementException("No downloadable source found for \"${media.title}\".")
            )

        val isTv = media.mediaType.equals("tv", ignoreCase = true) || bestSubject.subjectType == 2
        val matchingVariants = collectSameTitleLanguageVariants(media, bestSubject, candidates)
        val detailDubs = fetchSubjectDetailDubs(bestSubject, customHost)

        val combinedSubjects = buildList {
            add(bestSubject)
            addAll(detailDubs)
            addAll(matchingVariants)
        }.distinctBy { it.subjectId }.take(6)

        if (combinedSubjects.size <= 1) {
            return@withContext fetchDownloadBundle(
                subject = bestSubject,
                season = season,
                episode = episode,
                isTv = isTv,
                customHost = customHost
            )
        }

        val results = coroutineScope {
            combinedSubjects.map { subj ->
                async {
                    subj to fetchDownloadBundle(
                        subject = subj,
                        season = season,
                        episode = episode,
                        isTv = isTv,
                        customHost = customHost
                    ).getOrNull()
                }
            }.awaitAll()
        }

        val successfulBundles = results.mapNotNull { (subj, bundle) ->
            bundle?.takeIf { it.options.isNotEmpty() }?.let { subj to it }
        }
        if (successfulBundles.isEmpty()) {
            return@withContext fetchDownloadBundle(
                subject = bestSubject,
                season = season,
                episode = episode,
                isTv = isTv,
                customHost = customHost
            )
        }

        return@withContext Result.success(
            mergeLanguageBundles(
                primarySubject = bestSubject,
                bundlesWithSubjects = successfulBundles,
                season = if (isTv) season.coerceAtLeast(1) else 1,
                episode = if (isTv) episode.coerceAtLeast(1) else 1
            )
        )
    }

    suspend fun fetchMultiLanguageDownloadBundleForSubject(
        subject: MovieBoxSubject,
        season: Int = 1,
        episode: Int = 1,
        isTv: Boolean = subject.subjectType == 2,
        customHost: String = ""
    ): Result<MovieBoxDownloadBundle> = withContext(Dispatchers.IO) {
        val detailDubs = fetchSubjectDetailDubs(subject, customHost)
        val subjects = (listOf(subject) + detailDubs).distinctBy { it.subjectId }.take(6)
        if (subjects.size <= 1) {
            return@withContext fetchDownloadBundle(
                subject = subject,
                season = season,
                episode = episode,
                isTv = isTv,
                customHost = customHost
            )
        }

        val results = coroutineScope {
            subjects.map { subj ->
                async {
                    subj to fetchDownloadBundle(
                        subject = subj,
                        season = season,
                        episode = episode,
                        isTv = isTv,
                        customHost = customHost
                    ).getOrNull()
                }
            }.awaitAll()
        }

        val successful = results.mapNotNull { (subj, b) ->
            b?.takeIf { it.options.isNotEmpty() }?.let { subj to it }
        }
        if (successful.isEmpty()) {
            return@withContext fetchDownloadBundle(
                subject = subject,
                season = season,
                episode = episode,
                isTv = isTv,
                customHost = customHost
            )
        }

        Result.success(
            mergeLanguageBundles(
                primarySubject = subject,
                bundlesWithSubjects = successful,
                season = if (isTv) season.coerceAtLeast(1) else 1,
                episode = if (isTv) episode.coerceAtLeast(1) else 1
            )
        )
    }

    suspend fun fetchDownloadBundle(
        subject: MovieBoxSubject,
        season: Int = 1,
        episode: Int = 1,
        isTv: Boolean = subject.subjectType == 2,
        customHost: String = ""
    ): Result<MovieBoxDownloadBundle> = withContext(Dispatchers.IO) {
        val safeSeason = if (isTv) season.coerceAtLeast(1) else 0
        val safeEpisode = if (isTv) episode.coerceAtLeast(1) else 0
        var resolvedDetailPath = subject.detailPath
        val encodedId = URLEncoder.encode(subject.subjectId, "UTF-8")

        val hosts = buildList {
            if (subject.sourceHost.isNotBlank()) {
                val norm = normalizeHostUrl(subject.sourceHost)
                if (norm !in LEGACY_BROKEN_HOSTS) add(norm)
            }
            addAll(candidateHosts(customHost))
        }.distinct()

        val mergedOptions = mutableListOf<MovieBoxDownloadOption>()
        val mergedSubtitles = mutableListOf<MovieBoxSubtitleOption>()
        val discoveredResourceIds = linkedSetOf<String>()
        val discoveredStreamIds = linkedSetOf<String>()
        var winningSourceHost = ""
        var lastError: Throwable? = null

        // Phase 1: OpenStream Mobile BFF (/wefeed-mobile-bff/subject-api/*) for metadata, streams & captions
        for (host in hosts.filter { isMobileBackendHost(it) }) {
            if (resolvedDetailPath.isBlank()) {
                resolvedDetailPath = fetchMobileDetailPath(host, subject).orEmpty()
            }

            val mobileEndpoints = listOf(
                "$host/wefeed-mobile-bff/subject-api/resource/v2?subjectId=$encodedId&se=$safeSeason&ep=$safeEpisode&isVip=true",
                "$host/wefeed-mobile-bff/subject-api/play-info?subjectId=$encodedId&se=$safeSeason&ep=$safeEpisode",
                "$host/wefeed-mobile-bff/subject-api/play-info/v2?subjectId=$encodedId&se=$safeSeason&ep=$safeEpisode"
            )

            var hostHadSuccess = false
            for (endpoint in mobileEndpoints) {
                runCatching {
                    val (code, body) = executeMobileSigned(host = host, url = endpoint)
                    if (code !in 200..299) {
                        throw IllegalStateException("HTTP $code from $host")
                    }
                    hostHadSuccess = true
                    collectIdsFromJson(body, discoveredResourceIds, discoveredStreamIds)
                    val parsed = parseDownloadResponseJson(
                        json = body,
                        subject = subject.copy(detailPath = resolvedDetailPath),
                        sourceHost = host,
                        season = if (isTv) safeSeason else 1,
                        episode = if (isTv) safeEpisode else 1,
                        refererOverride = VIDEODOWNLOADER_REFERER,
                        userAgentOverride = H5_DESKTOP_USER_AGENT
                    )
                    if (parsed.options.isNotEmpty() && winningSourceHost.isBlank()) {
                        winningSourceHost = host
                    }
                    mergedOptions.addAll(parsed.options)
                    mergedSubtitles.addAll(parsed.subtitles)
                }.onFailure { lastError = it }
            }

            // Fetch real subtitles from Mobile BFF caption endpoints
            if (hostHadSuccess || mergedSubtitles.isEmpty()) {
                val captionEndpoints = buildList {
                    add("$host/wefeed-mobile-bff/subject-api/subtitle-search?subjectId=$encodedId&se=$safeSeason&ep=$safeEpisode")
                    for (rid in discoveredResourceIds.take(2)) {
                        val encRid = URLEncoder.encode(rid, "UTF-8")
                        add("$host/wefeed-mobile-bff/subject-api/get-ext-captions?subjectId=$encodedId&resourceId=$encRid")
                    }
                    for (sid in discoveredStreamIds.take(2)) {
                        val encSid = URLEncoder.encode(sid, "UTF-8")
                        add("$host/wefeed-mobile-bff/subject-api/get-stream-captions?subjectId=$encodedId&streamId=$encSid")
                    }
                }
                for (capUrl in captionEndpoints) {
                    runCatching {
                        val (code, body) = executeMobileSigned(host = host, url = capUrl)
                        if (code in 200..299) {
                            val capParsed = parseDownloadResponseJson(
                                json = body,
                                subject = subject.copy(detailPath = resolvedDetailPath),
                                sourceHost = host,
                                season = if (isTv) safeSeason else 1,
                                episode = if (isTv) safeEpisode else 1
                            )
                            mergedSubtitles.addAll(capParsed.subtitles)
                        }
                    }
                }
            }

            if (hostHadSuccess) break
        }

        // Phase 2: OpenStream x84.u H5 Unlock Bridge (h5-api.aoneroom.com with videodownloader.site referer)
        // Mobile visitor tokens frequently return only bait placeholder clips on mobile play-info,
        // while h5-api + videodownloader.site referer unlocks full 360p-1080p MP4 links + captions.
        if (mergedOptions.none { it.resolution >= 720 && !isBaitOrBlockedUrl(it.downloadUrl) }) {
            val encodedPath = URLEncoder.encode(resolvedDetailPath, "UTF-8")
            val querySuffix = if (encodedPath.isNotBlank()) "&detailPath=$encodedPath" else ""
            val h5Queries = buildList {
                add("subjectId=$encodedId&se=$safeSeason&ep=$safeEpisode$querySuffix")
                if (!isTv) {
                    add("subjectId=$encodedId&se=1&ep=1$querySuffix")
                } else if (safeSeason > 1) {
                    add("subjectId=$encodedId&se=1&ep=$safeEpisode$querySuffix")
                }
            }

            for (q in h5Queries) {
                var gotOptionsThisQuery = false
                for (path in listOf("play", "download")) {
                    val url = "$H5_UNLOCK_API_HOST/wefeed-h5api-bff/subject/$path?$q"
                    runCatching {
                        val (code, body) = executeH5UnlockRequest(url)
                        if (code !in 200..299) {
                            throw IllegalStateException("HTTP $code from $H5_UNLOCK_API_HOST")
                        }
                        val parsed = parseDownloadResponseJson(
                            json = body,
                            subject = subject.copy(detailPath = resolvedDetailPath),
                            sourceHost = H5_UNLOCK_API_HOST,
                            season = if (isTv) safeSeason else 1,
                            episode = if (isTv) safeEpisode else 1,
                            refererOverride = VIDEODOWNLOADER_REFERER,
                            userAgentOverride = H5_DESKTOP_USER_AGENT
                        )
                        if (parsed.options.isNotEmpty()) {
                            gotOptionsThisQuery = true
                            if (winningSourceHost.isBlank()) winningSourceHost = H5_UNLOCK_API_HOST
                        }
                        mergedOptions.addAll(parsed.options)
                        mergedSubtitles.addAll(parsed.subtitles)
                    }.onFailure { lastError = it }
                }
                if (gotOptionsThisQuery) break
            }
        }

        // Phase 3: H5 Mirror & Custom Proxy fallback (if Mobile BFF + h5-api bridge didn't yield options)
        if (mergedOptions.isEmpty()) {
            for (host in hosts.filter { !isMobileBackendHost(it) }) {
                if (resolvedDetailPath.isBlank()) {
                    resolvedDetailPath = fetchH5DetailPath(host, subject).orEmpty()
                }
                val encodedPath = URLEncoder.encode(resolvedDetailPath, "UTF-8")
                val playReferer = buildPlayPageReferer(host, subject.subjectId, resolvedDetailPath)
                val querySuffix = if (encodedPath.isNotBlank()) "&detailPath=$encodedPath" else ""
                val primaryQuery = "subjectId=$encodedId&se=$safeSeason&ep=$safeEpisode$querySuffix"

                val mirrorEndpoints = buildList {
                    add("$host/wefeed-h5api-bff/subject/download?$primaryQuery")
                    add("$host/wefeed-h5api-bff/subject/play?$primaryQuery")
                    if (!isTv) {
                        val altMovieQuery = "subjectId=$encodedId&se=1&ep=1$querySuffix"
                        add("$host/wefeed-h5api-bff/subject/download?$altMovieQuery")
                        add("$host/wefeed-h5api-bff/subject/play?$altMovieQuery")
                    }
                    add("$host/wefeed-h5-api/subject/download?$primaryQuery")
                    add("$host/wefeed-h5-api/subject/play?$primaryQuery")
                    add("$host/api/download?subjectId=$encodedId&id=$encodedId&se=$safeSeason&ep=$safeEpisode&detailPath=$encodedPath")
                    add("$host/api/sources?subjectId=$encodedId&id=$encodedId&se=$safeSeason&ep=$safeEpisode&detailPath=$encodedPath")
                }

                for (endpoint in mirrorEndpoints) {
                    runCatching {
                        val (code, body) = executeH5WithSession(
                            host = host,
                            url = endpoint,
                            referer = playReferer
                        )
                        if (code !in 200..299) {
                            throw IllegalStateException("HTTP $code from $host")
                        }
                        val parsed = parseDownloadResponseJson(
                            json = body,
                            subject = subject.copy(detailPath = resolvedDetailPath),
                            sourceHost = host,
                            season = if (isTv) safeSeason else 1,
                            episode = if (isTv) safeEpisode else 1
                        )
                        if (parsed.options.isNotEmpty() && winningSourceHost.isBlank()) {
                            winningSourceHost = host
                        }
                        mergedOptions.addAll(parsed.options)
                        mergedSubtitles.addAll(parsed.subtitles)
                    }.onFailure { lastError = it }
                    if (mergedOptions.isNotEmpty() && mergedSubtitles.isNotEmpty()) break
                }

                if (mergedOptions.isNotEmpty()) break
            }
        }

        val cleanOptions = finalizeOptions(mergedOptions)
        if (cleanOptions.isNotEmpty()) {
            val cleanSubtitles = finalizeSubtitles(mergedSubtitles)
            val updatedSubject = subject.copy(detailPath = resolvedDetailPath)
            val groups = buildLanguageGroupsFromOptions(
                primarySubject = updatedSubject,
                options = cleanOptions,
                subtitles = cleanSubtitles
            )
            return@withContext Result.success(
                MovieBoxDownloadBundle(
                    subject = updatedSubject,
                    options = cleanOptions,
                    subtitles = cleanSubtitles,
                    sourceHost = winningSourceHost.ifBlank { DEFAULT_HOST },
                    season = if (isTv) safeSeason else 1,
                    episode = if (isTv) safeEpisode else 1,
                    languageGroups = groups
                )
            )
        }

        Result.failure(
            lastError ?: IllegalStateException(
                "No downloadable video files available for \"${subject.title}\"."
            )
        )
    }

    suspend fun testHostConnection(hostUrl: String): Result<String> = withContext(Dispatchers.IO) {
        val normalized = normalizeHostUrl(hostUrl)
        runCatching {
            val searchPayload = mapAdapter.toJson(
                mapOf("keyword" to "Inception", "page" to 1, "perPage" to 5, "subjectType" to 0)
            )

            if (isMobileBackendHost(normalized)) {
                val token = ensureMobileSessionToken(normalized, forceRefresh = true)
                val (code, body) = executeMobileSigned(
                    host = normalized,
                    url = "$normalized/wefeed-mobile-bff/subject-api/search",
                    postJson = searchPayload
                )
                if (code !in 200..299) {
                    throw IllegalStateException("HTTP $code from $normalized")
                }
                val items = parseSearchResponseJson(body, normalized)
                if (items.isNotEmpty()) {
                    val first = items.first()
                    val bundleRes = fetchDownloadBundle(
                        subject = first,
                        season = if (first.subjectType == 2) 1 else 0,
                        episode = if (first.subjectType == 2) 1 else 0,
                        isTv = first.subjectType == 2,
                        customHost = normalized
                    )
                    val dlCount = bundleRes.getOrNull()?.options?.size ?: 0
                    val subCount = bundleRes.getOrNull()?.subtitles?.size ?: 0
                    if (dlCount > 0) {
                        "Connected to $normalized (${items.size} results, $dlCount stream/download links + $subCount subtitles verified)"
                    } else if (token.isNotBlank()) {
                        "Connected to $normalized (${items.size} results, signed session active)"
                    } else {
                        "Reachable ($normalized • HTTP $code)"
                    }
                } else if (isSuccessfulApiPayload(body)) {
                    "Reachable ($normalized • HTTP $code)"
                } else {
                    throw IllegalStateException("Unexpected response from $normalized")
                }
            } else {
                val token = ensureH5SessionToken(normalized, forceRefresh = true)
                val (code, body) = executeH5WithSession(
                    host = normalized,
                    url = "$normalized/wefeed-h5api-bff/subject/search",
                    referer = "$normalized/",
                    postJson = searchPayload
                )
                if (code !in 200..299) {
                    throw IllegalStateException("HTTP $code")
                }
                val items = parseSearchResponseJson(body, normalized)
                if (items.isNotEmpty()) {
                    val first = items.first()
                    val dlCheck = if (first.detailPath.isNotBlank()) {
                        val encodedId = URLEncoder.encode(first.subjectId, "UTF-8")
                        val encodedPath = URLEncoder.encode(first.detailPath, "UTF-8")
                        val se = if (first.subjectType == 2) 1 else 0
                        val ep = if (first.subjectType == 2) 1 else 0
                        val ref = buildPlayPageReferer(normalized, first.subjectId, first.detailPath)
                        runCatching {
                            val (dlCode, dlBody) = executeH5WithSession(
                                host = normalized,
                                url = "$normalized/wefeed-h5api-bff/subject/download?subjectId=$encodedId&se=$se&ep=$ep&detailPath=$encodedPath",
                                referer = ref
                            )
                            if (dlCode in 200..299) {
                                parseDownloadResponseJson(dlBody, first, normalized).options.size
                            } else {
                                0
                            }
                        }.getOrDefault(0)
                    } else {
                        0
                    }
                    if (dlCheck > 0) {
                        "Connected to $normalized (${items.size} results, $dlCheck download links verified)"
                    } else if (token.isNotBlank()) {
                        "Connected to $normalized (${items.size} results)"
                    } else {
                        "Reachable ($normalized • HTTP $code)"
                    }
                } else if (isSuccessfulApiPayload(body)) {
                    "Reachable ($normalized • HTTP $code)"
                } else {
                    throw IllegalStateException("Unexpected response from $normalized")
                }
            }
        }
    }

    private fun refreshGslbHostsIfNeeded(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && cachedGslbHosts.isNotEmpty() && now - lastGslbRefreshMs < 15 * 60_000L) {
            return
        }
        if (!force && lastGslbRefreshMs > 0L && now - lastGslbRefreshMs < 60_000L) {
            return
        }
        lastGslbRefreshMs = now
        val discovered = linkedSetOf<String>()
        for (domain in listOf("api4sg.aoneroom.com", "api4.aoneroom.com", "api.inmoviebox.com")) {
            runCatching {
                val ts = System.currentTimeMillis()
                val sign = "$ts${md5Hex(ts.toString().reversed().toByteArray(Charsets.UTF_8))}"
                val payload = mapAdapter.toJson(mapOf("domain" to domain))
                val req = Request.Builder()
                    .url(GSLB_CONVERT_URL)
                    .header("Accept", "*/*")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", MOBILE_USER_AGENT)
                    .header("X-Gslb-Sign", sign)
                    .post(payload.toByteArray(Charsets.UTF_8).toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string().orEmpty()
                    val root = runCatching { mapAdapter.fromJson(body) }.getOrNull() ?: return@use
                    val data = root["data"]
                    when (data) {
                        is String -> if (data.contains('.')) discovered.add(normalizeHostUrl(data))
                        is Map<*, *> -> {
                            listOf("domain", "host", "url").forEach { key ->
                                val candidate = data[key].asCleanString()
                                if (candidate.contains('.')) discovered.add(normalizeHostUrl(candidate))
                            }
                        }
                    }
                }
            }
        }
        if (discovered.isNotEmpty()) {
            cachedGslbHosts = discovered.toList()
        }
    }

    private fun ensureMobileSessionToken(host: String, forceRefresh: Boolean = false): String {
        if (!forceRefresh) {
            val cached = mobileTokenByHost[host]
            if (!cached.isNullOrBlank()) return cached
        }
        val visitorLoginUrl = "$host/wefeed-mobile-bff/user-api/visitor-login"
        val bodyJson = "{}"
        runCatching {
            val req = buildMobileSignedRequest(
                method = "POST",
                url = visitorLoginUrl,
                body = bodyJson,
                token = ""
            ).post(bodyJson.toByteArray(Charsets.UTF_8).toRequestBody(JSON_MEDIA_TYPE)).build()

            okHttpClient.newCall(req).execute().use { resp ->
                val respBody = resp.body?.string().orEmpty()
                val root = runCatching { mapAdapter.fromJson(respBody) }.getOrNull()
                val dataToken = root?.get("data")?.asMapOrNull()?.get("token").asCleanString()
                val headerToken = extractTokenFromResponse(resp, host, mobileTokenByHost)
                val token = dataToken.ifBlank { headerToken.orEmpty() }
                if (token.isNotBlank()) {
                    mobileTokenByHost[host] = token
                    return token
                }
            }
        }

        // Fallback handshake via tab-operating header
        val tabUrl = "$host/wefeed-mobile-bff/tab-operating?page=1&tabId=0&version="
        runCatching {
            val req = buildMobileSignedRequest(
                method = "GET",
                url = tabUrl,
                body = null,
                token = ""
            ).get().build()
            okHttpClient.newCall(req).execute().use { resp ->
                val token = extractTokenFromResponse(resp, host, mobileTokenByHost)
                if (!token.isNullOrBlank()) {
                    mobileTokenByHost[host] = token
                    return token
                }
            }
        }
        return mobileTokenByHost[host].orEmpty()
    }

    private fun executeMobileSigned(
        host: String,
        url: String,
        postJson: String? = null
    ): Pair<Int, String> {
        var token = ensureMobileSessionToken(host, forceRefresh = false)
        repeat(2) { attempt ->
            val method = if (postJson != null) "POST" else "GET"
            val builder = buildMobileSignedRequest(
                method = method,
                url = url,
                body = postJson,
                token = token
            )
            if (postJson != null) {
                builder.post(postJson.toByteArray(Charsets.UTF_8).toRequestBody(JSON_MEDIA_TYPE))
            } else {
                builder.get()
            }
            val callResult = runCatching {
                okHttpClient.newCall(builder.build()).execute().use { resp ->
                    extractTokenFromResponse(resp, host, mobileTokenByHost)?.let { refreshed ->
                        mobileTokenByHost[host] = refreshed
                        token = refreshed
                    }
                    val body = resp.body?.string().orEmpty()
                    resp.code to body
                }
            }
            val (code, body) = callResult.getOrElse { err ->
                val msg = err.message.orEmpty()
                if (msg.contains("407")) {
                    407 to msg
                } else {
                    throw err
                }
            }
            val tokenError = (code == 400 || code == 401 || code == 403 || code == 407) ||
                body.contains("invalid token", ignoreCase = true) ||
                body.contains("UNAUTHORIZED", ignoreCase = true) ||
                body.contains("token expired", ignoreCase = true)
            if (tokenError && attempt == 0) {
                token = ensureMobileSessionToken(host, forceRefresh = true)
                return@repeat
            }
            return code to body
        }
        return 500 to ""
    }

    private fun buildMobileSignedRequest(
        method: String,
        url: String,
        body: String?,
        token: String,
        timestampMillis: Long = System.currentTimeMillis()
    ): Request.Builder {
        val signature = computeTrSignature(method, url, body, timestampMillis)
        val clientToken = computeClientToken(timestampMillis)
        return Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", MOBILE_USER_AGENT)
            .header("X-Client-Info", mobileClientInfoHeader)
            .header("X-Client-Status", "0")
            .header("X-Client-Token", clientToken)
            .header("x-tr-signature", signature)
            .apply {
                if (token.isNotBlank()) {
                    header("Authorization", "Bearer $token")
                }
            }
    }

    private fun executeH5UnlockRequest(url: String): Pair<Int, String> {
        val req = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", H5_DESKTOP_USER_AGENT)
            .header("Referer", VIDEODOWNLOADER_REFERER)
            .header("Origin", VIDEODOWNLOADER_ORIGIN)
            .header("X-Client-Info", H5_UNLOCK_CLIENT_INFO)
            .header("x-request-lang", "en")
            .get()
            .build()
        okHttpClient.newCall(req).execute().use { resp ->
            return resp.code to resp.body?.string().orEmpty()
        }
    }

    private fun ensureH5SessionToken(host: String, forceRefresh: Boolean = false): String {
        if (!forceRefresh) {
            val cached = h5TokenByHost[host]
            if (!cached.isNullOrBlank()) return cached
        }
        val handshakeUrl = "$host/wefeed-h5api-bff/app/get-latest-app-pkgs?appName=moviebox"
        val req = buildH5BaseRequest(url = handshakeUrl, host = host, referer = "$host/")
            .get()
            .build()
        runCatching {
            okHttpClient.newCall(req).execute().use { resp ->
                extractTokenFromResponse(resp, host, h5TokenByHost)?.let { token ->
                    h5TokenByHost[host] = token
                    return token
                }
            }
        }
        return h5TokenByHost[host].orEmpty()
    }

    private fun executeH5WithSession(
        host: String,
        url: String,
        referer: String,
        postJson: String? = null
    ): Pair<Int, String> {
        var token = ensureH5SessionToken(host, forceRefresh = false)
        repeat(2) { attempt ->
            val builder = buildH5BaseRequest(
                url = url,
                host = host,
                token = token,
                referer = referer
            )
            if (postJson != null) {
                builder.post(postJson.toByteArray(Charsets.UTF_8).toRequestBody(JSON_MEDIA_TYPE))
            } else {
                builder.get()
            }
            okHttpClient.newCall(builder.build()).execute().use { resp ->
                extractTokenFromResponse(resp, host, h5TokenByHost)?.let { refreshed ->
                    h5TokenByHost[host] = refreshed
                    token = refreshed
                }
                val body = resp.body?.string().orEmpty()
                val tokenError = (resp.code == 400 || resp.code == 401) &&
                    (body.contains("invalid token", ignoreCase = true) ||
                        body.contains("UNAUTHORIZED", ignoreCase = true) ||
                        body.contains("PARAMS_ERROR", ignoreCase = true))
                if (tokenError && attempt == 0) {
                    token = ensureH5SessionToken(host, forceRefresh = true)
                    return@repeat
                }
                return resp.code to body
            }
        }
        return 500 to ""
    }

    private fun extractTokenFromResponse(
        response: Response,
        host: String,
        cache: ConcurrentHashMap<String, String>
    ): String? {
        val xUser = response.header("x-user")?.trim().orEmpty()
        if (xUser.isNotBlank()) {
            val fromJson = runCatching { mapAdapter.fromJson(xUser)?.get("token").asCleanString() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
            if (fromJson != null) return fromJson
            val regexMatch = Regex("\"token\"\\s*:\\s*\"([^\"]+)\"").find(xUser)?.groupValues?.getOrNull(1)
            if (!regexMatch.isNullOrBlank()) return regexMatch
        }
        val cookieToken = cookieStore[response.request.url.host]
            ?.firstOrNull { it.name == "token" && it.value.isNotBlank() }
            ?.value
        if (!cookieToken.isNullOrBlank()) return cookieToken
        return cache[host]?.takeIf { it.isNotBlank() }
    }

    private fun buildPlayPageReferer(host: String, subjectId: String, detailPath: String): String {
        val cleanHost = originForHost(host)
        if (detailPath.isBlank()) return "$cleanHost/"
        return "$cleanHost/spa/videoPlayPage/movies/$detailPath?id=$subjectId&type=/movie/detail"
    }

    private fun originForHost(host: String): String {
        return if (host.contains("h5-api.aoneroom.com", ignoreCase = true)) {
            VIDEODOWNLOADER_ORIGIN
        } else {
            host.trimEnd('/')
        }
    }

    private fun fetchMobileDetailPath(host: String, subject: MovieBoxSubject): String? {
        val encodedId = URLEncoder.encode(subject.subjectId, "UTF-8")
        val endpoints = listOf(
            "$host/wefeed-mobile-bff/subject-api/get?subjectId=$encodedId",
            "$host/wefeed-mobile-bff/subject-api/detail?subjectId=$encodedId"
        )
        for (url in endpoints) {
            val found = runCatching {
                val (code, body) = executeMobileSigned(host = host, url = url)
                if (code !in 200..299) return@runCatching null
                val (detailPath, _) = parseSubjectDetailDubsJson(body, subject, host)
                detailPath.ifBlank { null }
            }.getOrNull()
            if (!found.isNullOrBlank()) return found
        }
        return null
    }

    private fun fetchH5DetailPath(host: String, subject: MovieBoxSubject): String? {
        val encodedId = URLEncoder.encode(subject.subjectId, "UTF-8")
        val endpoints = listOf(
            "$host/wefeed-h5api-bff/subject/detail?subjectId=$encodedId",
            "$host/wefeed-h5-api/subject/detail?subjectId=$encodedId"
        )
        for (url in endpoints) {
            val found = runCatching {
                val (code, body) = executeH5WithSession(host = host, url = url, referer = "$host/")
                if (code !in 200..299) return@runCatching null
                val (detailPath, _) = parseSubjectDetailDubsJson(body, subject, host)
                detailPath.ifBlank { null }
            }.getOrNull()
            if (!found.isNullOrBlank()) return found
        }
        if (subject.title.isNotBlank()) {
            val payload = mapAdapter.toJson(
                mapOf("keyword" to subject.title, "page" to 1, "perPage" to 5, "subjectType" to subject.subjectType)
            )
            return runCatching {
                val (code, body) = executeH5WithSession(
                    host = host,
                    url = "$host/wefeed-h5api-bff/subject/search",
                    referer = "$host/",
                    postJson = payload
                )
                if (code !in 200..299) return@runCatching null
                val candidates = parseSearchResponseJson(body, host)
                candidates.firstOrNull { it.subjectId == subject.subjectId }?.detailPath?.ifBlank { null }
                    ?: candidates.firstOrNull()?.detailPath?.ifBlank { null }
            }.getOrNull()
        }
        return null
    }

    private fun fetchSubjectDetailDubs(
        subject: MovieBoxSubject,
        customHost: String = ""
    ): List<MovieBoxSubject> {
        val encodedId = URLEncoder.encode(subject.subjectId, "UTF-8")
        val hosts = buildList {
            if (subject.sourceHost.isNotBlank()) add(normalizeHostUrl(subject.sourceHost))
            addAll(candidateHosts(customHost))
        }.distinct()

        for (host in hosts.take(4)) {
            if (isMobileBackendHost(host)) {
                val endpoints = listOf(
                    "$host/wefeed-mobile-bff/subject-api/get?subjectId=$encodedId",
                    "$host/wefeed-mobile-bff/subject-api/detail?subjectId=$encodedId"
                )
                for (url in endpoints) {
                    val dubs = runCatching {
                        val (code, body) = executeMobileSigned(host = host, url = url)
                        if (code !in 200..299) return@runCatching emptyList()
                        parseSubjectDetailDubsJson(body, subject, host).second
                    }.getOrDefault(emptyList())
                    if (dubs.isNotEmpty()) return dubs
                }
            } else {
                val endpoints = listOf(
                    "$host/wefeed-h5api-bff/subject/detail?subjectId=$encodedId",
                    "$host/wefeed-h5-api/subject/detail?subjectId=$encodedId"
                )
                for (url in endpoints) {
                    val dubs = runCatching {
                        val (code, body) = executeH5WithSession(host = host, url = url, referer = "$host/")
                        if (code !in 200..299) return@runCatching emptyList()
                        parseSubjectDetailDubsJson(body, subject, host).second
                    }.getOrDefault(emptyList())
                    if (dubs.isNotEmpty()) return dubs
                }
            }
        }
        return emptyList()
    }

    fun parseSubjectDetailDubsJson(
        json: String,
        baseSubject: MovieBoxSubject,
        sourceHost: String = DEFAULT_HOST
    ): Pair<String, List<MovieBoxSubject>> {
        val trimmed = json.trim()
        if (trimmed.isBlank()) return "" to emptyList()
        val root = runCatching { mapAdapter.fromJson(trimmed) }.getOrNull() ?: return "" to emptyList()
        val data = root["data"].asMapOrNull() ?: root
        val subj = data["subject"].asMapOrNull() ?: data
        val detailPath = subj["detailPath"].asCleanString()
            .ifBlank { data["detailPath"].asCleanString() }
            .ifBlank { baseSubject.detailPath }

        val rawDubs = buildList {
            addAll(subj["dubs"].asMapListOrEmpty())
            addAll(data["dubs"].asMapListOrEmpty())
            addAll(data["subjectDubs"].asMapListOrEmpty())
            addAll(root["dubs"].asMapListOrEmpty())
        }
        val dubSubjects = rawDubs.mapNotNull { dub ->
            val dubId = dub["subjectId"].asCleanString()
                .ifBlank { dub["id"].asCleanString() }
            if (dubId.isBlank()) return@mapNotNull null
            val rawLang = dub["lanName"].asCleanString()
                .ifBlank { dub["language"].asCleanString() }
                .ifBlank { dub["langName"].asCleanString() }
                .ifBlank { dub["name"].asCleanString() }
                .ifBlank { dub["lan"].asCleanString() }
                .ifBlank { dub["lanCode"].asCleanString() }
            val normalizedLang = normalizeLanguageLabel(rawLang)
            val dubPath = dub["detailPath"].asCleanString().ifBlank { detailPath }
            baseSubject.copy(
                subjectId = dubId,
                detailPath = dubPath,
                sourceHost = sourceHost,
                language = normalizedLang
            )
        }.distinctBy { it.subjectId }

        return detailPath to dubSubjects
    }

    private fun buildH5BaseRequest(
        url: String,
        host: String = DEFAULT_HOST,
        token: String = "",
        referer: String = "$host/"
    ): Request.Builder {
        val origin = originForHost(host)
        return Request.Builder()
            .url(url)
            .header("Accept", "application/json, text/plain, */*")
            .header("Content-Type", "application/json")
            .header("Origin", origin)
            .header("Referer", referer)
            .header("User-Agent", H5_DESKTOP_USER_AGENT)
            .header("X-Client-Info", H5_SESSION_CLIENT_INFO)
            .header("X-Request-Lang", "en")
            .header("X-No-High-Risk-Restrict", "0")
            .header("X-Vip-Restrict", "0")
            .apply {
                if (token.isNotBlank()) {
                    header("Authorization", "Bearer $token")
                }
            }
    }

    private fun collectIdsFromJson(
        json: String,
        resourceIds: MutableSet<String>,
        streamIds: MutableSet<String>
    ) {
        val root = runCatching { mapAdapter.fromJson(json.trim()) }.getOrNull() ?: return
        val data = root["data"].asMapOrNull() ?: root
        val allEntries = buildList {
            addAll(data["list"].asMapListOrEmpty())
            addAll(data["streams"].asMapListOrEmpty())
            addAll(data["downloads"].asMapListOrEmpty())
            addAll(data["playUrls"].asMapListOrEmpty())
        }
        for (entry in allEntries) {
            val rid = entry["resourceId"].asCleanString()
            if (rid.isNotBlank()) resourceIds.add(rid)
            val sid = entry["streamId"].asCleanString().ifBlank { entry["id"].asCleanString() }
            if (sid.isNotBlank() && entry["resourceId"].asCleanString().isBlank()) streamIds.add(sid)
        }
    }

    private fun isSuccessfulApiPayload(json: String): Boolean {
        val root = runCatching { mapAdapter.fromJson(json) }.getOrNull() ?: return false
        val code = root["code"].asIntOrNull()
        val status = root["status"].asIntOrNull()
        return code == 0 || code == 200 || status == 200
    }

    private fun deduplicateSearchSubjects(items: List<MovieBoxSubject>): List<MovieBoxSubject> {
        if (items.size <= 1) return items
        return items.groupBy { it.subjectId }
            .values
            .map { group ->
                group.minByOrNull { item ->
                    val title = item.title
                    when {
                        Regex("\\bS1\\b", RegexOption.IGNORE_CASE).containsMatchIn(title) -> 0
                        !Regex("\\bS\\d+\\b", RegexOption.IGNORE_CASE).containsMatchIn(title) -> 1
                        else -> 2
                    }
                } ?: group.first()
            }
    }

    fun parseSearchResponseJson(json: String, sourceHost: String = DEFAULT_HOST): List<MovieBoxSubject> {
        val trimmed = json.trim()
        if (trimmed.isBlank()) return emptyList()
        val root = runCatching { mapAdapter.fromJson(trimmed) }.getOrNull() ?: return emptyList()
        val data = root["data"].asMapOrNull() ?: root
        val rawItems = data["items"].asMapListOrEmpty()
            .ifEmpty { data["results"].asMapListOrEmpty() }
            .ifEmpty { data["subjects"].asMapListOrEmpty() }
            .ifEmpty { root["items"].asMapListOrEmpty() }
            .ifEmpty { root["results"].asMapListOrEmpty() }

        return rawItems.mapNotNull { item ->
            val id = item["subjectId"].asCleanString()
                .ifBlank { item["id"].asCleanString() }
            val title = item["title"].asCleanString()
                .ifBlank { item["name"].asCleanString() }
            if (id.isBlank() || title.isBlank()) return@mapNotNull null

            val coverMap = item["cover"].asMapOrNull()
            val coverUrl = coverMap?.get("url").asCleanString()
                .ifBlank { item["poster"].asCleanString() }
                .ifBlank { item["coverUrl"].asCleanString() }

            val rawSubjectType = item["subjectType"].asIntOrNull()
                ?: if (item["mediaType"].asCleanString().equals("tv", ignoreCase = true)) 2 else 1

            val rawLang = item["lanName"].asCleanString()
                .ifBlank { item["language"].asCleanString() }
                .ifBlank { item["langName"].asCleanString() }
                .ifBlank { item["audioLanguage"].asCleanString() }
                .ifBlank { item["dubLanguage"].asCleanString() }
                .ifBlank { item["lan"].asCleanString() }
                .ifBlank { extractLanguageFromTitle(title) }

            MovieBoxSubject(
                subjectId = id,
                subjectType = rawSubjectType,
                title = title,
                description = item["description"].asCleanString().ifBlank { item["overview"].asCleanString() },
                releaseDate = item["releaseDate"].asCleanString(),
                imdbRating = item["imdbRatingValue"].asCleanString()
                    .ifBlank { item["imdbRating"].asCleanString() },
                detailPath = item["detailPath"].asCleanString(),
                coverUrl = coverUrl,
                hasResource = item["hasResource"] as? Boolean ?: true,
                sourceHost = sourceHost,
                language = normalizeLanguageLabel(rawLang)
            )
        }
    }

    fun parseDownloadResponseJson(
        json: String,
        subject: MovieBoxSubject,
        sourceHost: String = DEFAULT_HOST,
        season: Int = 1,
        episode: Int = 1,
        refererOverride: String? = null,
        userAgentOverride: String? = null
    ): MovieBoxDownloadBundle {
        val trimmed = json.trim()
        if (trimmed.isBlank()) {
            return MovieBoxDownloadBundle(subject = subject, options = emptyList(), sourceHost = sourceHost)
        }
        val root = runCatching { mapAdapter.fromJson(trimmed) }.getOrNull()
            ?: return MovieBoxDownloadBundle(subject = subject, options = emptyList(), sourceHost = sourceHost)
        val data = root["data"].asMapOrNull() ?: root

        val rawDownloads = buildList {
            addAll(data["downloads"].asMapListOrEmpty())
            addAll(data["streams"].asMapListOrEmpty())
            addAll(data["sources"].asMapListOrEmpty())
            addAll(data["list"].asMapListOrEmpty())
            addAll(data["playUrls"].asMapListOrEmpty())
            addAll(root["downloads"].asMapListOrEmpty())
            addAll(root["streams"].asMapListOrEmpty())
        }

        val singleUrlFallback = data["downloadUrl"].asCleanString()
            .ifBlank { data["playUrl"].asCleanString() }
            .ifBlank { data["resourceLink"].asCleanString() }
            .ifBlank { data["url"].asCleanString() }

        val effectiveReferer = refererOverride
            ?: "${originForHost(normalizeHostUrl(sourceHost))}/"
        val effectiveUserAgent = userAgentOverride ?: H5_DESKTOP_USER_AGENT
        val fallbackSubjectLang = normalizeLanguageLabel(
            subject.language.ifBlank { extractLanguageFromTitle(subject.title) }
        )

        val embeddedCaptions = mutableListOf<Map<String, Any?>>()
        val parsedOptions = rawDownloads.mapIndexedNotNull { idx, entry ->
            embeddedCaptions.addAll(entry["extCaptions"].asMapListOrEmpty())
            embeddedCaptions.addAll(entry["captions"].asMapListOrEmpty())
            embeddedCaptions.addAll(entry["subtitles"].asMapListOrEmpty())

            val url = entry["url"].asCleanString()
                .ifBlank { entry["downloadUrl"].asCleanString() }
                .ifBlank { entry["playUrl"].asCleanString() }
                .ifBlank { entry["resourceLink"].asCleanString() }
            if (url.isBlank() || isBaitOrBlockedUrl(url)) return@mapIndexedNotNull null

            val resInt = parseResolutionNumber(
                entry["resolution"] ?: entry["resolutions"] ?: entry["quality"]
            )
            val resLabel = when {
                resInt >= 2160 -> "4K Ultra HD"
                resInt >= 1080 -> "1080p Full HD"
                resInt >= 720 -> "720p HD"
                resInt >= 480 -> "480p SD"
                resInt > 0 -> "${resInt}p"
                else -> entry["quality"].asCleanString().ifBlank { "Auto" }
            }
            val sizeBytes = entry["size"].asLongOrNull()
                ?: entry["fileSize"].asLongOrNull()
                ?: 0L
            val format = entry["format"].asCleanString()
                .ifBlank { inferFormatFromUrl(url) }
                .uppercase(Locale.US)
            val signCookie = entry["signCookie"].asCleanString()
                .ifBlank { entry["cookie"].asCleanString() }
            val rawEntryLang = entry["lanName"].asCleanString()
                .ifBlank { entry["language"].asCleanString() }
                .ifBlank { entry["audioLanguage"].asCleanString() }
                .ifBlank { entry["audioLang"].asCleanString() }
                .ifBlank { entry["audio"].asCleanString() }
                .ifBlank { entry["dubName"].asCleanString() }
                .ifBlank { entry["lan"].asCleanString() }
                .ifBlank { entry["lang"].asCleanString() }
                .ifBlank { extractLanguageFromTitle(entry["name"].asCleanString()) }
                .ifBlank { extractLanguageFromTitle(entry["title"].asCleanString()) }
                .ifBlank { fallbackSubjectLang }
            val resolvedLang = normalizeLanguageLabel(rawEntryLang)

            MovieBoxDownloadOption(
                id = entry["id"].asCleanString()
                    .ifBlank { entry["resourceId"].asCleanString() }
                    .ifBlank { "opt_${idx}_$resInt" },
                resolution = resInt,
                resolutionLabel = resLabel,
                sizeBytes = sizeBytes,
                downloadUrl = url,
                format = format,
                refererHeader = effectiveReferer,
                userAgentHeader = effectiveUserAgent,
                cookieHeader = signCookie,
                language = resolvedLang
            )
        }.ifEmpty {
            if (singleUrlFallback.isNotBlank() && !isBaitOrBlockedUrl(singleUrlFallback)) {
                listOf(
                    MovieBoxDownloadOption(
                        id = "opt_default",
                        resolution = 720,
                        resolutionLabel = "720p HD",
                        sizeBytes = data["fileSize"].asLongOrNull() ?: 0L,
                        downloadUrl = singleUrlFallback,
                        format = inferFormatFromUrl(singleUrlFallback),
                        refererHeader = effectiveReferer,
                        userAgentHeader = effectiveUserAgent,
                        language = fallbackSubjectLang
                    )
                )
            } else {
                emptyList()
            }
        }

        val finalOptions = finalizeOptions(parsedOptions)

        val rawCaptions = buildList {
            addAll(data["captions"].asMapListOrEmpty())
            addAll(data["subtitles"].asMapListOrEmpty())
            addAll(data["extCaptions"].asMapListOrEmpty())
            addAll(data["list"].asMapListOrEmpty().filter { !it["url"].asCleanString().endsWith(".mp4", ignoreCase = true) })
            addAll(root["captions"].asMapListOrEmpty())
            addAll(embeddedCaptions)
        }

        val parsedSubtitles = rawCaptions.mapIndexedNotNull { idx, entry ->
            val subUrl = entry["url"].asCleanString()
            if (subUrl.isBlank()) return@mapIndexedNotNull null
            if (!subUrl.contains(".srt", ignoreCase = true) &&
                !subUrl.contains(".vtt", ignoreCase = true) &&
                entry["lan"].asCleanString().isBlank() &&
                entry["lang"].asCleanString().isBlank() &&
                entry["lanName"].asCleanString().isBlank() &&
                entry["language"].asCleanString().isBlank()
            ) {
                return@mapIndexedNotNull null
            }
            val lanCode = entry["lan"].asCleanString()
                .ifBlank { entry["lang"].asCleanString() }
                .ifBlank { entry["languageCode"].asCleanString() }
                .ifBlank { "en" }
            val lanName = entry["lanName"].asCleanString()
                .ifBlank { entry["language"].asCleanString() }
                .ifBlank { entry["name"].asCleanString() }
                .ifBlank { lanCode.uppercase(Locale.US) }

            MovieBoxSubtitleOption(
                id = entry["id"].asCleanString().ifBlank { "sub_${idx}_$lanCode" },
                languageCode = lanCode,
                languageName = lanName,
                url = subUrl,
                sizeBytes = entry["size"].asLongOrNull() ?: 0L
            )
        }

        val finalSubtitles = finalizeSubtitles(parsedSubtitles)
        val languageGroups = buildLanguageGroupsFromOptions(
            primarySubject = subject,
            options = finalOptions,
            subtitles = finalSubtitles
        )

        return MovieBoxDownloadBundle(
            subject = subject,
            options = finalOptions,
            subtitles = finalSubtitles,
            sourceHost = sourceHost,
            season = season,
            episode = episode,
            languageGroups = languageGroups
        )
    }

    private fun finalizeOptions(options: List<MovieBoxDownloadOption>): List<MovieBoxDownloadOption> {
        if (options.isEmpty()) return emptyList()
        return options
            .filterNot { isBaitOrBlockedUrl(it.downloadUrl) }
            .sortedWith(
                compareByDescending<MovieBoxDownloadOption> { it.resolution }
                    .thenBy { if (it.format.equals("MP4", ignoreCase = true)) 0 else 1 }
                    .thenByDescending { it.sizeBytes }
            )
            .distinctBy { it.downloadUrl }
            .distinctBy {
                val langKey = it.language.trim().lowercase(Locale.US)
                if (it.resolution > 0) "res_${it.resolution}_${it.format}_$langKey" else it.downloadUrl
            }
    }

    private fun finalizeSubtitles(subtitles: List<MovieBoxSubtitleOption>): List<MovieBoxSubtitleOption> {
        if (subtitles.isEmpty()) return emptyList()
        return subtitles
            .distinctBy { "${it.languageCode}_${it.url}" }
            .sortedWith(
                compareBy<MovieBoxSubtitleOption> { !it.languageCode.equals("en", ignoreCase = true) }
                    .thenBy { it.languageName }
            )
    }

    fun collectSameTitleLanguageVariants(
        media: MediaItem,
        primarySubject: MovieBoxSubject,
        candidates: List<MovieBoxSubject>
    ): List<MovieBoxSubject> {
        val baseMediaTitle = normalizeTitle(stripTitleLanguageAndSeason(media.title))
        val basePrimaryTitle = normalizeTitle(stripTitleLanguageAndSeason(primarySubject.title))
        val expectedType = primarySubject.subjectType
        return candidates.filter { cand ->
            if (cand.subjectId == primarySubject.subjectId) return@filter false
            if (cand.subjectType != expectedType) return@filter false
            val candBase = normalizeTitle(stripTitleLanguageAndSeason(cand.title))
            val titleMatches = candBase.isNotBlank() &&
                (candBase == basePrimaryTitle || candBase == baseMediaTitle)
            if (!titleMatches) return@filter false
            cand.language.isNotBlank() || extractLanguageFromTitle(cand.title).isNotBlank()
        }
    }

    fun mergeLanguageBundles(
        primarySubject: MovieBoxSubject,
        bundlesWithSubjects: List<Pair<MovieBoxSubject, MovieBoxDownloadBundle>>,
        season: Int = 1,
        episode: Int = 1
    ): MovieBoxDownloadBundle {
        val allOptions = mutableListOf<MovieBoxDownloadOption>()
        val allSubtitles = mutableListOf<MovieBoxSubtitleOption>()
        val groupedByLang = linkedMapOf<String, MutableList<MovieBoxDownloadOption>>()
        val subjectByLang = linkedMapOf<String, MovieBoxSubject>()
        val subtitlesByLang = linkedMapOf<String, MutableList<MovieBoxSubtitleOption>>()
        var winningHost = ""

        val hasAnyExplicitLang = bundlesWithSubjects.any { (subj, bundle) ->
            subj.language.isNotBlank() || bundle.options.any { it.language.isNotBlank() }
        }

        for ((subj, bundle) in bundlesWithSubjects) {
            if (winningHost.isBlank() && bundle.sourceHost.isNotBlank()) {
                winningHost = bundle.sourceHost
            }
            allSubtitles.addAll(bundle.subtitles)
            val subjLang = normalizeLanguageLabel(
                subj.language.ifBlank { extractLanguageFromTitle(subj.title) }
            )
            for (opt in bundle.options) {
                val optLang = opt.language.ifBlank { subjLang }.let { raw ->
                    if (raw.isBlank() && hasAnyExplicitLang && bundlesWithSubjects.size > 1) "Original" else raw
                }
                val taggedOption = if (optLang.isNotBlank() && opt.language != optLang) {
                    opt.copy(language = optLang)
                } else {
                    opt
                }
                allOptions.add(taggedOption)
                val groupKey = optLang.ifBlank { "Default" }
                groupedByLang.getOrPut(groupKey) { mutableListOf() }.add(taggedOption)
                subjectByLang.putIfAbsent(groupKey, subj.copy(language = optLang))
                subtitlesByLang.getOrPut(groupKey) { mutableListOf() }.addAll(bundle.subtitles)
            }
        }

        val groups = if (groupedByLang.size > 1) {
            groupedByLang.entries.map { (langLabel, opts) ->
                MovieBoxLanguageGroup(
                    language = langLabel,
                    subject = subjectByLang[langLabel] ?: primarySubject,
                    options = finalizeOptions(opts),
                    subtitles = finalizeSubtitles(subtitlesByLang[langLabel].orEmpty())
                )
            }.filter { it.options.isNotEmpty() }
        } else {
            emptyList()
        }

        val finalMergedOptions = finalizeOptions(allOptions)
        val finalMergedSubtitles = finalizeSubtitles(allSubtitles)

        return MovieBoxDownloadBundle(
            subject = primarySubject,
            options = finalMergedOptions,
            subtitles = finalMergedSubtitles,
            sourceHost = winningHost.ifBlank { DEFAULT_HOST },
            season = season,
            episode = episode,
            languageGroups = groups
        )
    }

    private fun buildLanguageGroupsFromOptions(
        primarySubject: MovieBoxSubject,
        options: List<MovieBoxDownloadOption>,
        subtitles: List<MovieBoxSubtitleOption>
    ): List<MovieBoxLanguageGroup> {
        val distinctLangs = options.map { it.language.trim() }.filter { it.isNotBlank() }.distinct()
        if (distinctLangs.size <= 1) return emptyList()
        val hasBlank = options.any { it.language.isBlank() }
        val grouped = linkedMapOf<String, MutableList<MovieBoxDownloadOption>>()
        for (opt in options) {
            val label = opt.language.ifBlank { if (hasBlank) "Original" else "Default" }
            grouped.getOrPut(label) { mutableListOf() }.add(
                if (opt.language.isBlank()) opt.copy(language = label) else opt
            )
        }
        if (grouped.size <= 1) return emptyList()
        return grouped.entries.map { (lang, opts) ->
            MovieBoxLanguageGroup(
                language = lang,
                subject = primarySubject.copy(language = lang),
                options = finalizeOptions(opts),
                subtitles = subtitles
            )
        }
    }

    fun pickBestSubjectCandidate(
        media: MediaItem,
        candidates: List<MovieBoxSubject>
    ): MovieBoxSubject? {
        return candidates.maxByOrNull { scoreSubjectCandidate(media, it) }
    }

    fun scoreSubjectCandidate(media: MediaItem, candidate: MovieBoxSubject): Int {
        var score = 0
        val normMediaTitle = normalizeTitle(media.title)
        val normCandTitle = normalizeTitle(candidate.title)
        val normCandStripped = normalizeTitle(stripTitleLanguageAndSeason(candidate.title))
        when {
            normMediaTitle == normCandTitle -> score += 105
            normMediaTitle == normCandStripped -> score += 95
            normCandTitle.startsWith(normMediaTitle) || normMediaTitle.startsWith(normCandTitle) -> score += 65
            normCandTitle.contains(normMediaTitle) || normMediaTitle.contains(normCandTitle) -> score += 40
        }

        val expectedType = if (media.mediaType.equals("tv", ignoreCase = true)) 2 else 1
        if (candidate.subjectType == expectedType) score += 30

        val mediaYear = media.releaseDate.take(4)
        if (mediaYear.length == 4 && candidate.releaseYear.length == 4) {
            if (mediaYear == candidate.releaseYear) {
                score += 25
            } else {
                val diff = abs((mediaYear.toIntOrNull() ?: 0) - (candidate.releaseYear.toIntOrNull() ?: 0))
                if (diff <= 1) score += 10
            }
        }

        if (candidate.hasResource) score += 20
        if (candidate.detailPath.isNotBlank()) score += 5
        return score
    }

    fun pickOptionForPreferredQuality(
        options: List<MovieBoxDownloadOption>,
        preferred: PreferredDownloadQuality
    ): MovieBoxDownloadOption? {
        if (options.isEmpty()) return null
        val sorted = options.sortedByDescending { it.resolution }
        if (preferred == PreferredDownloadQuality.AUTO) return sorted.first()
        return sorted.firstOrNull { it.resolution == preferred.targetResolution }
            ?: sorted.minByOrNull { abs(it.resolution - preferred.targetResolution) }
            ?: sorted.first()
    }

    private fun md5Hex(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): String {
        val md = MessageDigest.getInstance("MD5")
        md.update(bytes, offset, length)
        return md.digest().joinToString("") { "%02x".format(Locale.ROOT, it) }
    }

    private fun parseResolutionNumber(raw: Any?): Int {
        return when (raw) {
            is Number -> raw.toInt()
            is String -> {
                val all = Regex("(\\d{3,4})").findAll(raw).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
                all.maxOrNull() ?: 0
            }
            else -> 0
        }
    }

    private fun inferFormatFromUrl(url: String): String {
        val clean = url.substringBefore('?').lowercase(Locale.US)
        return when {
            clean.endsWith(".mkv") -> "MKV"
            clean.endsWith(".webm") -> "WEBM"
            clean.endsWith(".m3u8") -> "HLS"
            else -> "MP4"
        }
    }

    private fun normalizeTitle(raw: String): String {
        return raw.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]"), "")
    }

    fun extractLanguageFromTitle(title: String): String {
        val bracketMatches = Regex("[\\[(]([^\\])]+)[\\])]").findAll(title)
        for (match in bracketMatches) {
            val inner = match.groupValues[1].trim()
            val detected = matchKnownLanguageKeyword(inner)
            if (detected.isNotBlank()) return detected
        }
        val dashSuffix = title.substringAfterLast(" - ", "").trim()
        if (dashSuffix.isNotBlank() && dashSuffix != title) {
            val detected = matchKnownLanguageKeyword(dashSuffix)
            if (detected.isNotBlank()) return detected
        }
        return ""
    }

    fun stripTitleLanguageAndSeason(title: String): String {
        var cleaned = title
            .replace(Regex("\\bS\\d+(?:-S\\d+)?\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bSeason\\s*\\d+\\b", RegexOption.IGNORE_CASE), "")
        cleaned = Regex("[\\[(]([^\\])]+)[\\])]").replace(cleaned) { match ->
            val inner = match.groupValues[1].trim()
            if (matchKnownLanguageKeyword(inner).isNotBlank() ||
                inner.contains("dub", ignoreCase = true) ||
                inner.contains("audio", ignoreCase = true)
            ) {
                ""
            } else {
                match.value
            }
        }
        return cleaned.replace(Regex("\\s+"), " ").trim()
    }

    fun normalizeLanguageLabel(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        val cleaned = trimmed
            .replace(Regex("\\b(dubbed|dub|audio|version|sub)\\b", RegexOption.IGNORE_CASE), "")
            .trim()
        if (cleaned.isBlank()) return trimmed
        return LANGUAGE_CODE_MAP[cleaned.lowercase(Locale.ROOT)]
            ?: cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    }

    private fun matchKnownLanguageKeyword(raw: String): String {
        val cleaned = raw
            .replace(Regex("\\b(dubbed|dub|audio|version)\\b", RegexOption.IGNORE_CASE), "")
            .trim()
        if (cleaned.isBlank()) return ""
        val lower = cleaned.lowercase(Locale.ROOT)
        LANGUAGE_CODE_MAP[lower]?.let { return it }
        KNOWN_LANGUAGE_NAMES.firstOrNull { it.equals(lower, ignoreCase = true) }?.let { known ->
            return known.replaceFirstChar { it.titlecase(Locale.US) }
        }
        if (raw.contains("dual audio", ignoreCase = true)) return "Dual Audio"
        return ""
    }

    private val LANGUAGE_CODE_MAP = mapOf(
        "en" to "English",
        "eng" to "English",
        "hi" to "Hindi",
        "hin" to "Hindi",
        "bn" to "Bengali",
        "ben" to "Bengali",
        "bangla" to "Bengali",
        "ta" to "Tamil",
        "tam" to "Tamil",
        "te" to "Telugu",
        "tel" to "Telugu",
        "ml" to "Malayalam",
        "kn" to "Kannada",
        "mr" to "Marathi",
        "pa" to "Punjabi",
        "ur" to "Urdu",
        "es" to "Spanish",
        "spa" to "Spanish",
        "fr" to "French",
        "fre" to "French",
        "ar" to "Arabic",
        "ara" to "Arabic",
        "id" to "Indonesian",
        "ind" to "Indonesian",
        "ja" to "Japanese",
        "jp" to "Japanese",
        "jpn" to "Japanese",
        "ko" to "Korean",
        "kor" to "Korean",
        "zh" to "Chinese",
        "cn" to "Chinese",
        "th" to "Thai",
        "vi" to "Vietnamese",
        "tr" to "Turkish",
        "ru" to "Russian",
        "pt" to "Portuguese",
        "de" to "German",
        "it" to "Italian",
        "tl" to "Tagalog",
        "fil" to "Filipino"
    )

    private val KNOWN_LANGUAGE_NAMES = setOf(
        "english", "hindi", "bengali", "bangla", "tamil", "telugu", "malayalam",
        "kannada", "marathi", "punjabi", "urdu", "spanish", "french", "arabic",
        "indonesian", "japanese", "korean", "chinese", "mandarin", "cantonese",
        "thai", "vietnamese", "turkish", "russian", "portuguese", "german",
        "italian", "tagalog", "filipino", "persian", "farsi", "swahili", "hausa"
    )

    private fun Any?.asMapOrNull(): Map<String, Any?>? {
        @Suppress("UNCHECKED_CAST")
        return this as? Map<String, Any?>
    }

    private fun Any?.asMapListOrEmpty(): List<Map<String, Any?>> {
        val list = this as? List<*> ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        return list.mapNotNull { it as? Map<String, Any?> }
    }

    private fun Any?.asCleanString(): String {
        return when (this) {
            null -> ""
            is String -> this.trim()
            is Number -> {
                val d = this.toDouble()
                if (d % 1.0 == 0.0) this.toLong().toString() else this.toString()
            }
            else -> this.toString().trim()
        }
    }

    private fun Any?.asIntOrNull(): Int? {
        return when (this) {
            is Number -> this.toInt()
            is String -> this.trim().toIntOrNull()
            else -> null
        }
    }

    private fun Any?.asLongOrNull(): Long? {
        return when (this) {
            is Number -> this.toLong()
            is String -> this.trim().toLongOrNull()
            else -> null
        }
    }
}
