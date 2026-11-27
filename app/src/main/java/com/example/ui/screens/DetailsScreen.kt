package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContinueWatchingEntity
import com.example.data.model.ContinueWatchingPolicy
import com.example.data.model.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.EpisodeInfo
import com.example.data.model.MediaItem
import com.example.data.model.PlaybackSourcePolicy
import com.example.data.model.formatDownloadBytes
import com.example.data.model.matchesMedia
import com.example.ui.components.ActionCard
import com.example.ui.components.CategoryPill
import com.example.ui.components.CineEase
import com.example.ui.components.DetailedMovieView
import com.example.ui.components.DetailsEpisodesSkeleton
import com.example.ui.components.PosterCard
import com.example.ui.components.RedButton
import com.example.ui.components.RedPlayButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StylishProgressBar
import com.example.ui.components.formatReleaseDate
import com.example.ui.theme.CinetraTheme
import java.io.File

@Composable
fun DetailsScreen(
    media: MediaItem,
    isInWatchlist: Boolean,
    onWatchClick: (Int, Int) -> Unit, // season, episode
    onWatchlistToggle: () -> Unit,
    onTrailerClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    episodesBySeason: Map<Int, List<EpisodeInfo>> = emptyMap(),
    isLoadingSeasons: Boolean = false,
    loadingSeasonNumber: Int? = null,
    onSeasonSelected: (Int) -> Unit = {},
    continueWatchingItem: ContinueWatchingEntity? = null,
    similarMedia: List<MediaItem> = emptyList(),
    onSimilarMediaClick: (MediaItem) -> Unit = {},
    onReportProblemClick: () -> Unit = {},
    downloads: List<DownloadEntity> = emptyList(),
    onDownloadClick: (season: Int, episode: Int, episodeTitle: String) -> Unit = { _, _, _ -> },
    onDownloadSeason: (season: Int) -> Unit = {},
    onDownloadSelectedEpisodes: (season: Int, selectedEpisodes: Set<Int>) -> Unit = { _, _ -> },
    onPauseDownload: (downloadId: String) -> Unit = {},
    onResumeDownload: (downloadId: String) -> Unit = {},
    onCancelDownload: (downloadId: String) -> Unit = {},
    onRetryDownload: (DownloadEntity) -> Unit = {},
    onPlayOffline: (DownloadEntity) -> Unit = {}
) {
    val colors = CinetraTheme.colors
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
    var selectedEpisodesBySeason by remember(media.id) { mutableStateOf<Map<Int, Set<Int>>>(emptyMap()) }

    val mediaDownloads = remember(downloads, media.id, media.tmdbId, media.vidlyId, media.mediaType) {
        downloads.filter { it.matchesMedia(media) }
    }

    LaunchedEffect(seasonList) {
        if (seasonList.isNotEmpty() && seasonList.none { it.seasonNumber == selectedSeason }) {
            selectedSeason = seasonList.first().seasonNumber
        }
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

    val seasonEligibility = remember(
        media,
        selectedSeason,
        episodeEntries,
        mediaDownloads
    ) {
        PlaybackSourcePolicy.computeSeasonEligibility(
            media = media,
            seasonNumber = selectedSeason,
            episodeNumbers = episodeEntries.map { it.episodeNumber },
            episodesInSeason = episodeEntries,
            downloads = mediaDownloads
        )
    }

    val currentSeasonSelection = remember(selectedEpisodesBySeason, selectedSeason, seasonEligibility) {
        selectedEpisodesBySeason[selectedSeason].orEmpty()
            .intersect(seasonEligibility.eligibleEpisodes.toSet())
    }

    BackHandler {
        onBack()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item(
            key = "details_header_${media.id}",
            contentType = "details_header"
        ) {
            DetailedMovieView(
                media = media,
                backdropHeight = 236.dp,
                showBackButton = true,
                onDismissOrBack = onBack,
                actionsContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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

                        val targetStatus = activeTargetDownload?.parsedStatus
                        val hasCompletedTargetFile = targetStatus == DownloadStatus.COMPLETED &&
                            !activeTargetDownload.localFilePath.isBlank() &&
                            File(activeTargetDownload.localFilePath).exists()
                        val showLiveDownloadCard = hasCompletedTargetFile ||
                            (activeTargetDownload != null && targetStatus != null && (targetStatus.isActive || targetStatus == DownloadStatus.FAILED))

                        val watchCtaLabel = when {
                            hasCompletedTargetFile && media.mediaType == "tv" ->
                                "Play Offline • S${targetWatchPair.first}:E${targetWatchPair.second}"
                            hasCompletedTargetFile ->
                                "Play Offline"
                            continueWatchingItem != null && media.mediaType == "tv" ->
                                "Watch Now • Resume S${targetWatchPair.first}:E${targetWatchPair.second}"
                            continueWatchingItem != null ->
                                "Watch Now • Resume"
                            else -> "Watch Now"
                        }

                        // Full-width solid #E50914 Play button (14dp corners)
                        RedPlayButton(
                            label = watchCtaLabel,
                            onClick = { onWatchClick(targetWatchPair.first, targetWatchPair.second) },
                            modifier = Modifier.testTag("details_watch_now_button")
                        )

                        // 2-Column ActionCard Grid (#1E1E24, 16dp radius)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ActionCard(
                                icon = if (isInWatchlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                label = if (isInWatchlist) "Favorited" else "Favorite",
                                isActive = isInWatchlist,
                                onClick = onWatchlistToggle,
                                contentDescription = if (isInWatchlist) "Remove from Favorites" else "Add to Favorites",
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("details_watchlist_toggle_button")
                            )

                            ActionCard(
                                icon = Icons.Default.Videocam,
                                label = "Trailer",
                                onClick = onTrailerClick,
                                contentDescription = "Watch Trailer",
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("details_trailer_button")
                            )
                        }

                        if (!showLiveDownloadCard) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ActionCard(
                                    icon = Icons.Default.CloudDownload,
                                    label = if (media.mediaType == "tv") {
                                        "Download S${targetWatchPair.first}:E${targetWatchPair.second}"
                                    } else {
                                        "Download Offline"
                                    },
                                    onClick = {
                                        val epName = episodeEntries
                                            .firstOrNull { it.episodeNumber == targetWatchPair.second }
                                            ?.name
                                            .orEmpty()
                                        onDownloadClick(targetWatchPair.first, targetWatchPair.second, epName)
                                    },
                                    contentDescription = "Download Offline",
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("details_download_moviebox_button")
                                )

                                ActionCard(
                                    icon = Icons.Default.BugReport,
                                    label = "Report Issue",
                                    iconTint = colors.textSecondary,
                                    onClick = onReportProblemClick,
                                    contentDescription = "Report a Problem",
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("details_report_problem_button")
                                )
                            }
                        } else {
                            LiveDownloadControlCard(
                                media = media,
                                season = targetWatchPair.first,
                                episode = targetWatchPair.second,
                                download = activeTargetDownload,
                                onStartDownload = {
                                    val epName = episodeEntries
                                        .firstOrNull { it.episodeNumber == targetWatchPair.second }
                                        ?.name
                                        .orEmpty()
                                    onDownloadClick(targetWatchPair.first, targetWatchPair.second, epName)
                                },
                                onPause = { activeTargetDownload?.let { onPauseDownload(it.id) } },
                                onResume = { activeTargetDownload?.let { onResumeDownload(it.id) } },
                                onCancel = { activeTargetDownload?.let { onCancelDownload(it.id) } },
                                onRetry = { activeTargetDownload?.let { onRetryDownload(it) } },
                                onPlayOffline = { activeTargetDownload?.let { onPlayOffline(it) } }
                            )

                            ActionCard(
                                icon = Icons.Default.BugReport,
                                label = "Report Video / Info Issue",
                                iconTint = colors.textSecondary,
                                onClick = onReportProblemClick,
                                contentDescription = "Report a Problem",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("details_report_problem_button")
                            )
                        }

                        if (media.mediaType == "tv" && (media.latestEpisodeLabel.isNotBlank() || isInWatchlist)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(colors.surface)
                                    .border(1.dp, colors.border, MaterialTheme.shapes.medium)
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val infoText = buildString {
                                    if (media.latestEpisodeLabel.isNotBlank()) {
                                        append(media.latestEpisodeLabel)
                                    }
                                    if (isInWatchlist) {
                                        if (isNotEmpty()) append(" • ")
                                        append("Following for new episode alerts")
                                    }
                                }
                                Text(
                                    text = infoText,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            )
        }

        // TV Series / Anime Episodes & Seasons Section
        if (media.mediaType == "tv") {
            item(
                key = "tv_seasons_header_${media.id}",
                contentType = "tv_seasons_header"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Seasons & Episodes",
                                style = MaterialTheme.typography.titleMedium, // Bold 18.sp
                                color = colors.textPrimary
                            )
                            val seasonLabel = if (media.numberOfSeasons == 1) "1 Season" else "${media.numberOfSeasons} Seasons"
                            val totalEpCount = media.totalEpisodeCount()
                            Text(
                                text = "$seasonLabel • $totalEpCount Episodes",
                                color = colors.textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("details_seasons_summary")
                            )
                        }

                        if (isLoadingSeasons || loadingSeasonNumber == selectedSeason) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.border, RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = colors.primary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Syncing TMDB",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Seasons CategoryPills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("details_seasons_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        seasonList.forEach { seasonInfo ->
                            val sNum = seasonInfo.seasonNumber
                            val isSel = sNum == selectedSeason
                            val chipLabel = if (sNum == 0) {
                                "Specials (${seasonInfo.episodeCount})"
                            } else {
                                "Season $sNum (${seasonInfo.episodeCount})"
                            }
                            CategoryPill(
                                label = chipLabel,
                                selected = isSel,
                                onClick = {
                                    selectedSeason = sNum
                                    onSeasonSelected(sNum)
                                },
                                modifier = Modifier.testTag("details_season_chip_$sNum")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Season Batch Download Controls
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = MaterialTheme.shapes.medium, // 14.dp
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.border, MaterialTheme.shapes.medium)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(tween(durationMillis = 220, easing = CineEase))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = seasonEligibility.eligibleEpisodes.isNotEmpty()) {
                                            val allSelected = seasonEligibility.isAllEligibleSelected(currentSeasonSelection)
                                            val nextSet = if (allSelected) {
                                                emptySet()
                                            } else {
                                                seasonEligibility.eligibleEpisodes.toSet()
                                            }
                                            selectedEpisodesBySeason = selectedEpisodesBySeason + (selectedSeason to nextSet)
                                        }
                                        .testTag("details_select_all_season_$selectedSeason")
                                ) {
                                    Checkbox(
                                        checked = seasonEligibility.isAllEligibleSelected(currentSeasonSelection),
                                        enabled = seasonEligibility.eligibleEpisodes.isNotEmpty(),
                                        onCheckedChange = { checked ->
                                            val nextSet = if (checked) {
                                                seasonEligibility.eligibleEpisodes.toSet()
                                            } else {
                                                emptySet()
                                            }
                                            selectedEpisodesBySeason = selectedEpisodesBySeason + (selectedSeason to nextSet)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = colors.primary,
                                            uncheckedColor = colors.textSecondary
                                        )
                                    )
                                    Column {
                                        Text(
                                            text = "Select all",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (seasonEligibility.eligibleEpisodes.isNotEmpty()) {
                                                colors.textPrimary
                                            } else {
                                                colors.textMuted
                                            }
                                        )
                                        Text(
                                            text = "${seasonEligibility.eligibleEpisodes.size} downloadable • ${seasonEligibility.downloadedEpisodes.size} downloaded",
                                            fontSize = 11.sp,
                                            color = colors.textMuted
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onDownloadSeason(selectedSeason) },
                                    enabled = seasonEligibility.eligibleEpisodes.isNotEmpty(),
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = colors.surfaceElevated,
                                        contentColor = colors.textPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("details_download_season_$selectedSeason")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = "Download Season $selectedSeason",
                                        tint = if (seasonEligibility.eligibleEpisodes.isNotEmpty()) colors.textPrimary else colors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Download Season",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (seasonEligibility.eligibleEpisodes.isNotEmpty()) colors.textPrimary else colors.textMuted
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = currentSeasonSelection.isNotEmpty(),
                                enter = fadeIn(tween(180, easing = CineEase)) + expandVertically(tween(220, easing = CineEase)),
                                exit = fadeOut(tween(140, easing = CineEase)) + shrinkVertically(tween(180, easing = CineEase))
                            ) {
                                RedButton(
                                    onClick = {
                                        val picked = currentSeasonSelection
                                        onDownloadSelectedEpisodes(selectedSeason, picked)
                                        selectedEpisodesBySeason = selectedEpisodesBySeason + (selectedSeason to emptySet())
                                    },
                                    fillWidth = true,
                                    minHeight = 48.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("details_download_selected_$selectedSeason")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = colors.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Download selected (${currentSeasonSelection.size})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (loadedEpisodesForSeason.isEmpty() && (isLoadingSeasons || loadingSeasonNumber == selectedSeason)) {
                item(
                    key = "details_ep_skeleton_${media.id}_$selectedSeason",
                    contentType = "episodes_skeleton"
                ) {
                    DetailsEpisodesSkeleton()
                }
            } else {
                items(
                    items = episodeEntries,
                    key = { epInfo -> "ep_${media.id}_${selectedSeason}_${epInfo.episodeNumber}" },
                    contentType = { "episode_row" }
                ) { epInfo ->
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
                    val isEpEligible = hasDirectLink && !isEpDownloaded && (epStatus == null || !epStatus.isActive)
                    val isEpChecked = epNum in currentSeasonSelection

                    val metaParts = buildList {
                        epInfo.runtimeMinutes?.let { add("${it}m") }
                        if (epInfo.airDate.isNotBlank()) add(formatReleaseDate(epInfo.airDate))
                    }
                    val subtitleText = when {
                        !hasDirectLink && !isEpDownloaded -> "No link • Embed stream only"
                        metaParts.isNotEmpty() -> "S$selectedSeason • E$epNum • ${metaParts.joinToString(" • ")}"
                        else -> "Season $selectedSeason • Direct Ready"
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = MaterialTheme.shapes.medium, // 14.dp
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .alpha(if (!hasDirectLink && !isEpDownloaded) 0.58f else 1f)
                            .testTag("details_episode_${selectedSeason}_$epNum")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isEpDownloaded || isEpChecked,
                                    enabled = isEpEligible,
                                    onCheckedChange = { checked ->
                                        if (!isEpEligible) return@Checkbox
                                        val updated = if (checked) {
                                            currentSeasonSelection + epNum
                                        } else {
                                            currentSeasonSelection - epNum
                                        }
                                        selectedEpisodesBySeason = selectedEpisodesBySeason + (selectedSeason to updated)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = if (isEpDownloaded) colors.success else colors.primary,
                                        uncheckedColor = colors.textSecondary,
                                        disabledCheckedColor = colors.success.copy(alpha = 0.65f),
                                        disabledUncheckedColor = colors.textMuted.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.testTag("details_ep_checkbox_${selectedSeason}_$epNum")
                                )

                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.surfaceElevated)
                                        .border(1.dp, colors.border, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$epNum",
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = epInfo.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        ),
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = subtitleText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                when {
                                    isEpDownloaded -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(colors.success.copy(alpha = 0.16f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .testTag("details_ep_downloaded_badge_${selectedSeason}_$epNum")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DownloadDone,
                                                contentDescription = "Downloaded",
                                                tint = colors.success,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Downloaded",
                                                color = colors.success,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    !hasDirectLink -> {
                                        Text(
                                            text = "No link",
                                            color = colors.textMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(colors.surfaceElevated)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    epDownload == null -> {
                                        IconButton(
                                            onClick = { onDownloadClick(selectedSeason, epNum, epInfo.name) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("details_download_ep_${selectedSeason}_$epNum")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Download Episode $epNum",
                                                tint = colors.textSecondary
                                            )
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (isEpDownloaded && epDownload != null) {
                                            onPlayOffline(epDownload)
                                        } else {
                                            onWatchClick(selectedSeason, epNum)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = colors.surfaceElevated,
                                        contentColor = colors.textPrimary
                                    ),
                                    border = BorderStroke(1.dp, colors.border),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Play",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }
                            }

                            if (epDownload != null && !isEpDownloaded) {
                                InlineEpisodeDownloadRow(
                                    download = epDownload,
                                    onPause = { onPauseDownload(epDownload.id) },
                                    onResume = { onResumeDownload(epDownload.id) },
                                    onCancel = { onCancelDownload(epDownload.id) },
                                    onRetry = { onRetryDownload(epDownload) }
                                )
                            }

                            if (epInfo.overview.isNotBlank()) {
                                Text(
                                    text = epInfo.overview,
                                    color = colors.textSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Similar Movies & Series Row
        if (similarMedia.isNotEmpty()) {
            item(
                key = "details_similar_section_${media.id}",
                contentType = "similar_section"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .testTag("details_similar_section")
                ) {
                    SectionHeader(
                        title = if (media.mediaType == "tv") "Similar TV Series" else "Similar Movies"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = similarMedia,
                            key = { "sim_${it.id}" },
                            contentType = { "movie_card" }
                        ) { simItem ->
                            PosterCard(
                                media = simItem,
                                onClick = { onSimilarMediaClick(simItem) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveDownloadControlCard(
    media: MediaItem,
    season: Int,
    episode: Int,
    download: DownloadEntity?,
    onStartDownload: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onPlayOffline: () -> Unit
) {
    val colors = CinetraTheme.colors
    val status = download?.parsedStatus
    val hasCompletedFile = status == DownloadStatus.COMPLETED &&
        !download.localFilePath.isBlank() &&
        File(download.localFilePath).exists()

    when {
        hasCompletedFile && download != null -> {
            Button(
                onClick = onPlayOffline,
                shape = MaterialTheme.shapes.small, // 12.dp
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.success,
                    contentColor = colors.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("details_downloaded_play_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = "Downloaded",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Downloaded ✓ — Play",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        download != null && status != null && (status.isActive || status == DownloadStatus.FAILED) -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = MaterialTheme.shapes.medium, // 14.dp
                border = BorderStroke(
                    width = 1.dp,
                    color = if (status == DownloadStatus.FAILED) colors.primary.copy(alpha = 0.6f) else colors.border
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("details_live_download_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = (download.progressPercent / 100f).coerceIn(0f, 1f),
                        animationSpec = tween(durationMillis = 220, easing = CineEase),
                        label = "detailsLiveDownloadProgress"
                    )
                    val animatedPercent by animateIntAsState(
                        targetValue = download.progressPercent.coerceIn(0, 100),
                        animationSpec = tween(durationMillis = 200, easing = CineEase),
                        label = "detailsLiveDownloadPercent"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (status) {
                                DownloadStatus.RUNNING -> "Downloading • $animatedPercent%"
                                DownloadStatus.PAUSED -> "Paused • $animatedPercent%"
                                DownloadStatus.QUEUED -> "Queued • Waiting for slot"
                                DownloadStatus.FAILED -> "Download Failed"
                                else -> status.displayLabel
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (status) {
                                DownloadStatus.FAILED -> colors.primary
                                DownloadStatus.PAUSED -> colors.textSecondary
                                else -> colors.textPrimary
                            }
                        )
                        val sizeText = if (download.totalBytes > 0L) {
                            "${formatDownloadBytes(download.bytesDownloaded)} / ${formatDownloadBytes(download.totalBytes)}"
                        } else if (download.bytesDownloaded > 0L) {
                            formatDownloadBytes(download.bytesDownloaded)
                        } else {
                            download.qualityLabel
                        }
                        Text(
                            text = sizeText,
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }

                    if (status != DownloadStatus.FAILED) {
                        if (download.progressPercent > 0) {
                            StylishProgressBar(
                                progress = animatedProgress,
                                label = "$animatedPercent%",
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(
                                color = colors.primary,
                                trackColor = colors.surfaceHover,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }
                    } else if (download.errorReason.isNotBlank()) {
                        Text(
                            text = download.errorReason,
                            fontSize = 11.sp,
                            color = colors.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (status) {
                            DownloadStatus.RUNNING, DownloadStatus.QUEUED -> {
                                OutlinedButton(
                                    onClick = onPause,
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = colors.surfaceElevated,
                                        contentColor = colors.textPrimary
                                    ),
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("details_pause_download_button")
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pause", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            DownloadStatus.PAUSED -> {
                                OutlinedButton(
                                    onClick = onResume,
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = colors.surfaceElevated,
                                        contentColor = colors.textPrimary
                                    ),
                                    border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("details_resume_download_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Resume", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            DownloadStatus.FAILED -> {
                                OutlinedButton(
                                    onClick = onRetry,
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = colors.surfaceElevated,
                                        contentColor = colors.textPrimary
                                    ),
                                    border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.7f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("details_retry_download_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            else -> Unit
                        }

                        OutlinedButton(
                            onClick = onCancel,
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.surfaceElevated,
                                contentColor = colors.textSecondary
                            ),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("details_cancel_download_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel", color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        else -> {
            OutlinedButton(
                onClick = onStartDownload,
                shape = MaterialTheme.shapes.small, // 12.dp
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.surfaceElevated,
                    contentColor = colors.textPrimary
                ),
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("details_download_moviebox_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = "Download Offline",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (media.mediaType == "tv") {
                        "Download S$season:E$episode"
                    } else {
                        "Download Offline"
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun InlineEpisodeDownloadRow(
    download: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val colors = CinetraTheme.colors
    val status = download.parsedStatus
    val animatedProgress by animateFloatAsState(
        targetValue = (download.progressPercent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 220, easing = CineEase),
        label = "inlineEpProgress"
    )
    val animatedPercent by animateIntAsState(
        targetValue = download.progressPercent.coerceIn(0, 100),
        animationSpec = tween(durationMillis = 200, easing = CineEase),
        label = "inlineEpPercent"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (status != DownloadStatus.FAILED) {
            if (download.progressPercent > 0) {
                StylishProgressBar(
                    progress = animatedProgress,
                    label = "$animatedPercent%",
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(
                    color = colors.primary,
                    trackColor = colors.surfaceHover,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val statusSummary = when (status) {
                DownloadStatus.RUNNING -> "$animatedPercent% • ${formatDownloadBytes(download.bytesDownloaded)}"
                DownloadStatus.PAUSED -> "Paused ($animatedPercent%)"
                DownloadStatus.QUEUED -> "Queued"
                DownloadStatus.FAILED -> download.errorReason.ifBlank { "Failed" }
                else -> status.displayLabel
            }
            Text(
                text = statusSummary,
                fontSize = 11.sp,
                color = if (status == DownloadStatus.FAILED) colors.primary else colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when (status) {
                    DownloadStatus.RUNNING, DownloadStatus.QUEUED -> {
                        IconButton(onClick = onPause, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        IconButton(onClick = onResume, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = colors.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    DownloadStatus.FAILED -> {
                        IconButton(onClick = onRetry, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = colors.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    else -> Unit
                }
                IconButton(onClick = onCancel, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = colors.textMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
