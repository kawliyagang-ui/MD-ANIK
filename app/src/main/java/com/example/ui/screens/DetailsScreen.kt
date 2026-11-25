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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.CineEase
import com.example.ui.components.CineGradientButton
import com.example.ui.components.DetailedMovieView
import com.example.ui.components.DetailsEpisodesSkeleton
import com.example.ui.components.MovieCard
import com.example.ui.components.formatReleaseDate
import com.example.ui.theme.CineTheme
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
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
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
            .background(gradients.appBackdrop),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item(
            key = "details_header_${media.id}",
            contentType = "details_header"
        ) {
            DetailedMovieView(
                media = media,
                backdropHeight = 260.dp,
                showBackButton = true,
                onDismissOrBack = onBack,
                actionsContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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

                        val watchCtaLabel = when {
                            activeTargetDownload?.parsedStatus == DownloadStatus.COMPLETED && media.mediaType == "tv" ->
                                "Play Offline • S${targetWatchPair.first}:E${targetWatchPair.second}"
                            activeTargetDownload?.parsedStatus == DownloadStatus.COMPLETED ->
                                "Play Offline"
                            continueWatchingItem != null && media.mediaType == "tv" ->
                                "Watch Now • Resume S${targetWatchPair.first}:E${targetWatchPair.second}"
                            continueWatchingItem != null ->
                                "Watch Now • Resume"
                            else -> "Watch Now"
                        }

                        // Primary CTA: Watch Now (uses resolvePlaybackSource priority)
                        CineGradientButton(
                            onClick = { onWatchClick(targetWatchPair.first, targetWatchPair.second) },
                            shape = RoundedCornerShape(10.dp),
                            fillWidth = true,
                            minHeight = 48.dp,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("details_watch_now_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = watchCtaLabel,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = watchCtaLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Secondary CTAs: Favorite & Watch Trailer
                        val favScale by animateFloatAsState(
                            targetValue = if (isInWatchlist) 1.03f else 1f,
                            animationSpec = tween(durationMillis = 180, easing = CineEase),
                            label = "favorite_button_scale"
                        )
                        val favContainerColor by animateColorAsState(
                            targetValue = if (isInWatchlist) colors.softRose.copy(alpha = 0.16f) else colors.surfaceElevated,
                            animationSpec = tween(220),
                            label = "favorite_container_color"
                        )
                        val favContentColor by animateColorAsState(
                            targetValue = if (isInWatchlist) colors.softRose else colors.textPrimary,
                            animationSpec = tween(220),
                            label = "favorite_content_color"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onWatchlistToggle,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = favContainerColor,
                                    contentColor = favContentColor
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isInWatchlist) colors.softRose.copy(alpha = 0.65f) else colors.border
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .graphicsLayer {
                                        scaleX = favScale
                                        scaleY = favScale
                                    }
                                    .testTag("details_watchlist_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isInWatchlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isInWatchlist) "Remove from Favorites" else "Add to Favorites",
                                    tint = if (isInWatchlist) colors.softRose else colors.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isInWatchlist) "Favorited" else "Favorite",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = onTrailerClick,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = colors.surfaceElevated,
                                    contentColor = colors.textPrimary
                                ),
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("details_trailer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Watch Trailer",
                                    tint = colors.gold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Trailer",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Live Download Control for Movie / Target Episode
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

                        if (media.mediaType == "tv" && (media.latestEpisodeLabel.isNotBlank() || isInWatchlist)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.primaryContainer.copy(alpha = 0.45f))
                                    .border(0.75.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = colors.gold,
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
                                    fontSize = 11.sp,
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onReportProblemClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.surfaceElevated.copy(alpha = 0.7f),
                                contentColor = colors.textSecondary
                            ),
                            border = BorderStroke(1.dp, colors.border.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("details_report_problem_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Report a Problem",
                                tint = colors.softRose,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Report Video / Info Issue",
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
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
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            val seasonLabel = if (media.numberOfSeasons == 1) "1 Season" else "${media.numberOfSeasons} Seasons"
                            val totalEpCount = media.totalEpisodeCount()
                            Text(
                                text = "$seasonLabel • $totalEpCount Episodes",
                                color = colors.gold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.testTag("details_seasons_summary")
                            )
                        }

                        if (isLoadingSeasons || loadingSeasonNumber == selectedSeason) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.surfaceElevated)
                                    .border(0.75.dp, colors.border, RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = colors.softRose,
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Seasons pills
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
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedSeason = sNum
                                    onSeasonSelected(sNum)
                                },
                                label = {
                                    Text(
                                        text = chipLabel,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.accentMid,
                                    selectedLabelColor = Color.White,
                                    containerColor = colors.surfaceElevated,
                                    labelColor = colors.textPrimary
                                ),
                                modifier = Modifier.testTag("details_season_chip_$sNum")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Season Batch Download Controls (Download Season + Select all + Download selected (N))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
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
                                            fontSize = 10.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onDownloadSeason(selectedSeason) },
                                    enabled = seasonEligibility.eligibleEpisodes.isNotEmpty(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (seasonEligibility.eligibleEpisodes.isNotEmpty()) colors.softRose.copy(alpha = 0.7f) else colors.border
                                    ),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("details_download_season_$selectedSeason")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = "Download Season $selectedSeason",
                                        tint = if (seasonEligibility.eligibleEpisodes.isNotEmpty()) colors.softRose else colors.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Download Season",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (seasonEligibility.eligibleEpisodes.isNotEmpty()) colors.textPrimary else colors.textMuted
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = currentSeasonSelection.isNotEmpty(),
                                enter = fadeIn(tween(180, easing = CineEase)) + expandVertically(tween(220, easing = CineEase)),
                                exit = fadeOut(tween(140, easing = CineEase)) + shrinkVertically(tween(180, easing = CineEase))
                            ) {
                                CineGradientButton(
                                    onClick = {
                                        val picked = currentSeasonSelection
                                        onDownloadSelectedEpisodes(selectedSeason, picked)
                                        selectedEpisodesBySeason = selectedEpisodesBySeason + (selectedSeason to emptySet())
                                    },
                                    shape = RoundedCornerShape(8.dp),
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
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Download selected (${currentSeasonSelection.size})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
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
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                    shape = RoundedCornerShape(10.dp),
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
                                    .size(28.dp)
                                    .background(colors.background.copy(alpha = 0.65f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$epNum",
                                    color = colors.gold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = epInfo.name,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = subtitleText,
                                    color = if (!hasDirectLink && !isEpDownloaded) colors.textMuted else colors.textSecondary,
                                    fontSize = 11.sp,
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
                                            .clip(RoundedCornerShape(6.dp))
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
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.background.copy(alpha = 0.5f))
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
                                            tint = colors.gold
                                        )
                                    }
                                }
                            }

                            CineGradientButton(
                                onClick = {
                                    if (isEpDownloaded && epDownload != null) {
                                        onPlayOffline(epDownload)
                                    } else {
                                        onWatchClick(selectedSeason, epNum)
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                minHeight = 40.dp,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isEpDownloaded) "Play" else "Play",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Inline live download progress row for active/failed episodes
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
                    Text(
                        text = if (media.mediaType == "tv") "Similar TV Series" else "Similar Movies",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
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
                            MovieCard(
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
    val colors = CineTheme.colors
    val status = download?.parsedStatus
    val hasCompletedFile = status == DownloadStatus.COMPLETED &&
        !download.localFilePath.isBlank() &&
        File(download.localFilePath).exists()

    when {
        hasCompletedFile && download != null -> {
            Button(
                onClick = onPlayOffline,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.success,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("details_downloaded_play_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = "Downloaded",
                    tint = Color.White,
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
                colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (status == DownloadStatus.FAILED) colors.softRose.copy(alpha = 0.6f) else colors.primary.copy(alpha = 0.55f),
                        RoundedCornerShape(10.dp)
                    )
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
                                DownloadStatus.FAILED -> colors.softRose
                                DownloadStatus.PAUSED -> colors.gold
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
                            color = colors.textSecondary
                        )
                    }

                    if (status != DownloadStatus.FAILED) {
                        if (download.progressPercent > 0) {
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                color = if (status == DownloadStatus.PAUSED) colors.gold else colors.primary,
                                trackColor = colors.background,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        } else {
                            LinearProgressIndicator(
                                color = if (status == DownloadStatus.PAUSED) colors.gold else colors.primary,
                                trackColor = colors.background,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }
                    } else if (download.errorReason.isNotBlank()) {
                        Text(
                            text = download.errorReason,
                            fontSize = 11.sp,
                            color = colors.softRose,
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
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("details_pause_download_button")
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null, tint = colors.gold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pause", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            DownloadStatus.PAUSED -> {
                                Button(
                                    onClick = onResume,
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("details_resume_download_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Resume", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            DownloadStatus.FAILED -> {
                                Button(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("details_retry_download_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            else -> Unit
                        }

                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, colors.softRose.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("details_cancel_download_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = colors.softRose, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cancel", color = colors.softRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        else -> {
            val hasDirect = if (media.mediaType == "tv") {
                media.resolveDirectLinkForEpisode(season, episode).hasAnyLink()
            } else {
                media.effectiveDownloadUrl().startsWith("http", ignoreCase = true)
            }
            OutlinedButton(
                onClick = onStartDownload,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.primaryContainer.copy(alpha = 0.55f),
                    contentColor = colors.textPrimary
                ),
                border = BorderStroke(1.dp, colors.softRose.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("details_download_moviebox_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = "Download Offline",
                    tint = colors.softRose,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (media.mediaType == "tv") {
                        "Download S$season:E$episode"
                    } else {
                        "Download Offline"
                    },
                    fontWeight = FontWeight.Bold,
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
    val colors = CineTheme.colors
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
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = if (status == DownloadStatus.PAUSED) colors.gold else colors.primary,
                    trackColor = colors.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                )
            } else {
                LinearProgressIndicator(
                    color = if (status == DownloadStatus.PAUSED) colors.gold else colors.primary,
                    trackColor = colors.background,
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
                color = if (status == DownloadStatus.FAILED) colors.softRose else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when (status) {
                    DownloadStatus.RUNNING, DownloadStatus.QUEUED -> {
                        IconButton(onClick = onPause, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = colors.gold, modifier = Modifier.size(18.dp))
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        IconButton(onClick = onResume, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = colors.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    DownloadStatus.FAILED -> {
                        IconButton(onClick = onRetry, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = colors.gold, modifier = Modifier.size(18.dp))
                        }
                    }
                    else -> Unit
                }
                IconButton(onClick = onCancel, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = colors.softRose, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
