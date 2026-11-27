package com.example.data.download

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.data.local.AppDatabase
import com.example.data.local.MediaDao
import com.example.data.model.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaItem
import com.example.data.model.MovieBoxDownloadOption
import com.example.data.model.MovieBoxSubtitleOption
import com.example.data.model.TmdbCanonicalMetadata
import com.example.data.remote.MovieBoxApiClient
import com.example.data.remote.TmdbMetadataResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

data class BatchEpisodeDownloadRequest(
    val media: MediaItem,
    val season: Int,
    val episode: Int,
    val episodeTitle: String,
    val sourceUrl: String,
    val qualityLabel: String = "Direct HD",
    val format: String = "MP4",
    val subtitleUrl: String = "",
    val subtitleLabel: String = ""
)

class DownloadEngine(
    private val context: Context,
    private val mediaDao: MediaDao
) {
    private val appContext = context.applicationContext

    suspend fun enqueueDownload(
        media: MediaItem,
        season: Int,
        episode: Int,
        episodeTitle: String,
        option: MovieBoxDownloadOption,
        subtitle: MovieBoxSubtitleOption? = null,
        providerName: String = DEFAULT_CLOUD_PROVIDER,
        wifiOnly: Boolean = false
    ): Result<DownloadEntity> = withContext(Dispatchers.IO) {
        val cleanUrl = option.downloadUrl.trim()
        if (!cleanUrl.startsWith("http://", ignoreCase = true) &&
            !cleanUrl.startsWith("https://", ignoreCase = true)
        ) {
            return@withContext Result.failure(
                IllegalArgumentException("Only direct HTTP/HTTPS video links can be downloaded offline.")
            )
        }

        val safeSeason = if (media.mediaType.equals("tv", ignoreCase = true)) season.coerceAtLeast(0) else 1
        val safeEpisode = if (media.mediaType.equals("tv", ignoreCase = true)) episode.coerceAtLeast(1) else 1
        val langSuffix = option.language.trim()
            .takeIf { it.isNotBlank() && !it.equals("Default", ignoreCase = true) }
        val effectiveQualityLabel = if (langSuffix != null) {
            "${option.resolutionLabel} ($langSuffix)"
        } else {
            option.resolutionLabel
        }
        val downloadId = buildDownloadId(
            mediaId = media.id,
            mediaType = media.mediaType,
            season = safeSeason,
            episode = safeEpisode,
            qualityLabel = effectiveQualityLabel
        )

        val resolvedTmdbId = media.resolvedTmdbId()
        val existingByTmdb = if (resolvedTmdbId != null) {
            mediaDao.getDownloadsByTmdbId(resolvedTmdbId, media.mediaType).firstOrNull { dl ->
                (!media.mediaType.equals("tv", ignoreCase = true) ||
                    (dl.season == safeSeason && dl.episode == safeEpisode)) &&
                    dl.qualityLabel.equals(effectiveQualityLabel, ignoreCase = true)
            }
        } else {
            null
        }
        val existing = mediaDao.getDownloadById(downloadId) ?: existingByTmdb
        if (existing?.parsedStatus == DownloadStatus.COMPLETED &&
            existing.localFilePath.isNotBlank() &&
            File(existing.localFilePath).let { it.exists() && it.length() > 0L }
        ) {
            return@withContext Result.success(existing)
        }

        cancelWorkQuietly(downloadId)
        if (existing != null) {
            if (existing.id != downloadId) {
                cancelWorkQuietly(existing.id)
            }
            if (existing.systemDownloadId > 0L) {
                runCatching { getLegacyDownloadManager()?.remove(existing.systemDownloadId) }
            }
            deleteLocalFileIfExists(existing.localFilePath)
            deleteLocalFileIfExists("${existing.localFilePath}.part")
        }

        val ext = inferExtension(option.format, cleanUrl)
        val relativeFileName = buildSafeFileName(
            title = media.title,
            mediaType = media.mediaType,
            season = safeSeason,
            episode = safeEpisode,
            qualityLabel = effectiveQualityLabel,
            extension = ext
        )
        val folder = resolveDownloadFolder(appContext)
        val destinationFile = File(folder, relativeFileName)
        deleteLocalFileIfExists(destinationFile.absolutePath)
        deleteLocalFileIfExists("${destinationFile.absolutePath}.part")

        val isManaged = isManagedApiProvider(providerName)
        val effectiveReferer = option.refererHeader.ifBlank {
            if (isManaged) MovieBoxDownloadOption.DEFAULT_REFERER else ""
        }
        val effectiveUa = option.userAgentHeader.ifBlank { MovieBoxDownloadOption.DEFAULT_USER_AGENT }

        val now = System.currentTimeMillis()
        val entity = DownloadEntity(
            id = downloadId,
            systemDownloadId = IN_APP_ENGINE_ID,
            mediaId = media.id,
            tmdbId = resolvedTmdbId,
            title = media.title,
            posterPath = media.posterPath,
            backdropPath = media.backdropPath,
            mediaType = media.mediaType,
            season = safeSeason,
            episode = safeEpisode,
            episodeTitle = episodeTitle,
            qualityLabel = effectiveQualityLabel,
            format = option.format,
            providerName = providerName,
            sourceUrl = cleanUrl,
            localFilePath = destinationFile.absolutePath,
            localUri = Uri.fromFile(destinationFile).toString(),
            subtitleUrl = subtitle?.url.orEmpty(),
            subtitleLabel = subtitle?.languageName.orEmpty(),
            status = DownloadStatus.QUEUED.storageValue,
            bytesDownloaded = 0L,
            totalBytes = option.sizeBytes,
            progressPercent = 0,
            errorReason = "",
            refererHeader = effectiveReferer,
            userAgentHeader = effectiveUa,
            cookieHeader = option.cookieHeader,
            wifiOnly = wifiOnly,
            createdAt = now,
            updatedAt = now,
            completedAt = 0L
        )
        mediaDao.upsertDownload(entity)
        reconcileQueue(appContext)

        Result.success(mediaDao.getDownloadById(downloadId) ?: entity)
    }

    suspend fun enqueueDirectUrlDownload(
        media: MediaItem,
        season: Int = 1,
        episode: Int = 1,
        episodeTitle: String = "",
        directUrl: String,
        wifiOnly: Boolean = false,
        subtitleUrl: String = "",
        subtitleLabel: String = ""
    ): Result<DownloadEntity> {
        val cleanUrl = directUrl.trim()
        val format = inferExtension("", cleanUrl).uppercase(Locale.US)
        val qualityLabel = "Direct HD"
        val opt = MovieBoxDownloadOption(
            id = "direct_${media.id}_s${season}_e${episode}",
            resolution = 1080,
            resolutionLabel = qualityLabel,
            sizeBytes = 0L,
            downloadUrl = cleanUrl,
            format = format,
            refererHeader = "",
            userAgentHeader = MovieBoxDownloadOption.DEFAULT_USER_AGENT
        )
        val subtitleOption = buildDirectSubtitleOption(
            mediaId = media.id,
            season = season,
            episode = episode,
            subtitleUrl = subtitleUrl.ifBlank { media.subtitleUrl },
            subtitleLabel = subtitleLabel.ifBlank { media.subtitleLabel }
        )
        return enqueueDownload(
            media = media,
            season = season,
            episode = episode,
            episodeTitle = episodeTitle,
            option = opt,
            subtitle = subtitleOption,
            providerName = "Direct URL",
            wifiOnly = wifiOnly
        )
    }

    suspend fun enqueueBatchDownloads(
        requests: List<BatchEpisodeDownloadRequest>,
        wifiOnly: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        var addedCount = 0
        val folder = resolveDownloadFolder(appContext)
        val existingByMedia = requests.map { it.media.id }.distinct()
            .associateWith { id -> mediaDao.getDownloadsByMediaId(id) }

        requests.forEachIndexed { idx, req ->
            val cleanUrl = req.sourceUrl.trim()
            if (!cleanUrl.startsWith("http://", ignoreCase = true) &&
                !cleanUrl.startsWith("https://", ignoreCase = true)
            ) {
                return@forEachIndexed
            }

            val safeSeason = if (req.media.mediaType.equals("tv", ignoreCase = true)) req.season.coerceAtLeast(0) else 1
            val safeEpisode = if (req.media.mediaType.equals("tv", ignoreCase = true)) req.episode.coerceAtLeast(1) else 1

            val resolvedTmdbId = req.media.resolvedTmdbId()
            val existingByMediaList = existingByMedia[req.media.id].orEmpty()
            val existingByTmdbList = if (resolvedTmdbId != null) {
                mediaDao.getDownloadsByTmdbId(resolvedTmdbId, req.media.mediaType)
            } else {
                emptyList()
            }
            val existingEp = (existingByMediaList + existingByTmdbList)
                .firstOrNull { it.season == safeSeason && it.episode == safeEpisode }

            val alreadyDoneOrActive = existingEp != null && (
                (existingEp.parsedStatus == DownloadStatus.COMPLETED &&
                    existingEp.localFilePath.isNotBlank() &&
                    File(existingEp.localFilePath).let { it.exists() && it.length() > 0L }) ||
                    existingEp.parsedStatus.isActive
                )
            if (alreadyDoneOrActive) return@forEachIndexed

            val ext = inferExtension(req.format, cleanUrl)
            val downloadId = buildDownloadId(
                mediaId = req.media.id,
                mediaType = req.media.mediaType,
                season = safeSeason,
                episode = safeEpisode,
                qualityLabel = req.qualityLabel
            )
            val fileName = buildSafeFileName(
                title = req.media.title,
                mediaType = req.media.mediaType,
                season = safeSeason,
                episode = safeEpisode,
                qualityLabel = req.qualityLabel,
                extension = ext
            )
            val destFile = File(folder, fileName)
            deleteLocalFileIfExists(destFile.absolutePath)
            deleteLocalFileIfExists("${destFile.absolutePath}.part")

            val resolvedBatchSub = buildDirectSubtitleOption(
                mediaId = req.media.id,
                season = safeSeason,
                episode = safeEpisode,
                subtitleUrl = req.subtitleUrl.ifBlank { req.media.subtitleUrl },
                subtitleLabel = req.subtitleLabel.ifBlank { req.media.subtitleLabel }
            )
            val now = System.currentTimeMillis() + idx
            val entity = DownloadEntity(
                id = downloadId,
                systemDownloadId = IN_APP_ENGINE_ID,
                mediaId = req.media.id,
                tmdbId = resolvedTmdbId,
                title = req.media.title,
                posterPath = req.media.posterPath,
                backdropPath = req.media.backdropPath,
                mediaType = req.media.mediaType,
                season = safeSeason,
                episode = safeEpisode,
                episodeTitle = req.episodeTitle,
                qualityLabel = req.qualityLabel,
                format = ext.uppercase(Locale.US),
                providerName = "Direct URL",
                sourceUrl = cleanUrl,
                localFilePath = destFile.absolutePath,
                localUri = Uri.fromFile(destFile).toString(),
                subtitleUrl = resolvedBatchSub?.url.orEmpty(),
                subtitleLabel = resolvedBatchSub?.languageName.orEmpty(),
                status = DownloadStatus.QUEUED.storageValue,
                bytesDownloaded = 0L,
                totalBytes = 0L,
                progressPercent = 0,
                errorReason = "",
                refererHeader = "",
                userAgentHeader = MovieBoxDownloadOption.DEFAULT_USER_AGENT,
                wifiOnly = wifiOnly,
                createdAt = now,
                updatedAt = now,
                completedAt = 0L
            )
            mediaDao.upsertDownload(entity)
            addedCount++
        }

        if (addedCount > 0) {
            reconcileQueue(appContext)
        }
        addedCount
    }

    suspend fun pauseDownload(downloadId: String) = withContext(Dispatchers.IO) {
        val existing = mediaDao.getDownloadById(downloadId) ?: return@withContext
        val partFile = File("${existing.localFilePath}.part")
        val partialBytes = if (partFile.exists()) partFile.length() else existing.bytesDownloaded
        val total = maxOf(existing.totalBytes, partialBytes)
        mediaDao.upsertDownload(
            existing.copy(
                status = DownloadStatus.PAUSED.storageValue,
                bytesDownloaded = partialBytes,
                totalBytes = total,
                progressPercent = computeProgressPercent(partialBytes, total),
                errorReason = "Paused",
                updatedAt = System.currentTimeMillis()
            )
        )
        cancelWorkQuietly(downloadId)
        reconcileQueue(appContext)
    }

    suspend fun resumeDownload(downloadId: String) = withContext(Dispatchers.IO) {
        val existing = mediaDao.getDownloadById(downloadId) ?: return@withContext
        val destFile = existing.localFilePath.takeIf { it.isNotBlank() }?.let { File(it) }
        if (destFile != null && destFile.exists() && destFile.length() > 0L) {
            val finalSize = destFile.length()
            mediaDao.upsertDownload(
                existing.copy(
                    status = DownloadStatus.COMPLETED.storageValue,
                    bytesDownloaded = finalSize,
                    totalBytes = maxOf(existing.totalBytes, finalSize),
                    progressPercent = 100,
                    errorReason = "",
                    updatedAt = System.currentTimeMillis(),
                    completedAt = System.currentTimeMillis()
                )
            )
            return@withContext
        }

        val partFile = File("${existing.localFilePath}.part")
        val partialBytes = if (partFile.exists()) partFile.length() else existing.bytesDownloaded
        val total = maxOf(existing.totalBytes, partialBytes)
        mediaDao.upsertDownload(
            existing.copy(
                status = DownloadStatus.QUEUED.storageValue,
                bytesDownloaded = partialBytes,
                totalBytes = total,
                progressPercent = computeProgressPercent(partialBytes, total),
                errorReason = "",
                updatedAt = System.currentTimeMillis()
            )
        )
        reconcileQueue(appContext)
    }

    suspend fun cancelAndRemoveDownload(downloadId: String) = withContext(Dispatchers.IO) {
        cancelWorkQuietly(downloadId)
        val existing = mediaDao.getDownloadById(downloadId) ?: return@withContext
        if (existing.systemDownloadId > 0L) {
            runCatching { getLegacyDownloadManager()?.remove(existing.systemDownloadId) }
        }
        deleteLocalFileIfExists(existing.localFilePath)
        deleteLocalFileIfExists("${existing.localFilePath}.part")
        mediaDao.deleteDownloadById(downloadId)
        reconcileQueue(appContext)
    }

    suspend fun retryDownload(
        download: DownloadEntity,
        wifiOnly: Boolean = false
    ): Result<DownloadEntity> = withContext(Dispatchers.IO) {
        cancelWorkQuietly(download.id)
        val baseMedia = MediaItem(
            id = download.mediaId,
            tmdbId = download.tmdbId ?: download.mediaId.toLongOrNull()?.takeIf { it > 0L },
            title = download.title,
            overview = "",
            posterPath = download.posterPath,
            backdropPath = download.backdropPath,
            mediaType = download.mediaType
        )
        val canonical = TmdbMetadataResolver.resolveForMedia(baseMedia)
        val media = TmdbMetadataResolver.enrichMediaItem(baseMedia, canonical)
        val targetRes = Regex("(\\d{3,4})").find(download.qualityLabel)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 720
        val freshOption = if (isManagedApiProvider(download.providerName)) {
            resolveFreshOptionForDownload(media, download.season, download.episode, targetRes, canonical)
        } else {
            null
        }

        val partFile = File("${download.localFilePath}.part")
        val partialBytes = if (partFile.exists()) partFile.length() else 0L
        val updatedUrl = freshOption?.downloadUrl?.takeIf { it.isNotBlank() } ?: download.sourceUrl
        val now = System.currentTimeMillis()
        val requeued = download.copy(
            tmdbId = media.resolvedTmdbId() ?: download.tmdbId,
            sourceUrl = updatedUrl,
            refererHeader = freshOption?.refererHeader ?: download.refererHeader,
            userAgentHeader = freshOption?.userAgentHeader ?: download.userAgentHeader,
            cookieHeader = freshOption?.cookieHeader ?: download.cookieHeader,
            wifiOnly = wifiOnly,
            status = DownloadStatus.QUEUED.storageValue,
            bytesDownloaded = partialBytes,
            totalBytes = maxOf(download.totalBytes, freshOption?.sizeBytes ?: 0L),
            progressPercent = computeProgressPercent(partialBytes, download.totalBytes),
            errorReason = "",
            updatedAt = now
        )
        mediaDao.upsertDownload(requeued)
        reconcileQueue(appContext)
        Result.success(mediaDao.getDownloadById(download.id) ?: requeued)
    }

    suspend fun syncDownloads(): List<DownloadEntity> = withContext(Dispatchers.IO) {
        val current = mediaDao.getAllDownloadsOnce()
        if (current.isEmpty()) return@withContext current

        val wm = runCatching { WorkManager.getInstance(appContext) }.getOrNull()
        val now = System.currentTimeMillis()
        var needsReconcile = false

        for (item in current) {
            val destFile = item.localFilePath.takeIf { it.isNotBlank() }?.let { File(it) }
            if (item.parsedStatus != DownloadStatus.COMPLETED &&
                destFile != null && destFile.exists() && destFile.length() > 0L
            ) {
                val fileSize = destFile.length()
                mediaDao.upsertDownload(
                    item.copy(
                        status = DownloadStatus.COMPLETED.storageValue,
                        bytesDownloaded = fileSize,
                        totalBytes = maxOf(item.totalBytes, fileSize),
                        progressPercent = 100,
                        errorReason = "",
                        updatedAt = now,
                        completedAt = if (item.completedAt > 0L) item.completedAt else now
                    )
                )
                needsReconcile = true
                continue
            }

            if (item.parsedStatus == DownloadStatus.PAUSED &&
                item.errorReason.contains("Wi-Fi", ignoreCase = true) &&
                isWifiConnected()
            ) {
                mediaDao.upsertDownload(
                    item.copy(
                        status = DownloadStatus.QUEUED.storageValue,
                        errorReason = "",
                        updatedAt = now
                    )
                )
                needsReconcile = true
                continue
            }

            if (item.parsedStatus == DownloadStatus.RUNNING) {
                val infos = wm?.getWorkInfosForUniqueWork(workNameFor(item.id))?.get().orEmpty()
                val isWorkAlive = infos.any {
                    it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
                }
                if (!isWorkAlive) {
                    val partFile = File("${item.localFilePath}.part")
                    val partialBytes = if (partFile.exists()) partFile.length() else item.bytesDownloaded
                    mediaDao.upsertDownload(
                        item.copy(
                            systemDownloadId = IN_APP_ENGINE_ID,
                            status = DownloadStatus.QUEUED.storageValue,
                            bytesDownloaded = partialBytes,
                            totalBytes = maxOf(item.totalBytes, partialBytes),
                            progressPercent = computeProgressPercent(partialBytes, item.totalBytes),
                            errorReason = "",
                            updatedAt = now
                        )
                    )
                    needsReconcile = true
                }
            } else if (item.parsedStatus == DownloadStatus.QUEUED) {
                needsReconcile = true
            }
        }

        if (needsReconcile) {
            reconcileQueue(appContext)
        }
        mediaDao.getAllDownloadsOnce()
    }

    suspend fun exportToPublicDownloads(download: DownloadEntity): Result<String> = withContext(Dispatchers.IO) {
        val sourceFile = File(download.localFilePath)
        if (!sourceFile.exists() || sourceFile.length() <= 0L) {
            return@withContext Result.failure(IllegalStateException("Local video file not found."))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val resolver = appContext.contentResolver
                val mime = when (download.format.uppercase(Locale.US)) {
                    "MKV" -> "video/x-matroska"
                    "WEBM" -> "video/webm"
                    else -> "video/mp4"
                }
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, sourceFile.name)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Cinetra")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, values)
                    ?: throw IllegalStateException("Unable to create entry in Downloads folder.")
                resolver.openOutputStream(itemUri)?.use { out ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                } ?: throw IllegalStateException("Unable to write to Downloads folder.")
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
                "Saved to Downloads/Cinetra/${sourceFile.name}"
            }
        } else {
            Result.success("Saved offline in Cinetra library (${sourceFile.name})")
        }
    }

    private suspend fun resolveFreshOptionForDownload(
        media: MediaItem,
        season: Int,
        episode: Int,
        targetResolution: Int,
        canonical: TmdbCanonicalMetadata? = null
    ): MovieBoxDownloadOption? {
        val subj = MovieBoxApiClient.resolveSubjectForMedia(
            media = media,
            season = season,
            canonicalMetadata = canonical
        ).getOrNull() ?: return null
        val bundle = MovieBoxApiClient.fetchDownloadBundle(
            subject = subj,
            season = season,
            episode = episode,
            isTv = media.mediaType.equals("tv", ignoreCase = true) || subj.subjectType == 2
        ).getOrNull() ?: return null
        val opts = bundle.options
        if (opts.isEmpty()) return null
        return opts.firstOrNull { it.resolution == targetResolution }
            ?: opts.minByOrNull { kotlin.math.abs(it.resolution - targetResolution) }
            ?: opts.firstOrNull()
    }

    private fun cancelWorkQuietly(downloadId: String) {
        runCatching {
            WorkManager.getInstance(appContext).cancelUniqueWork(workNameFor(downloadId))
        }
    }

    private fun isWifiConnected(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    private fun getLegacyDownloadManager(): DownloadManager? {
        return appContext.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    }

    private fun deleteLocalFileIfExists(path: String) {
        if (path.isBlank()) return
        runCatching {
            val f = File(path)
            if (f.exists()) f.delete()
        }
    }

    companion object {
        const val MAX_CONCURRENT_DOWNLOADS = 2
        const val DOWNLOAD_CHANNEL_ID = "cinetra_offline_downloads"
        const val DEFAULT_CLOUD_PROVIDER = "Cloud HD"
        private const val IN_APP_ENGINE_ID = -2L
        private val queueMutex = Mutex()

        fun isManagedApiProvider(providerName: String): Boolean {
            return providerName.contains("MovieBox", ignoreCase = true) ||
                providerName.contains("Cloud HD", ignoreCase = true) ||
                providerName.contains("HD Server", ignoreCase = true)
        }

        fun workNameFor(downloadId: String): String = "download_$downloadId"

        fun computeSlotsToPromote(
            runningCount: Int,
            queuedCount: Int,
            maxConcurrent: Int = MAX_CONCURRENT_DOWNLOADS
        ): Int {
            val openSlots = (maxConcurrent - runningCount.coerceAtLeast(0)).coerceAtLeast(0)
            return minOf(openSlots, queuedCount.coerceAtLeast(0))
        }

        suspend fun reconcileQueue(context: Context) = withContext(Dispatchers.IO) {
            queueMutex.withLock {
                val appCtx = context.applicationContext
                val dao = AppDatabase.getDatabase(appCtx).mediaDao()
                val wm = runCatching { WorkManager.getInstance(appCtx) }.getOrNull() ?: return@withLock

                val runningCount = dao.countRunningDownloads()
                val queued = dao.getQueuedDownloadsOrdered()
                val toStart = computeSlotsToPromote(runningCount, queued.size, MAX_CONCURRENT_DOWNLOADS)
                if (toStart <= 0) return@withLock

                val now = System.currentTimeMillis()
                queued.take(toStart).forEach { entity ->
                    dao.upsertDownload(
                        entity.copy(
                            status = DownloadStatus.RUNNING.storageValue,
                            updatedAt = now
                        )
                    )
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(if (entity.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                        .build()
                    val input = Data.Builder()
                        .putString(DownloadWorker.KEY_DOWNLOAD_ID, entity.id)
                        .build()
                    val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                        .setConstraints(constraints)
                        .setInputData(input)
                        .addTag(workNameFor(entity.id))
                        .build()
                    wm.enqueueUniqueWork(
                        workNameFor(entity.id),
                        ExistingWorkPolicy.KEEP,
                        request
                    )
                }
            }
        }

        fun resolveDownloadFolder(context: Context): File {
            val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            return File(base, "cinetra").apply { mkdirs() }
        }

        fun buildHeaderProfiles(
            refererHeader: String,
            userAgentHeader: String,
            cookieHeader: String,
            targetUrl: String
        ): List<Map<String, String>> {
            val ref = refererHeader.trim()
            val ua = userAgentHeader.trim().ifBlank { MovieBoxDownloadOption.DEFAULT_USER_AGENT }
            val cookie = cookieHeader.trim()
            val origin = if (ref.isNotBlank()) {
                val scheme = ref.substringBefore("://", "https")
                val hostPort = ref.substringAfter("://", ref).substringBefore('/')
                if (hostPort.isNotBlank()) "$scheme://$hostPort" else ref.trimEnd('/')
            } else {
                ""
            }
            val targetHostOrigin = runCatching {
                val scheme = targetUrl.substringBefore("://", "https")
                val hostPort = targetUrl.substringAfter("://", "").substringBefore('/')
                if (hostPort.isNotBlank()) "$scheme://$hostPort/" else ""
            }.getOrDefault("")

            return buildList {
                add(buildMap {
                    if (ref.isNotBlank()) put("Referer", ref)
                    if (origin.isNotBlank()) put("Origin", origin)
                    put("User-Agent", ua)
                    if (cookie.isNotBlank()) put("Cookie", cookie)
                })
                if (ref.isNotBlank()) {
                    add(buildMap {
                        put("Referer", ref)
                        put("User-Agent", ua)
                        if (cookie.isNotBlank()) put("Cookie", cookie)
                    })
                }
                if (targetHostOrigin.isNotBlank() && targetHostOrigin != ref) {
                    add(buildMap {
                        put("Referer", targetHostOrigin)
                        put("User-Agent", ua)
                        if (cookie.isNotBlank()) put("Cookie", cookie)
                    })
                }
                add(buildMap {
                    put("User-Agent", ua)
                    if (cookie.isNotBlank()) put("Cookie", cookie)
                })
            }.distinct()
        }

        fun parseTotalBytesFromContentRange(contentRangeHeader: String?): Long {
            if (contentRangeHeader.isNullOrBlank()) return -1L
            val totalPart = contentRangeHeader.substringAfter('/', "").trim()
            return totalPart.toLongOrNull()?.takeIf { it > 0L } ?: -1L
        }

        fun isHtmlOrJsonErrorContentType(contentType: String?): Boolean {
            val lower = contentType?.trim()?.lowercase(Locale.US).orEmpty()
            return lower.startsWith("text/html") ||
                lower.startsWith("application/json") ||
                lower.startsWith("text/plain")
        }

        fun mapHttpStatusToReadableError(code: Int): String {
            return when (code) {
                401, 403 -> "Download link expired or blocked by CDN (HTTP $code). Tap Retry."
                404, 410 -> "Video file no longer available on this server (HTTP $code)."
                429 -> "CDN rejected download request (HTTP 429). Tap Retry for a fresh link."
                in 500..599 -> "CDN temporarily unavailable (HTTP $code)."
                else -> "Server rejected download request (HTTP $code)."
            }
        }

        fun buildDirectSubtitleOption(
            mediaId: String,
            season: Int,
            episode: Int,
            subtitleUrl: String,
            subtitleLabel: String = ""
        ): MovieBoxSubtitleOption? {
            val cleanSubUrl = subtitleUrl.trim()
            if (!cleanSubUrl.startsWith("http://", ignoreCase = true) &&
                !cleanSubUrl.startsWith("https://", ignoreCase = true)
            ) {
                return null
            }
            val cleanLabel = subtitleLabel.trim()
            return MovieBoxSubtitleOption(
                id = "direct_sub_${mediaId}_s${season}_e${episode}",
                languageCode = cleanLabel.take(16).ifBlank { "en" },
                languageName = cleanLabel.ifBlank { "English" },
                url = cleanSubUrl
            )
        }

        fun buildDownloadId(
            mediaId: String,
            mediaType: String,
            season: Int,
            episode: Int,
            qualityLabel: String
        ): String {
            val epTag = if (mediaType.equals("tv", ignoreCase = true)) "s${season}_e${episode}" else "movie"
            val qTag = qualityLabel.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_")
            return "${mediaId}_${epTag}_${qTag}"
        }

        fun buildSafeFileName(
            title: String,
            mediaType: String,
            season: Int,
            episode: Int,
            qualityLabel: String,
            extension: String
        ): String {
            val cleanTitle = title.trim()
                .replace(Regex("[^A-Za-z0-9._\\- ]+"), "")
                .replace(Regex("\\s+"), "_")
                .take(40)
                .ifBlank { "Cinetra_Video" }
            val epPart = if (mediaType.equals("tv", ignoreCase = true)) "_S${season}E${episode}" else ""
            val qPart = qualityLabel
                .replace(Regex("[^A-Za-z0-9]+"), "_")
                .trim('_')
                .ifBlank { "HD" }
            val ext = extension.lowercase(Locale.US).trimStart('.').ifBlank { "mp4" }
            return "${cleanTitle}${epPart}_${qPart}.${ext}"
        }

        fun inferExtension(format: String, url: String): String {
            val f = format.trim().lowercase(Locale.US)
            return when {
                f == "mkv" -> "mkv"
                f == "webm" -> "webm"
                url.substringBefore('?').endsWith(".mkv", ignoreCase = true) -> "mkv"
                url.substringBefore('?').endsWith(".webm", ignoreCase = true) -> "webm"
                else -> "mp4"
            }
        }

        fun computeProgressPercent(downloaded: Long, total: Long): Int {
            if (total <= 0L || downloaded <= 0L) return 0
            return ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
        }

        fun mapDownloadManagerReason(reasonCode: Int): String {
            return when (reasonCode) {
                DownloadManager.ERROR_CANNOT_RESUME -> "Download interrupted and cannot resume."
                DownloadManager.ERROR_DEVICE_NOT_FOUND -> "External storage unavailable."
                DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists."
                DownloadManager.ERROR_FILE_ERROR -> "Storage write error."
                DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP stream error from host."
                DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient device storage space."
                DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many CDN redirects."
                DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Server rejected download request."
                else -> "Download failed (code $reasonCode)."
            }
        }

        fun mapPauseReason(reasonCode: Int): String {
            return when (reasonCode) {
                DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Waiting for network..."
                DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Waiting for Wi-Fi connection..."
                DownloadManager.PAUSED_WAITING_TO_RETRY -> "Temporary error, retrying..."
                else -> "Paused"
            }
        }
    }
}
