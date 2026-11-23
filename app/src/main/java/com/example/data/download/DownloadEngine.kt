package com.example.data.download

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.MediaDao
import com.example.data.model.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaItem
import com.example.data.model.MovieBoxDownloadOption
import com.example.data.model.MovieBoxSubtitleOption
import com.example.data.remote.MovieBoxApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

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
        providerName: String = "MovieBox API",
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

        val downloadId = buildDownloadId(
            mediaId = media.id,
            mediaType = media.mediaType,
            season = season,
            episode = episode,
            qualityLabel = option.resolutionLabel
        )

        // Cancel any active job or legacy DownloadManager task with the same ID
        activeJobs.remove(downloadId)?.cancel()
        mediaDao.getDownloadById(downloadId)?.let { existing ->
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
            season = season,
            episode = episode,
            qualityLabel = option.resolutionLabel,
            extension = ext
        )

        val folder = resolveDownloadFolder()
        val destinationFile = File(folder, relativeFileName)
        val partFile = File(folder, "$relativeFileName.part")
        deleteLocalFileIfExists(destinationFile.absolutePath)
        deleteLocalFileIfExists(partFile.absolutePath)

        val effectiveReferer = option.refererHeader.ifBlank { MovieBoxDownloadOption.DEFAULT_REFERER }
        val effectiveUa = option.userAgentHeader.ifBlank { MovieBoxDownloadOption.DEFAULT_USER_AGENT }
        val normalizedOption = option.copy(
            downloadUrl = cleanUrl,
            refererHeader = effectiveReferer,
            userAgentHeader = effectiveUa
        )

        val now = System.currentTimeMillis()
        val entity = DownloadEntity(
            id = downloadId,
            systemDownloadId = IN_APP_ENGINE_ID,
            mediaId = media.id,
            tmdbId = media.tmdbId,
            title = media.title,
            posterPath = media.posterPath,
            backdropPath = media.backdropPath,
            mediaType = media.mediaType,
            season = if (media.mediaType.equals("tv", ignoreCase = true)) season else 1,
            episode = if (media.mediaType.equals("tv", ignoreCase = true)) episode else 1,
            episodeTitle = episodeTitle,
            qualityLabel = option.resolutionLabel,
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
            errorReason = "Connecting to MovieBox CDN...",
            createdAt = now,
            updatedAt = now
        )
        mediaDao.upsertDownload(entity)

        val taskSpec = ActiveDownloadSpec(
            media = media,
            option = normalizedOption,
            subtitle = subtitle,
            wifiOnly = wifiOnly,
            relativeFileName = relativeFileName
        )
        activeSpecs[downloadId] = taskSpec
        startDownloadWorker(entity, taskSpec)

        Result.success(entity)
    }

    suspend fun syncDownloads(): List<DownloadEntity> = withContext(Dispatchers.IO) {
        val current = mediaDao.getAllDownloadsOnce()
        val activeItems = current.filter { it.parsedStatus.isActive }
        if (activeItems.isEmpty()) return@withContext current

        val now = System.currentTimeMillis()
        for (item in activeItems) {
            val runningJob = activeJobs[item.id]
            if (runningJob != null && runningJob.isActive) {
                continue
            }

            // If item was paused waiting for Wi-Fi and Wi-Fi is now available, auto-resume
            if (item.parsedStatus == DownloadStatus.PAUSED &&
                item.errorReason.contains("Wi-Fi", ignoreCase = true)
            ) {
                val spec = activeSpecs[item.id] ?: buildFallbackSpecFromEntity(item, wifiOnly = true)
                if (isWifiConnected()) {
                    activeSpecs[item.id] = spec
                    startDownloadWorker(item, spec)
                }
                continue
            }

            // Check if destination already finished
            val destFile = item.localFilePath.takeIf { it.isNotBlank() }?.let { File(it) }
            if (destFile != null && destFile.exists() && destFile.length() > 0L) {
                val fileSize = destFile.length()
                mediaDao.upsertDownload(
                    item.copy(
                        status = DownloadStatus.COMPLETED.storageValue,
                        bytesDownloaded = fileSize,
                        totalBytes = maxOf(item.totalBytes, fileSize),
                        progressPercent = 100,
                        errorReason = "",
                        updatedAt = now
                    )
                )
                continue
            }

            // Recover orphaned in-progress downloads once (e.g. after app process restart)
            if (item.parsedStatus == DownloadStatus.QUEUED || item.parsedStatus == DownloadStatus.DOWNLOADING) {
                if (item.systemDownloadId > 0L) {
                    // Migrate legacy DownloadManager items seamlessly to in-app OkHttp engine
                    runCatching { getLegacyDownloadManager()?.remove(item.systemDownloadId) }
                }
                if (autoResumedIds.add(item.id)) {
                    val spec = activeSpecs[item.id] ?: buildFallbackSpecFromEntity(item, wifiOnly = false)
                    activeSpecs[item.id] = spec
                    val resetItem = item.copy(
                        systemDownloadId = IN_APP_ENGINE_ID,
                        errorReason = "Resuming download..."
                    )
                    mediaDao.upsertDownload(resetItem)
                    startDownloadWorker(resetItem, spec)
                } else {
                    mediaDao.upsertDownload(
                        item.copy(
                            status = DownloadStatus.PAUSED.storageValue,
                            errorReason = "Paused — tap Cancel or Retry to continue.",
                            updatedAt = now
                        )
                    )
                }
            }
        }

        mediaDao.getAllDownloadsOnce()
    }

    suspend fun cancelAndRemoveDownload(downloadId: String) = withContext(Dispatchers.IO) {
        activeJobs.remove(downloadId)?.cancel()
        activeSpecs.remove(downloadId)
        val existing = mediaDao.getDownloadById(downloadId) ?: return@withContext
        if (existing.systemDownloadId > 0L) {
            runCatching { getLegacyDownloadManager()?.remove(existing.systemDownloadId) }
        }
        deleteLocalFileIfExists(existing.localFilePath)
        deleteLocalFileIfExists("${existing.localFilePath}.part")
        mediaDao.deleteDownloadById(downloadId)
    }

    suspend fun retryDownload(
        download: DownloadEntity,
        wifiOnly: Boolean = false
    ): Result<DownloadEntity> {
        val media = MediaItem(
            id = download.mediaId,
            tmdbId = download.tmdbId,
            title = download.title,
            overview = "",
            posterPath = download.posterPath,
            backdropPath = download.backdropPath,
            mediaType = download.mediaType
        )
        val cachedSpec = activeSpecs[download.id]
        val resolvedOption = cachedSpec?.option ?: run {
            val targetRes = Regex("(\\d{3,4})").find(download.qualityLabel)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 720
            // Try refreshing signed URLs from MovieBox API first so expired links don't fail again
            val refreshed = resolveFreshOptionForDownload(media, download.season, download.episode, targetRes)
            refreshed ?: MovieBoxDownloadOption(
                id = download.id,
                resolution = targetRes,
                resolutionLabel = download.qualityLabel,
                sizeBytes = download.totalBytes,
                downloadUrl = download.sourceUrl,
                format = download.format,
                refererHeader = MovieBoxDownloadOption.DEFAULT_REFERER,
                userAgentHeader = MovieBoxDownloadOption.DEFAULT_USER_AGENT
            )
        }
        val subtitle = cachedSpec?.subtitle ?: download.subtitleUrl.takeIf { it.isNotBlank() }?.let { subUrl ->
            MovieBoxSubtitleOption(
                id = "sub_${download.id}",
                languageCode = "en",
                languageName = download.subtitleLabel.ifBlank { "English" },
                url = subUrl
            )
        }
        return enqueueDownload(
            media = media,
            season = download.season,
            episode = download.episode,
            episodeTitle = download.episodeTitle,
            option = resolvedOption,
            subtitle = subtitle,
            providerName = download.providerName,
            wifiOnly = wifiOnly
        )
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

    private fun startDownloadWorker(initialEntity: DownloadEntity, spec: ActiveDownloadSpec) {
        val downloadId = initialEntity.id
        activeJobs[downloadId]?.cancel()

        val job = engineScope.launch {
            var currentEntity = initialEntity
            try {
                if (spec.wifiOnly && !isWifiConnected()) {
                    currentEntity = currentEntity.copy(
                        status = DownloadStatus.PAUSED.storageValue,
                        errorReason = "Waiting for Wi-Fi connection...",
                        updatedAt = System.currentTimeMillis()
                    )
                    mediaDao.upsertDownload(currentEntity)
                    return@launch
                }

                // Download optional subtitle first (fast small file)
                if (spec.subtitle != null && spec.subtitle.url.startsWith("http", ignoreCase = true)) {
                    downloadSubtitleFile(spec, currentEntity.season, currentEntity.episode)
                }

                val destFile = File(currentEntity.localFilePath).also { it.parentFile?.mkdirs() }
                val partFile = File("${destFile.absolutePath}.part")

                var activeOption = spec.option
                var refreshedOnce = false
                var lastFailure: Throwable? = null

                // Up to 3 attempts (initial + link refresh if CDN expired/403/429 + transient retry)
                for (attempt in 0 until 3) {
                    coroutineContext.ensureActive()
                    val streamOutcome = streamWithHeaderFallbacks(
                        entity = currentEntity,
                        option = activeOption,
                        partFile = partFile,
                        onProgress = { updated ->
                            currentEntity = updated
                            mediaDao.upsertDownload(updated)
                        }
                    )

                    if (streamOutcome.isSuccess) {
                        val finalTotal = streamOutcome.getOrThrow()
                        if (!partFile.exists() || partFile.length() <= 0L) {
                            throw IOException("Downloaded file is empty.")
                        }
                        if (destFile.exists()) {
                            runCatching { destFile.delete() }
                        }
                        if (!partFile.renameTo(destFile)) {
                            partFile.copyTo(destFile, overwrite = true)
                            runCatching { partFile.delete() }
                        }
                        val finalSize = destFile.length()
                        val completed = currentEntity.copy(
                            status = DownloadStatus.COMPLETED.storageValue,
                            bytesDownloaded = finalSize,
                            totalBytes = maxOf(finalSize, finalTotal),
                            progressPercent = 100,
                            localFilePath = destFile.absolutePath,
                            localUri = Uri.fromFile(destFile).toString(),
                            errorReason = "",
                            updatedAt = System.currentTimeMillis()
                        )
                        mediaDao.upsertDownload(completed)
                        activeSpecs.remove(downloadId)
                        postCompletedNotification(completed)
                        return@launch
                    }

                    val err = streamOutcome.exceptionOrNull()
                    lastFailure = err
                    if (err is CancellationException) throw err

                    val shouldRefreshLink = !refreshedOnce &&
                        currentEntity.providerName.contains("MovieBox", ignoreCase = true) &&
                        (err is HttpRejectedException && (err.code == 401 || err.code == 403 || err.code == 410 || err.code == 429))

                    if (shouldRefreshLink) {
                        refreshedOnce = true
                        currentEntity = currentEntity.copy(
                            status = DownloadStatus.DOWNLOADING.storageValue,
                            errorReason = "Refreshing MovieBox download link...",
                            updatedAt = System.currentTimeMillis()
                        )
                        mediaDao.upsertDownload(currentEntity)
                        val targetRes = if (activeOption.resolution > 0) activeOption.resolution else 720
                        val freshOpt = resolveFreshOptionForDownload(
                            media = spec.media,
                            season = currentEntity.season,
                            episode = currentEntity.episode,
                            targetResolution = targetRes
                        )
                        if (freshOpt != null && freshOpt.downloadUrl.isNotBlank()) {
                            activeOption = freshOpt
                            currentEntity = currentEntity.copy(sourceUrl = freshOpt.downloadUrl)
                            activeSpecs[downloadId] = spec.copy(option = freshOpt)
                            continue
                        }
                    }

                    // Brief backoff before retrying transient socket/CDN hiccup
                    if (attempt < 2) {
                        currentEntity = currentEntity.copy(
                            status = DownloadStatus.DOWNLOADING.storageValue,
                            errorReason = "Retrying connection (${attempt + 2}/3)...",
                            updatedAt = System.currentTimeMillis()
                        )
                        mediaDao.upsertDownload(currentEntity)
                        delay(1200L * (attempt + 1))
                    }
                }

                throw lastFailure ?: IOException("Unable to download video from host.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (err: Throwable) {
                val failed = currentEntity.copy(
                    status = DownloadStatus.FAILED.storageValue,
                    errorReason = err.message?.takeIf { it.isNotBlank() } ?: "Download failed.",
                    updatedAt = System.currentTimeMillis()
                )
                runCatching { mediaDao.upsertDownload(failed) }
            } finally {
                activeJobs.remove(downloadId)
            }
        }
        activeJobs[downloadId] = job
    }

    private suspend fun streamWithHeaderFallbacks(
        entity: DownloadEntity,
        option: MovieBoxDownloadOption,
        partFile: File,
        onProgress: suspend (DownloadEntity) -> Unit
    ): Result<Long> {
        val headerProfiles = buildHeaderProfiles(
            refererHeader = option.refererHeader,
            userAgentHeader = option.userAgentHeader,
            cookieHeader = option.cookieHeader,
            targetUrl = option.downloadUrl
        )
        var lastError: Throwable? = null

        for (headers in headerProfiles) {
            coroutineContext.ensureActive()
            var restartWithoutRange = false
            for (rangePass in 0 until 2) {
                val existingBytes = if (!restartWithoutRange && partFile.exists()) partFile.length() else 0L
                val reqBuilder = Request.Builder()
                    .url(option.downloadUrl)
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "identity")
                    .header("Connection", "keep-alive")
                headers.forEach { (k, v) ->
                    if (v.isNotBlank()) reqBuilder.header(k, v)
                }
                if (existingBytes > 0L) {
                    reqBuilder.header("Range", "bytes=$existingBytes-")
                }

                val attemptResult = runCatching {
                    downloadHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                        if (resp.code == 416 && existingBytes > 0L && !restartWithoutRange) {
                            deleteLocalFileIfExists(partFile.absolutePath)
                            restartWithoutRange = true
                            return@use -1L
                        }
                        if (!resp.isSuccessful) {
                            throw HttpRejectedException(resp.code, mapHttpStatusToReadableError(resp.code))
                        }

                        val contentType = resp.header("Content-Type").orEmpty().lowercase(Locale.US)
                        if (isHtmlOrJsonErrorContentType(contentType)) {
                            throw HttpRejectedException(
                                429,
                                "Host returned a web/error page instead of video stream."
                            )
                        }

                        val body = resp.body ?: throw IOException("Empty response body from CDN.")
                        val appendMode = resp.code == 206 && existingBytes > 0L
                        val startOffset = if (appendMode) existingBytes else 0L
                        if (!appendMode && partFile.exists()) {
                            deleteLocalFileIfExists(partFile.absolutePath)
                        }

                        val rangeTotal = parseTotalBytesFromContentRange(resp.header("Content-Range"))
                        val bodyLen = body.contentLength()
                        val resolvedTotal = when {
                            rangeTotal > 0L -> rangeTotal
                            bodyLen > 0L -> startOffset + bodyLen
                            option.sizeBytes > 0L -> option.sizeBytes
                            else -> 0L
                        }

                        var downloaded = startOffset
                        var lastUiUpdateMs = 0L
                        var lastUiBytes = downloaded

                        val startState = entity.copy(
                            status = DownloadStatus.DOWNLOADING.storageValue,
                            bytesDownloaded = downloaded,
                            totalBytes = resolvedTotal,
                            progressPercent = computeProgressPercent(downloaded, resolvedTotal),
                            errorReason = "",
                            updatedAt = System.currentTimeMillis()
                        )
                        onProgress(startState)

                        body.byteStream().use { input ->
                            FileOutputStream(partFile, appendMode).use { output ->
                                val buffer = ByteArray(64 * 1024)
                                while (true) {
                                    coroutineContext.ensureActive()
                                    val read = input.read(buffer)
                                    if (read == -1) break
                                    output.write(buffer, 0, read)
                                    downloaded += read

                                    val now = System.currentTimeMillis()
                                    if (now - lastUiUpdateMs >= 450L || downloaded - lastUiBytes >= 512L * 1024L) {
                                        lastUiUpdateMs = now
                                        lastUiBytes = downloaded
                                        onProgress(
                                            startState.copy(
                                                bytesDownloaded = downloaded,
                                                totalBytes = maxOf(resolvedTotal, downloaded),
                                                progressPercent = computeProgressPercent(downloaded, resolvedTotal),
                                                updatedAt = now
                                            )
                                        )
                                    }
                                }
                                output.flush()
                            }
                        }

                        maxOf(resolvedTotal, downloaded)
                    }
                }

                if (restartWithoutRange) continue

                if (attemptResult.isSuccess) {
                    return Result.success(attemptResult.getOrThrow())
                }

                val err = attemptResult.exceptionOrNull()!!
                if (err is CancellationException) throw err
                lastError = err

                // Only continue trying alternate header profiles if server rejected headers (400/401/403/429)
                val isHeaderRejection = err is HttpRejectedException &&
                    (err.code == 400 || err.code == 401 || err.code == 403 || err.code == 429)
                if (!isHeaderRejection) {
                    return Result.failure(err)
                }
                break
            }
        }

        return Result.failure(lastError ?: IOException("CDN rejected download request."))
    }

    private suspend fun resolveFreshOptionForDownload(
        media: MediaItem,
        season: Int,
        episode: Int,
        targetResolution: Int
    ): MovieBoxDownloadOption? {
        val subj = MovieBoxApiClient.resolveSubjectForMedia(media).getOrNull() ?: return null
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

    private fun downloadSubtitleFile(spec: ActiveDownloadSpec, season: Int, episode: Int) {
        val subtitle = spec.subtitle ?: return
        runCatching {
            val subExt = if (subtitle.url.contains(".vtt", ignoreCase = true)) "vtt" else "srt"
            val subFileName = buildSafeFileName(
                title = spec.media.title,
                mediaType = spec.media.mediaType,
                season = season,
                episode = episode,
                qualityLabel = subtitle.languageCode,
                extension = subExt
            )
            val subFile = File(resolveDownloadFolder(), subFileName)
            val req = Request.Builder()
                .url(subtitle.url)
                .header("Referer", spec.option.refererHeader)
                .header("User-Agent", spec.option.userAgentHeader)
                .apply {
                    if (spec.option.cookieHeader.isNotBlank()) {
                        header("Cookie", spec.option.cookieHeader)
                    }
                }
                .get()
                .build()
            downloadHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bytes = resp.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        subFile.writeBytes(bytes)
                    }
                }
            }
        }
    }

    private fun buildFallbackSpecFromEntity(item: DownloadEntity, wifiOnly: Boolean): ActiveDownloadSpec {
        val media = MediaItem(
            id = item.mediaId,
            tmdbId = item.tmdbId,
            title = item.title,
            overview = "",
            posterPath = item.posterPath,
            backdropPath = item.backdropPath,
            mediaType = item.mediaType
        )
        val resNum = Regex("(\\d{3,4})").find(item.qualityLabel)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 720
        val option = MovieBoxDownloadOption(
            id = item.id,
            resolution = resNum,
            resolutionLabel = item.qualityLabel,
            sizeBytes = item.totalBytes,
            downloadUrl = item.sourceUrl,
            format = item.format,
            refererHeader = MovieBoxDownloadOption.DEFAULT_REFERER,
            userAgentHeader = MovieBoxDownloadOption.DEFAULT_USER_AGENT
        )
        val subtitle = item.subtitleUrl.takeIf { it.isNotBlank() }?.let {
            MovieBoxSubtitleOption(
                id = "sub_${item.id}",
                languageCode = "en",
                languageName = item.subtitleLabel.ifBlank { "English" },
                url = it
            )
        }
        val relName = item.localFilePath.takeIf { it.isNotBlank() }?.let { File(it).name }
            ?: buildSafeFileName(
                title = item.title,
                mediaType = item.mediaType,
                season = item.season,
                episode = item.episode,
                qualityLabel = item.qualityLabel,
                extension = inferExtension(item.format, item.sourceUrl)
            )
        return ActiveDownloadSpec(
            media = media,
            option = option,
            subtitle = subtitle,
            wifiOnly = wifiOnly,
            relativeFileName = relName
        )
    }

    private fun resolveDownloadFolder(): File {
        val base = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: appContext.filesDir
        return File(base, "cinetra").apply { mkdirs() }
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

    private fun postCompletedNotification(download: DownloadEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (nm != null && nm.getNotificationChannel(DOWNLOAD_CHANNEL_ID) == null) {
                    nm.createNotificationChannel(
                        NotificationChannel(
                            DOWNLOAD_CHANNEL_ID,
                            "Offline Downloads",
                            NotificationManager.IMPORTANCE_DEFAULT
                        )
                    )
                }
            }
            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                appContext,
                download.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(appContext, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("${download.title} downloaded")
                .setContentText("${download.qualityLabel} • Ready for offline playback")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(appContext).notify(download.id.hashCode(), notification)
        }
    }

    private fun deleteLocalFileIfExists(path: String) {
        if (path.isBlank()) return
        runCatching {
            val f = File(path)
            if (f.exists()) f.delete()
        }
    }

    private class HttpRejectedException(val code: Int, message: String) : IOException(message)

    private data class ActiveDownloadSpec(
        val media: MediaItem,
        val option: MovieBoxDownloadOption,
        val subtitle: MovieBoxSubtitleOption?,
        val wifiOnly: Boolean,
        val relativeFileName: String
    )

    companion object {
        private const val IN_APP_ENGINE_ID = -2L
        private const val DOWNLOAD_CHANNEL_ID = "cinetra_offline_downloads"

        private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val activeJobs = ConcurrentHashMap<String, Job>()
        private val activeSpecs = ConcurrentHashMap<String, ActiveDownloadSpec>()
        private val autoResumedIds = ConcurrentHashMap.newKeySet<String>()

        private val downloadHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
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
                // 1. Primary: Referer + Origin + User-Agent + Cookie (required by MovieBox hakunaymatata CDN)
                add(buildMap {
                    if (ref.isNotBlank()) put("Referer", ref)
                    if (origin.isNotBlank()) put("Origin", origin)
                    put("User-Agent", ua)
                    if (cookie.isNotBlank()) put("Cookie", cookie)
                })
                // 2. Fallback: Referer + User-Agent + Cookie without Origin (for S3/CloudFront strict GETs)
                if (ref.isNotBlank()) {
                    add(buildMap {
                        put("Referer", ref)
                        put("User-Agent", ua)
                        if (cookie.isNotBlank()) put("Cookie", cookie)
                    })
                }
                // 3. Fallback: Self-host Referer + User-Agent + Cookie
                if (targetHostOrigin.isNotBlank() && targetHostOrigin != ref) {
                    add(buildMap {
                        put("Referer", targetHostOrigin)
                        put("User-Agent", ua)
                        if (cookie.isNotBlank()) put("Cookie", cookie)
                    })
                }
                // 4. Fallback: User-Agent + Cookie only
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
                in 500..599 -> "MovieBox CDN temporarily unavailable (HTTP $code)."
                else -> "Server rejected download request (HTTP $code)."
            }
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
