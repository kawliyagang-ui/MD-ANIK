package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.DownloadEntity
import com.example.data.model.MediaItem

@Composable
fun OfflineVideoPlayerScreen(
    download: DownloadEntity,
    initialPositionMs: Long = 0L,
    onSavePosition: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val media = MediaItem(
        id = download.mediaId,
        tmdbId = download.tmdbId,
        title = download.title,
        overview = "",
        posterPath = download.posterPath,
        backdropPath = download.backdropPath,
        mediaType = download.mediaType
    )
    DirectMediaPlayerScreen(
        media = media,
        season = download.season,
        episode = download.episode,
        sourceUrl = download.localFilePath.ifBlank { download.localUri.ifBlank { download.sourceUrl } },
        isLocalFile = download.localFilePath.isNotBlank() || download.localUri.isNotBlank(),
        downloadEntity = download,
        initialPositionMs = initialPositionMs,
        onSavePosition = onSavePosition,
        onBack = onBack,
        modifier = modifier
    )
}
