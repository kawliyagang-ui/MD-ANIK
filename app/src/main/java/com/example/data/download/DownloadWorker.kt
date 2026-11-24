package com.example.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaItem
import com.example.data.model.MovieBoxDownloadOption
import com.example.data.model.formatDownloadBytes
import com.example.data.remote.MovieBoxApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val mediaDao by lazy { AppDatabase.getDatabase(applicationContext).mediaDao() }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID)?.takeIf { it.isNotBlank() }
            ?: return@withContext Result.failure()

        ensureNotificationChannel()
        var currentEntity = mediaDao.getDownloadById(downloadId)
            ?: return@withContext Result.success()

        val initialStatus = currentEntity.parsedStatus
        if (initialStatus == DownloadStatus.COMPLETED ||
            initialStatus == DownloadStatus.PAUSED ||
            initialStatus == DownloadStatus.CANCELLED
        ) {
            DownloadEngine.reconcileQueue(applicationContext)
            return@withContext Result.success()
        }

        if (currentEntity.wifiOnly && !isWifiConnected()) {
            mediaDao.upsertDownload(
                currentEntity.copy(
                    status = DownloadStatus.PAUSED.storageValue,
                    errorReason = "Waiting for Wi-Fi connection...",
                    updatedAt = System.currentTimeMillis()
                )
            )
            DownloadEngine.reconcileQueue(applicationContext)
            return@withContext Result.success()
        }

        try {
            currentEntity = currentEntity.copy(
                status = DownloadStatus.RUNNING.storageValue,
                errorReason = "",
                updatedAt = System.currentTimeMillis()
            )
            mediaDao.upsertDownload(currentEntity)
            runCatching { setForeground(createForegroundInfo(currentEntity)) }

            val destFile = File(currentEntity.localFilePath).also { it.parentFile?.mkdirs() }
            if (destFile.exists() && destFile.length() > 0L && currentEntity.parsedStatus == DownloadStatus.COMPLETED) {
                return@withContext Result.success()
            }

            if (currentEntity.subtitleUrl.startsWith("http", ignoreCase = true)) {
                downloadSubtitleFile(currentEntity)
            }

            val partFile = File("${destFile.absolutePath}.part")
            var activeEntity = currentEntity
            var refreshedOnce = false
            var lastFailure: Throwable? = null

            for (attempt in 0 until 3) {
                coroutineContext.ensureActive()
                val latestDb = mediaDao.getDownloadById(downloadId)
                if (latestDb == null || latestDb.parsedStatus == DownloadStatus.CANCELLED) {
                    deleteFileQuietly(partFile)
                    return@withContext Result.success()
                }
                if (latestDb.parsedStatus == DownloadStatus.PAUSED || isStopped) {
                    preservePausedState(latestDb, partFile)
                    return@withContext Result.success()
                }

                val streamOutcome = streamWithHeaderFallbacks(
                    entity = activeEntity,
                    partFile = partFile,
                    onProgress = { updated ->
                        activeEntity = updated
                        mediaDao.upsertDownload(updated)
                        runCatching { setForeground(createForegroundInfo(updated)) }
                    },
                    shouldPauseOrCancel = {
                        if (isStopped) {
                            StopSignal.PAUSED
                        } else {
                            val row = mediaDao.getDownloadById(downloadId)
                            when (row?.parsedStatus) {
                                null, DownloadStatus.CANCELLED -> StopSignal.CANCELLED
                                DownloadStatus.PAUSED -> StopSignal.PAUSED
                                else -> StopSignal.NONE
                            }
                        }
                    }
                )

                when (val outcome = streamOutcome) {
                    is StreamOutcome.Completed -> {
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
                        val now = System.currentTimeMillis()
                        val completed = activeEntity.copy(
                            status = DownloadStatus.COMPLETED.storageValue,
                            bytesDownloaded = finalSize,
                            totalBytes = maxOf(finalSize, outcome.totalBytes),
                            progressPercent = 100,
                            localFilePath = destFile.absolutePath,
                            localUri = Uri.fromFile(destFile).toString(),
                            errorReason = "",
                            updatedAt = now,
                            completedAt = now
                        )
                        mediaDao.upsertDownload(completed)
                        postCompletedNotification(completed)
                        return@withContext Result.success()
                    }

                    is StreamOutcome.Paused -> {
                        val row = mediaDao.getDownloadById(downloadId) ?: activeEntity
                        preservePausedState(row, partFile)
                        return@withContext Result.success()
                    }

                    is StreamOutcome.Cancelled -> {
                        deleteFileQuietly(partFile)
                        deleteFileQuietly(destFile)
                        return@withContext Result.success()
                    }

                    is StreamOutcome.Failed -> {
                        val err = outcome.error
                        lastFailure = err
                        if (err is CancellationException) {
                            preservePausedState(activeEntity, partFile)
                            return@withContext Result.success()
                        }

                        val shouldRefreshLink = !refreshedOnce &&
                            DownloadEngine.isManagedApiProvider(activeEntity.providerName) &&
                            (err is HttpRejectedException && (err.code == 401 || err.code == 403 || err.code == 410 || err.code == 429))

                        if (shouldRefreshLink) {
                            refreshedOnce = true
                            activeEntity = activeEntity.copy(
                                status = DownloadStatus.RUNNING.storageValue,
                                errorReason = "Refreshing download link...",
                                updatedAt = System.currentTimeMillis()
                            )
                            mediaDao.upsertDownload(activeEntity)
                            val targetRes = Regex("(\\d{3,4})").find(activeEntity.qualityLabel)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 720
                            val freshOpt = resolveFreshMovieBoxOption(activeEntity, targetRes)
                            if (freshOpt != null && freshOpt.downloadUrl.isNotBlank()) {
                                activeEntity = activeEntity.copy(
                                    sourceUrl = freshOpt.downloadUrl,
                                    refererHeader = freshOpt.refererHeader,
                                    userAgentHeader = freshOpt.userAgentHeader,
                                    cookieHeader = freshOpt.cookieHeader
                                )
                                mediaDao.upsertDownload(activeEntity)
                                continue
                            }
                        }

                        if (attempt < 2) {
                            activeEntity = activeEntity.copy(
                                status = DownloadStatus.RUNNING.storageValue,
                                errorReason = "Retrying connection (${attempt + 2}/3)...",
                                updatedAt = System.currentTimeMillis()
                            )
                            mediaDao.upsertDownload(activeEntity)
                            delay(1200L * (attempt + 1))
                        }
                    }
                }
            }

            throw lastFailure ?: IOException("Unable to download video from host.")
        } catch (cancelled: CancellationException) {
            val row = runCatching { mediaDao.getDownloadById(downloadId) }.getOrNull()
            if (row != null && row.parsedStatus != DownloadStatus.CANCELLED && row.parsedStatus != DownloadStatus.COMPLETED) {
                val partFile = File("${row.localFilePath}.part")
                preservePausedState(row, partFile)
            }
            return@withContext Result.success()
        } catch (err: Throwable) {
            val failed = currentEntity.copy(
                status = DownloadStatus.FAILED.storageValue,
                errorReason = err.message?.takeIf { it.isNotBlank() } ?: "Download failed.",
                updatedAt = System.currentTimeMillis()
            )
            runCatching { mediaDao.upsertDownload(failed) }
            return@withContext Result.failure()
        } finally {
            runCatching { DownloadEngine.reconcileQueue(applicationContext) }
        }
    }

    private suspend fun preservePausedState(entity: DownloadEntity, partFile: File) {
        val currentBytes = if (partFile.exists()) partFile.length() else entity.bytesDownloaded
        val total = maxOf(entity.totalBytes, currentBytes)
        mediaDao.upsertDownload(
            entity.copy(
                status = DownloadStatus.PAUSED.storageValue,
                bytesDownloaded = currentBytes,
                totalBytes = total,
                progressPercent = DownloadEngine.computeProgressPercent(currentBytes, total),
                errorReason = if (entity.errorReason.contains("Wi-Fi", ignoreCase = true)) {
                    entity.errorReason
                } else {
                    "Paused"
                },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun streamWithHeaderFallbacks(
        entity: DownloadEntity,
        partFile: File,
        onProgress: suspend (DownloadEntity) -> Unit,
        shouldPauseOrCancel: suspend () -> StopSignal
    ): StreamOutcome {
        val referer = entity.refererHeader.ifBlank {
            if (DownloadEngine.isManagedApiProvider(entity.providerName)) MovieBoxDownloadOption.DEFAULT_REFERER else ""
        }
        val ua = entity.userAgentHeader.ifBlank { MovieBoxDownloadOption.DEFAULT_USER_AGENT }
        val headerProfiles = DownloadEngine.buildHeaderProfiles(
            refererHeader = referer,
            userAgentHeader = ua,
            cookieHeader = entity.cookieHeader,
            targetUrl = entity.sourceUrl
        )
        var lastError: Throwable? = null

        for (headers in headerProfiles) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            var restartWithoutRange = false
            for (rangePass in 0 until 2) {
                val signalBefore = shouldPauseOrCancel()
                if (signalBefore == StopSignal.PAUSED) return StreamOutcome.Paused
                if (signalBefore == StopSignal.CANCELLED) return StreamOutcome.Cancelled

                val existingBytes = if (!restartWithoutRange && partFile.exists()) partFile.length() else 0L
                val reqBuilder = Request.Builder()
                    .url(entity.sourceUrl)
                    .header("Accept", "*/*")
                    .header("Accept-Encoding", "identity")
                    .header("Connection", "keep-alive")
                headers.forEach { (k, v) ->
                    if (v.isNotBlank()) reqBuilder.header(k, v)
                }
                if (existingBytes > 0L) {
                    reqBuilder.header("Range", "bytes=$existingBytes-")
                }

                try {
                    workerHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                        if (resp.code == 416 && existingBytes > 0L && !restartWithoutRange) {
                            deleteFileQuietly(partFile)
                            restartWithoutRange = true
                            return@use
                        }
                        if (!resp.isSuccessful) {
                            throw HttpRejectedException(resp.code, DownloadEngine.mapHttpStatusToReadableError(resp.code))
                        }

                        val contentType = resp.header("Content-Type").orEmpty().lowercase(Locale.US)
                        if (DownloadEngine.isHtmlOrJsonErrorContentType(contentType)) {
                            throw HttpRejectedException(429, "Host returned a web/error page instead of video stream.")
                        }

                        val body = resp.body ?: throw IOException("Empty response body from host.")
                        val appendMode = resp.code == 206 && existingBytes > 0L
                        val startOffset = if (appendMode) existingBytes else 0L
                        if (!appendMode && partFile.exists()) {
                            deleteFileQuietly(partFile)
                        }

                        val rangeTotal = DownloadEngine.parseTotalBytesFromContentRange(resp.header("Content-Range"))
                        val bodyLen = body.contentLength()
                        val resolvedTotal = when {
                            rangeTotal > 0L -> rangeTotal
                            bodyLen > 0L -> startOffset + bodyLen
                            entity.totalBytes > 0L -> entity.totalBytes
                            else -> 0L
                        }

                        var downloaded = startOffset
                        var lastUiUpdateMs = 0L
                        var lastUiBytes = downloaded

                        val startState = entity.copy(
                            status = DownloadStatus.RUNNING.storageValue,
                            bytesDownloaded = downloaded,
                            totalBytes = maxOf(resolvedTotal, downloaded),
                            progressPercent = DownloadEngine.computeProgressPercent(downloaded, resolvedTotal),
                            errorReason = "",
                            updatedAt = System.currentTimeMillis()
                        )
                        onProgress(startState)

                        body.byteStream().use { input ->
                            FileOutputStream(partFile, appendMode).use { output ->
                                val buffer = ByteArray(64 * 1024)
                                while (true) {
                                    when (shouldPauseOrCancel()) {
                                        StopSignal.PAUSED -> {
                                            output.flush()
                                            return StreamOutcome.Paused
                                        }
                                        StopSignal.CANCELLED -> {
                                            return StreamOutcome.Cancelled
                                        }
                                        StopSignal.NONE -> Unit
                                    }

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
                                                progressPercent = DownloadEngine.computeProgressPercent(downloaded, resolvedTotal),
                                                updatedAt = now
                                            )
                                        )
                                    }
                                }
                                output.flush()
                            }
                        }

                        return StreamOutcome.Completed(maxOf(resolvedTotal, downloaded))
                    }
                } catch (err: Throwable) {
                    if (err is CancellationException) return StreamOutcome.Paused
                    lastError = err
                    val isHeaderRejection = err is HttpRejectedException &&
                        (err.code == 400 || err.code == 401 || err.code == 403 || err.code == 429)
                    if (!isHeaderRejection) {
                        return StreamOutcome.Failed(err)
                    }
                }

                if (!restartWithoutRange) break
            }
        }

        return StreamOutcome.Failed(lastError ?: IOException("CDN rejected download request."))
    }

    private suspend fun resolveFreshMovieBoxOption(
        entity: DownloadEntity,
        targetResolution: Int
    ): MovieBoxDownloadOption? {
        val media = MediaItem(
            id = entity.mediaId,
            tmdbId = entity.tmdbId,
            title = entity.title,
            overview = "",
            posterPath = entity.posterPath,
            backdropPath = entity.backdropPath,
            mediaType = entity.mediaType
        )
        val subj = MovieBoxApiClient.resolveSubjectForMedia(media).getOrNull() ?: return null
        val bundle = MovieBoxApiClient.fetchDownloadBundle(
            subject = subj,
            season = entity.season,
            episode = entity.episode,
            isTv = media.mediaType.equals("tv", ignoreCase = true) || subj.subjectType == 2
        ).getOrNull() ?: return null
        val opts = bundle.options
        if (opts.isEmpty()) return null
        return opts.firstOrNull { it.resolution == targetResolution }
            ?: opts.minByOrNull { kotlin.math.abs(it.resolution - targetResolution) }
            ?: opts.firstOrNull()
    }

    private fun downloadSubtitleFile(entity: DownloadEntity) {
        runCatching {
            val destFile = File(entity.localFilePath)
            val parent = destFile.parentFile ?: return
            val subExt = if (entity.subtitleUrl.contains(".vtt", ignoreCase = true)) "vtt" else "srt"
            val subFile = File(parent, "${destFile.nameWithoutExtension}.$subExt")
            val req = Request.Builder()
                .url(entity.subtitleUrl)
                .apply {
                    if (entity.refererHeader.isNotBlank()) header("Referer", entity.refererHeader)
                    if (entity.userAgentHeader.isNotBlank()) header("User-Agent", entity.userAgentHeader)
                    if (entity.cookieHeader.isNotBlank()) header("Cookie", entity.cookieHeader)
                }
                .get()
                .build()
            workerHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bytes = resp.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        subFile.writeBytes(bytes)
                    }
                }
            }
        }
    }

    private fun createForegroundInfo(entity: DownloadEntity): ForegroundInfo {
        val notificationId = entity.id.hashCode()
        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(applicationContext, DownloadActionReceiver::class.java).apply {
            action = DownloadActionReceiver.ACTION_PAUSE_DOWNLOAD
            putExtra(DownloadActionReceiver.EXTRA_DOWNLOAD_ID, entity.id)
        }
        val pausePi = PendingIntent.getBroadcast(
            applicationContext,
            notificationId + 1,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(applicationContext, DownloadActionReceiver::class.java).apply {
            action = DownloadActionReceiver.ACTION_CANCEL_DOWNLOAD
            putExtra(DownloadActionReceiver.EXTRA_DOWNLOAD_ID, entity.id)
        }
        val cancelPi = PendingIntent.getBroadcast(
            applicationContext,
            notificationId + 2,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = if (entity.mediaType.equals("tv", ignoreCase = true)) {
            "${entity.title} • S${entity.season}:E${entity.episode}"
        } else {
            entity.title
        }
        val sizeSummary = when {
            entity.totalBytes > 0L -> "${entity.progressPercent}% • ${formatDownloadBytes(entity.bytesDownloaded)} / ${formatDownloadBytes(entity.totalBytes)}"
            entity.bytesDownloaded > 0L -> formatDownloadBytes(entity.bytesDownloaded)
            else -> "Preparing download..."
        }

        val notification = NotificationCompat.Builder(applicationContext, DownloadEngine.DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Downloading $titleText")
            .setContentText(sizeSummary)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, entity.progressPercent.coerceIn(0, 100), entity.totalBytes <= 0L)
            .setContentIntent(contentPi)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pausePi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPi)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun postCompletedNotification(download: DownloadEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        runCatching {
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                applicationContext,
                download.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val titleText = if (download.mediaType.equals("tv", ignoreCase = true)) {
                "${download.title} S${download.season}:E${download.episode}"
            } else {
                download.title
            }
            val notification = NotificationCompat.Builder(applicationContext, DownloadEngine.DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("$titleText downloaded")
                .setContentText("${download.qualityLabel} • Ready for offline playback")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(applicationContext).notify(download.id.hashCode(), notification)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (nm != null && nm.getNotificationChannel(DownloadEngine.DOWNLOAD_CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        DownloadEngine.DOWNLOAD_CHANNEL_ID,
                        "Offline Downloads",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }

    private fun isWifiConnected(): Boolean {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    private fun deleteFileQuietly(file: File) {
        runCatching {
            if (file.exists()) file.delete()
        }
    }

    private enum class StopSignal { NONE, PAUSED, CANCELLED }

    private sealed interface StreamOutcome {
        data class Completed(val totalBytes: Long) : StreamOutcome
        data object Paused : StreamOutcome
        data object Cancelled : StreamOutcome
        data class Failed(val error: Throwable) : StreamOutcome
    }

    private class HttpRejectedException(val code: Int, message: String) : IOException(message)

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"

        private val workerHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        }
    }
}
