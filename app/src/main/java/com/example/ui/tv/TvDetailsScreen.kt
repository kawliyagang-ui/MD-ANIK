package com.example.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
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
import com.example.data.model.ContinueWatchingEntity
import com.example.data.model.ContinueWatchingPolicy
import com.example.data.model.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.EpisodeInfo
import com.example.data.model.MediaItem
import com.example.data.model.PlaybackSourcePolicy
import com.example.data.model.formatDownloadBytes
import com.example.data.model.matchesMedia
import com.example.ui.components.formatReleaseDate
import com.example.ui.components.rememberCineImageRequest
import com.example.ui.components.rememberDarkImagePainter
import com.example.ui.theme.CineTheme
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@Composable
fun TvDetailsScreen(
    media: MediaItem,
    isInWatchlist: Boolean,
    episodesBySeason: Map<Int, List<EpisodeInfo>>,
    isLoadingSeasons: Boolean,
    loadingSeasonNumber: Int?,
    continueWatchingItem: ContinueWatchingEntity?,
    similarMedia: List<MediaItem>,
    downloads: List<DownloadEntity>,
    onWatchClick: (Int, Int) -> Unit,
    onWatchlistToggle: () -> Unit,
    onTrailerClick: () -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onSimilarMediaClick: (MediaItem) -> Unit,
    onDownloadClick: (season: Int, episode: Int, episodeTitle: String) -> Unit,
    onDownloadSeason: (season: Int) -> Unit,
    onPauseDownload: (downloadId: String) -> Unit,
    onResumeDownload: (downloadId: String) -> Unit,
    onCancelDownload: (downloadId: String) -> Unit,
    onRetryDownload: (DownloadEntity) -> Unit,
    onPlayOffline: (DownloadEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    val darkPainter = rememberDarkImagePainter()

    val seasonList = remember(media.id, media.seasons, media.numberOfSeasons, media.episodesPerSeason) {
        media.resolvedSeasonList()
    }
    val initialSeason = remember(media.id, continueWatchingItem?.season) {
        if (media.mediaType == "tv" && continueWatchingItem != null) {
            continueWatchingItem.season.coerceAtLeast(0)
        } else {
            media.defaultSeasonNumber()
        }
    }
    var selectedSeason by remember(media.id) { mutableIntStateOf(initialSeason) }

    LaunchedEffect(seasonList) {
        if (seasonList.isNotEmpty() && seasonList.none { it.seasonNumber == selectedSeason }) {
            selectedSeason = seasonList.first().seasonNumber
        }
    }

    val mediaDownloads = remember(downloads, media.id, media.tmdbId, media.vidlyId, media.mediaType) {
        downloads.filter { it.matchesMedia(media) }
    }

    val loadedEpisodesForSeason = episodesBySeason[selectedSeason].orEmpty()
    val expectedEpisodeCount = maxOf(
        loadedEpisodesForSeason.size,
        media.episodeCountForSeason(selectedSeason)
    )
    val episodeEntries = remember(selectedSeason, loadedEpisodesForSeason, expectedEpisodeCount) {
        if (loadedEpisodesForSeason.isNotEmpty()) {
            loadedEpisodesForSeason
        } else {
            (1..expectedEpisodeCount).map { epNum ->
                EpisodeInfo(
                    episodeNumber = epNum,
                    seasonNumber = selectedSeason,
                    name = "Episode $epNum"
                )
            }
        }
    }

    val seasonEligibility = remember(media, selectedSeason, episodeEntries, mediaDownloads) {
        PlaybackSourcePolicy.computeSeasonEligibility(
            media = media,
            seasonNumber = selectedSeason,
            episodeNumbers = episodeEntries.map { it.episodeNumber },
            episodesInSeason = episodeEntries,
            downloads = mediaDownloads
        )
    }

    val targetWatchPair = remember(media.mediaType, selectedSeason, continueWatchingItem) {
        if (continueWatchingItem != null) {
            ContinueWatchingPolicy.normalizeSeasonEpisode(
                mediaType = media.mediaType,
                season = continueWatchingItem.season,
                episode = continueWatchingItem.episode
            )
        } else {
            selectedSeason to 1
        }
    }

    val activeTargetDownload = remember(mediaDownloads, media.mediaType, targetWatchPair) {
        val matching = if (media.mediaType == "tv") {
            mediaDownloads.filter {
                it.season == targetWatchPair.first && it.episode == targetWatchPair.second
            }
        } else {
            mediaDownloads
        }
        matching.firstOrNull {
            it.parsedStatus == DownloadStatus.COMPLETED &&
                it.localFilePath.isNotBlank() &&
                File(it.localFilePath).exists()
        } ?: matching.maxByOrNull { it.updatedAt }
    }

    val playFocusRequester = remember { FocusRequester() }
    LaunchedEffect(media.id) {
        delay(45L)
        runCatching { playFocusRequester.requestFocus() }
    }

    BackHandler(onBack = onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradients.appBackdrop)
            .testTag("tv_details_screen")
    ) {
        // Subtle ambient backdrop behind top half
        val backdropRequest = rememberCineImageRequest(
            url = media.fullBackdropUrl.ifBlank { media.fullPosterUrl },
            widthDp = 1280.dp,
            heightDp = 520.dp
        )
        AsyncImage(
            model = backdropRequest,
            contentDescription = null,
            placeholder = darkPainter,
            error = darkPainter,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .alpha(0.45f)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.background.copy(alpha = 0.35f),
                            colors.background.copy(alpha = 0.85f),
                            colors.background
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = TvOverscanHorizontal,
                end = TvOverscanHorizontal,
                top = TvOverscanVertical,
                bottom = TvOverscanVertical + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top Details Header
            item(key = "tv_details_header_${media.id}", contentType = "tv_details_header") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TvOutlinedActionButton(
                        text = "Back",
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBack,
                        modifier = Modifier.testTag("tv_details_back_button")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        val posterReq = rememberCineImageRequest(
                            url = media.fullPosterUrl,
                            widthDp = 180.dp,
                            heightDp = 270.dp
                        )
                        AsyncImage(
                            model = posterReq,
                            contentDescription = media.title,
                            placeholder = darkPainter,
                            error = darkPainter,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 180.dp, height = 270.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = media.title,
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 38.sp
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (media.voteAverage > 0.0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = colors.gold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.1f", media.voteAverage),
                                            color = colors.gold,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (media.releaseDate.isNotBlank()) {
                                    Text(
                                        text = formatReleaseDate(media.releaseDate),
                                        color = colors.textSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                                if (media.certification.isNotBlank()) {
                                    Text(
                                        text = media.certification,
                                        color = colors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .border(1.dp, colors.border, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                if (media.mediaType.equals("tv", ignoreCase = true)) {
                                    Text(
                                        text = "${media.numberOfSeasons} Seasons • ${media.totalEpisodeCount()} Episodes",
                                        color = colors.softRose,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (media.genres.isNotEmpty()) {
                                Text(
                                    text = media.genres.joinToString(" • "),
                                    color = colors.goldLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (media.overview.isNotBlank()) {
                                Text(
                                    text = media.overview,
                                    color = colors.textSecondary,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Action Row (Play, Download, My List, Trailer if available)
                            val watchLabel = when {
                                activeTargetDownload?.parsedStatus == DownloadStatus.COMPLETED && media.mediaType == "tv" ->
                                    "Play Offline • S${targetWatchPair.first}:E${targetWatchPair.second}"
                                activeTargetDownload?.parsedStatus == DownloadStatus.COMPLETED ->
                                    "Play Offline"
                                continueWatchingItem != null && media.mediaType == "tv" ->
                                    "Resume S${targetWatchPair.first}:E${targetWatchPair.second}"
                                continueWatchingItem != null -> "Resume"
                                else -> "Play"
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TvActionButton(
                                    text = watchLabel,
                                    icon = Icons.Default.PlayArrow,
                                    onClick = { onWatchClick(targetWatchPair.first, targetWatchPair.second) },
                                    modifier = Modifier
                                        .focusRequester(playFocusRequester)
                                        .testTag("tv_details_play_button")
                                )

                                // Download action button when not currently downloaded/active
                                val dlStatus = activeTargetDownload?.parsedStatus
                                val hasCompletedFile = dlStatus == DownloadStatus.COMPLETED &&
                                    !activeTargetDownload.localFilePath.isBlank() &&
                                    File(activeTargetDownload.localFilePath).exists()

                                if (hasCompletedFile && activeTargetDownload != null) {
                                    TvOutlinedActionButton(
                                        text = "Downloaded ✓",
                                        icon = Icons.Default.DownloadDone,
                                        highlighted = true,
                                        onClick = { onPlayOffline(activeTargetDownload) },
                                        modifier = Modifier.testTag("tv_details_downloaded_button")
                                    )
                                } else if (activeTargetDownload == null || (dlStatus != null && !dlStatus.isActive && dlStatus != DownloadStatus.FAILED)) {
                                    TvOutlinedActionButton(
                                        text = "Download",
                                        icon = Icons.Default.Download,
                                        onClick = {
                                            val epName = episodeEntries
                                                .firstOrNull { it.episodeNumber == targetWatchPair.second }
                                                ?.name
                                                .orEmpty()
                                            onDownloadClick(targetWatchPair.first, targetWatchPair.second, epName)
                                        },
                                        modifier = Modifier.testTag("tv_details_download_button")
                                    )
                                }

                                TvOutlinedActionButton(
                                    text = if (isInWatchlist) "In My List" else "My List",
                                    icon = if (isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    highlighted = isInWatchlist,
                                    onClick = onWatchlistToggle,
                                    modifier = Modifier.testTag("tv_details_watchlist_button")
                                )

                                if (!media.trailerYoutubeKey.isNullOrBlank()) {
                                    TvOutlinedActionButton(
                                        text = "Trailer",
                                        icon = Icons.Default.Videocam,
                                        onClick = onTrailerClick,
                                        modifier = Modifier.testTag("tv_details_trailer_button")
                                    )
                                }
                            }

                            // Active or Failed download bar with TV remote controls
                            val dlStatus = activeTargetDownload?.parsedStatus
                            if (activeTargetDownload != null && dlStatus != null && (dlStatus.isActive || dlStatus == DownloadStatus.FAILED)) {
                                TvLiveDownloadStrip(
                                    download = activeTargetDownload,
                                    onPause = { onPauseDownload(activeTargetDownload.id) },
                                    onResume = { onResumeDownload(activeTargetDownload.id) },
                                    onRetry = { onRetryDownload(activeTargetDownload) },
                                    onCancel = { onCancelDownload(activeTargetDownload.id) }
                                )
                            }
                        }
                    }
                }
            }

            // TV Series Seasons & Episodes Section
            if (media.mediaType == "tv") {
                item(key = "tv_details_seasons_bar_${media.id}", contentType = "tv_seasons_bar") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Episodes",
                                color = colors.textPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isLoadingSeasons || loadingSeasonNumber == selectedSeason) {
                                    CircularProgressIndicator(
                                        color = colors.softRose,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (seasonEligibility.eligibleEpisodes.isNotEmpty()) {
                                    TvOutlinedActionButton(
                                        text = "Download Season $selectedSeason",
                                        icon = Icons.Default.CloudDownload,
                                        onClick = { onDownloadSeason(selectedSeason) },
                                        modifier = Modifier.testTag("tv_details_download_season_$selectedSeason")
                                    )
                                }
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.testTag("tv_details_seasons_row")
                        ) {
                            items(
                                items = seasonList,
                                key = { "tv_season_${it.seasonNumber}" }
                            ) { seasonInfo ->
                                val sNum = seasonInfo.seasonNumber
                                val label = if (sNum == 0) {
                                    "Specials (${seasonInfo.episodeCount})"
                                } else {
                                    "Season $sNum (${seasonInfo.episodeCount})"
                                }
                                TvFilterPill(
                                    text = label,
                                    selected = sNum == selectedSeason,
                                    onClick = {
                                        selectedSeason = sNum
                                        onSeasonSelected(sNum)
                                    },
                                    modifier = Modifier.testTag("tv_details_season_chip_$sNum")
                                )
                            }
                        }
                    }
                }

                itemsIndexed(
                    items = episodeEntries,
                    key = { _, ep -> "tv_ep_${media.id}_${selectedSeason}_${ep.episodeNumber}" },
                    contentType = { _, _ -> "tv_episode_row" }
                ) { _, epInfo ->
                    val epNum = epInfo.episodeNumber
                    val epDownload = mediaDownloads
                        .filter { it.season == selectedSeason && it.episode == epNum }
                        .maxByOrNull { it.updatedAt }
                    val epStatus = epDownload?.parsedStatus
                    val isEpDownloaded = epStatus == DownloadStatus.COMPLETED &&
                        epDownload.localFilePath.isNotBlank() &&
                        File(epDownload.localFilePath).exists()
                    val resolvedLink = media.resolveDirectLinkForEpisode(
                        season = selectedSeason,
                        episode = epNum,
                        episodesInSeason = episodeEntries
                    )
                    val hasDirectLink = resolvedLink.hasAnyLink()

                    TvEpisodeRowCard(
                        seasonNumber = selectedSeason,
                        episode = epInfo,
                        download = epDownload,
                        isDownloaded = isEpDownloaded,
                        hasDirectLink = hasDirectLink,
                        onPlayClick = {
                            if (isEpDownloaded && epDownload != null) {
                                onPlayOffline(epDownload)
                            } else {
                                onWatchClick(selectedSeason, epNum)
                            }
                        },
                        onDownloadClick = { onDownloadClick(selectedSeason, epNum, epInfo.name) },
                        onPauseDownload = { epDownload?.let { onPauseDownload(it.id) } },
                        onResumeDownload = { epDownload?.let { onResumeDownload(it.id) } },
                        onRetryDownload = { epDownload?.let { onRetryDownload(it) } },
                        onCancelDownload = { epDownload?.let { onCancelDownload(it.id) } }
                    )
                }
            }

            // Similar Titles Shelf
            if (similarMedia.isNotEmpty()) {
                item(key = "tv_details_similar_${media.id}", contentType = "tv_similar_row") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (media.mediaType == "tv") "Similar TV Series" else "Similar Movies",
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            items(
                                items = similarMedia,
                                key = { "tv_sim_${it.id}" },
                                contentType = { "tv_poster_card" }
                            ) { simItem ->
                                TvPosterCard(
                                    media = simItem,
                                    onClick = { onSimilarMediaClick(simItem) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvLiveDownloadStrip(
    download: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = CineTheme.colors
    val status = download.parsedStatus
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val statusLabel = when (status) {
                DownloadStatus.RUNNING -> "Downloading • ${download.progressPercent}%"
                DownloadStatus.QUEUED -> "Queued"
                DownloadStatus.PAUSED -> "Paused • ${download.progressPercent}%"
                DownloadStatus.FAILED -> download.errorReason.ifBlank { "Download failed" }
                DownloadStatus.COMPLETED -> "Completed"
                DownloadStatus.CANCELLED -> "Cancelled"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = statusLabel,
                    color = if (status == DownloadStatus.FAILED) colors.softRose else colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (download.totalBytes > 0L) {
                    Text(
                        text = "${formatDownloadBytes(download.bytesDownloaded)} / ${formatDownloadBytes(download.totalBytes)}",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            LinearProgressIndicator(
                progress = { (download.progressPercent / 100f).coerceIn(0f, 1f) },
                color = if (status == DownloadStatus.FAILED) colors.softRose else colors.primary,
                trackColor = colors.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
            )
        }

        when (status) {
            DownloadStatus.RUNNING, DownloadStatus.QUEUED -> {
                TvOutlinedActionButton(text = "Pause", icon = Icons.Default.Pause, onClick = onPause)
            }
            DownloadStatus.PAUSED -> {
                TvOutlinedActionButton(text = "Resume", icon = Icons.Default.PlayArrow, onClick = onResume)
            }
            DownloadStatus.FAILED -> {
                TvOutlinedActionButton(text = "Retry", icon = Icons.Default.Refresh, onClick = onRetry)
            }
            else -> Unit
        }
        TvOutlinedActionButton(text = "Cancel", icon = Icons.Default.Close, onClick = onCancel)
    }
}

@Composable
private fun TvEpisodeRowCard(
    seasonNumber: Int,
    episode: EpisodeInfo,
    download: DownloadEntity?,
    isDownloaded: Boolean,
    hasDirectLink: Boolean,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onPauseDownload: () -> Unit,
    onResumeDownload: () -> Unit,
    onRetryDownload: () -> Unit,
    onCancelDownload: () -> Unit
) {
    val colors = CineTheme.colors
    val epStatus = download?.parsedStatus
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("tv_details_episode_${seasonNumber}_${episode.episodeNumber}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "E${episode.episodeNumber}",
                color = colors.gold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = episode.name.ifBlank { "Episode ${episode.episodeNumber}" },
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val meta = buildList {
                add("S$seasonNumber · E${episode.episodeNumber}")
                episode.runtimeMinutes?.let { add("${it}m") }
                if (episode.airDate.isNotBlank()) add(formatReleaseDate(episode.airDate))
                if (epStatus != null && epStatus.isActive) add("${epStatus.name} ${download.progressPercent}%")
                if (isDownloaded) add("Downloaded")
            }.joinToString(" • ")
            Text(
                text = meta,
                color = if (isDownloaded) colors.success else colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
            if (episode.overview.isNotBlank()) {
                Text(
                    text = episode.overview,
                    color = colors.textMuted,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TvActionButton(
                text = if (isDownloaded) "Play Offline" else "Play",
                icon = Icons.Default.PlayArrow,
                onClick = onPlayClick,
                modifier = Modifier.testTag("tv_ep_play_${seasonNumber}_${episode.episodeNumber}")
            )

            when {
                isDownloaded -> Unit
                epStatus == DownloadStatus.RUNNING || epStatus == DownloadStatus.QUEUED -> {
                    TvOutlinedActionButton(text = "Pause", icon = Icons.Default.Pause, onClick = onPauseDownload)
                    TvOutlinedActionButton(text = "Cancel", icon = Icons.Default.Close, onClick = onCancelDownload)
                }
                epStatus == DownloadStatus.PAUSED -> {
                    TvOutlinedActionButton(text = "Resume", icon = Icons.Default.PlayArrow, onClick = onResumeDownload)
                    TvOutlinedActionButton(text = "Cancel", icon = Icons.Default.Close, onClick = onCancelDownload)
                }
                epStatus == DownloadStatus.FAILED -> {
                    TvOutlinedActionButton(text = "Retry", icon = Icons.Default.Refresh, onClick = onRetryDownload)
                    TvOutlinedActionButton(text = "Cancel", icon = Icons.Default.Close, onClick = onCancelDownload)
                }
                hasDirectLink -> {
                    TvOutlinedActionButton(
                        text = "Download",
                        icon = Icons.Default.Download,
                        onClick = onDownloadClick,
                        modifier = Modifier.testTag("tv_ep_download_${seasonNumber}_${episode.episodeNumber}")
                    )
                }
                else -> {
                    TvOutlinedActionButton(
                        text = "Download",
                        icon = Icons.Default.Download,
                        onClick = onDownloadClick,
                        modifier = Modifier.testTag("tv_ep_download_${seasonNumber}_${episode.episodeNumber}")
                    )
                }
            }
        }
    }
}
