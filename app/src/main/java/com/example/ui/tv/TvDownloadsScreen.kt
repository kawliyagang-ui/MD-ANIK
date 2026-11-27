package com.example.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.data.model.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.PlaybackSourcePolicy
import com.example.data.model.formatDownloadBytes
import com.example.ui.components.rememberCineImageRequest
import com.example.ui.components.rememberDarkImagePainter
import com.example.ui.theme.CineTheme
import kotlinx.coroutines.delay

@Composable
fun TvDownloadsScreen(
    downloads: List<DownloadEntity>,
    onPlayOffline: (DownloadEntity) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onRetryDownload: (DownloadEntity) -> Unit,
    onCancelOrDelete: (String) -> Unit,
    onRefreshStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredDownloads = remember(downloads, selectedFilter) {
        when (selectedFilter) {
            "Active" -> downloads.filter { it.parsedStatus.isActive }
            "Completed" -> downloads.filter { it.parsedStatus == DownloadStatus.COMPLETED }
            "Failed" -> downloads.filter { it.parsedStatus == DownloadStatus.FAILED }
            else -> downloads
        }
    }
    val grouping = remember(filteredDownloads) {
        PlaybackSourcePolicy.groupDownloadsForDisplay(filteredDownloads)
    }

    val completedCount = remember(downloads) { downloads.count { it.parsedStatus == DownloadStatus.COMPLETED } }
    val activeCount = remember(downloads) { downloads.count { it.parsedStatus.isActive } }
    val totalBytes = remember(downloads) {
        downloads.filter { it.parsedStatus == DownloadStatus.COMPLETED }
            .sumOf { maxOf(it.bytesDownloaded, it.totalBytes) }
    }

    val firstFilterRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(50L)
        runCatching { firstFilterRequester.requestFocus() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradients.appBackdrop)
            .padding(horizontal = TvOverscanHorizontal, vertical = 12.dp)
            .testTag("tv_downloads_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header + Filter Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Downloads",
                    color = colors.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
                val summary = buildString {
                    append("$completedCount completed")
                    if (activeCount > 0) append(" • $activeCount active")
                    if (totalBytes > 0L) append(" • ${formatDownloadBytes(totalBytes)}")
                }
                Text(
                    text = summary,
                    color = colors.gold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            TvOutlinedActionButton(
                text = "Sync Queue",
                icon = Icons.Default.Refresh,
                onClick = onRefreshStatus,
                modifier = Modifier.testTag("tv_downloads_refresh_button")
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val filters = listOf("All", "Active", "Completed", "Failed")
            items(filters.size) { index ->
                val tab = filters[index]
                TvFilterPill(
                    text = when (tab) {
                        "Active" -> "Active ($activeCount)"
                        "Completed" -> "Completed ($completedCount)"
                        else -> tab
                    },
                    selected = selectedFilter == tab,
                    onClick = { selectedFilter = tab },
                    modifier = if (index == 0) {
                        Modifier
                            .focusRequester(firstFilterRequester)
                            .testTag("tv_downloads_filter_$tab")
                    } else {
                        Modifier.testTag("tv_downloads_filter_$tab")
                    }
                )
            }
        }

        if (filteredDownloads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                        .padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = colors.gold,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (selectedFilter == "All") "No Downloads Yet" else "No $selectedFilter Downloads",
                        color = colors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Open any movie or TV series and select Download to save titles for offline playback.",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = TvOverscanVertical + 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Grouped Series -> Seasons -> Episodes
                grouping.seriesGroups.forEach { seriesGroup ->
                    item(key = "tv_dl_series_header_${seriesGroup.mediaId}") {
                        Text(
                            text = seriesGroup.seriesTitle,
                            color = colors.textPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    seriesGroup.seasons.forEach { seasonGroup ->
                        item(key = "tv_dl_season_${seriesGroup.mediaId}_${seasonGroup.seasonNumber}") {
                            Text(
                                text = if (seasonGroup.seasonNumber == 0) "Specials" else "Season ${seasonGroup.seasonNumber}",
                                color = colors.softRose,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        items(
                            items = seasonGroup.episodes,
                            key = { "tv_dl_ep_${it.id}" }
                        ) { download ->
                            TvDownloadItemRow(
                                download = download,
                                titleLabel = "E${download.episode} • ${download.episodeTitle.ifBlank { "Episode ${download.episode}" }}",
                                onPlayOffline = { onPlayOffline(download) },
                                onPause = { onPauseDownload(download.id) },
                                onResume = { onResumeDownload(download.id) },
                                onRetry = { onRetryDownload(download) },
                                onDelete = { onCancelOrDelete(download.id) }
                            )
                        }
                    }
                }

                // Movie Downloads
                if (grouping.movieDownloads.isNotEmpty() && grouping.seriesGroups.isNotEmpty()) {
                    item(key = "tv_dl_movies_header") {
                        Text(
                            text = "Movies",
                            color = colors.textPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
                items(
                    items = grouping.movieDownloads,
                    key = { "tv_dl_movie_${it.id}" }
                ) { download ->
                    TvDownloadItemRow(
                        download = download,
                        titleLabel = download.title,
                        onPlayOffline = { onPlayOffline(download) },
                        onPause = { onPauseDownload(download.id) },
                        onResume = { onResumeDownload(download.id) },
                        onRetry = { onRetryDownload(download) },
                        onDelete = { onCancelOrDelete(download.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvDownloadItemRow(
    download: DownloadEntity,
    titleLabel: String,
    onPlayOffline: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = CineTheme.colors
    val darkPainter = rememberDarkImagePainter()
    val status = download.parsedStatus
    val posterUrl = remember(download.posterPath) {
        when {
            download.posterPath.isBlank() -> ""
            download.posterPath.startsWith("http") -> download.posterPath
            else -> "https://image.tmdb.org/t/p/w342${download.posterPath}"
        }
    }
    val thumbReq = rememberCineImageRequest(url = posterUrl, widthDp = 72.dp, heightDp = 104.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("tv_download_item_${download.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = thumbReq,
            contentDescription = titleLabel,
            placeholder = darkPainter,
            error = darkPainter,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 68.dp, height = 98.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = titleLabel,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val metaText = buildList {
                if (download.qualityLabel.isNotBlank()) add(download.qualityLabel)
                add(
                    when (status) {
                        DownloadStatus.COMPLETED -> "Ready Offline"
                        DownloadStatus.RUNNING -> "Downloading ${download.progressPercent}%"
                        DownloadStatus.QUEUED -> "Queued"
                        DownloadStatus.PAUSED -> "Paused ${download.progressPercent}%"
                        DownloadStatus.FAILED -> download.errorReason.ifBlank { "Failed" }
                        DownloadStatus.CANCELLED -> "Cancelled"
                    }
                )
                if (download.totalBytes > 0L) {
                    add("${formatDownloadBytes(download.bytesDownloaded)} / ${formatDownloadBytes(download.totalBytes)}")
                } else if (download.bytesDownloaded > 0L) {
                    add(formatDownloadBytes(download.bytesDownloaded))
                }
            }.joinToString(" • ")

            Text(
                text = metaText,
                color = when (status) {
                    DownloadStatus.COMPLETED -> colors.success
                    DownloadStatus.FAILED -> colors.softRose
                    else -> colors.textSecondary
                },
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (status.isActive || status == DownloadStatus.FAILED) {
                LinearProgressIndicator(
                    progress = { (download.progressPercent / 100f).coerceIn(0f, 1f) },
                    color = if (status == DownloadStatus.FAILED) colors.softRose else colors.primary,
                    trackColor = colors.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (status) {
                DownloadStatus.COMPLETED -> {
                    TvActionButton(
                        text = "Play",
                        icon = Icons.Default.PlayArrow,
                        onClick = onPlayOffline,
                        modifier = Modifier.testTag("tv_download_play_${download.id}")
                    )
                }
                DownloadStatus.RUNNING, DownloadStatus.QUEUED -> {
                    TvOutlinedActionButton(
                        text = "Pause",
                        icon = Icons.Default.Pause,
                        onClick = onPause,
                        modifier = Modifier.testTag("tv_download_pause_${download.id}")
                    )
                }
                DownloadStatus.PAUSED -> {
                    TvActionButton(
                        text = "Resume",
                        icon = Icons.Default.PlayArrow,
                        onClick = onResume,
                        modifier = Modifier.testTag("tv_download_resume_${download.id}")
                    )
                }
                DownloadStatus.FAILED -> {
                    TvActionButton(
                        text = "Retry",
                        icon = Icons.Default.Refresh,
                        onClick = onRetry,
                        modifier = Modifier.testTag("tv_download_retry_${download.id}")
                    )
                }
                DownloadStatus.CANCELLED -> Unit
            }

            TvOutlinedActionButton(
                text = "Delete",
                icon = Icons.Default.Delete,
                onClick = onDelete,
                modifier = Modifier.testTag("tv_download_delete_${download.id}")
            )
        }
    }
}
