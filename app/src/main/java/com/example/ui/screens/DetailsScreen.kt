package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.IconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.data.model.EpisodeInfo
import com.example.data.model.MediaItem
import com.example.ui.components.CineGradientButton
import com.example.ui.components.DetailedMovieView
import com.example.ui.components.MovieCard
import com.example.ui.components.formatReleaseDate
import com.example.ui.theme.CineTheme

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
    onDownloadClick: (season: Int, episode: Int, episodeTitle: String) -> Unit = { _, _, _ -> }
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

    BackHandler {
        onBack()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(gradients.appBackdrop),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item(key = "details_header_${media.id}") {
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
                        val watchCtaLabel = when {
                            continueWatchingItem != null && media.mediaType == "tv" ->
                                "Watch Now • Resume S${targetWatchPair.first}:E${targetWatchPair.second}"
                            continueWatchingItem != null ->
                                "Watch Now • Resume"
                            else -> "Watch Now"
                        }

                        // Primary CTA: Watch Now
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
                            targetValue = if (isInWatchlist) 1.04f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
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
                                border = androidx.compose.foundation.BorderStroke(
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
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
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

                        // MovieBox API Offline Download CTA
                        OutlinedButton(
                            onClick = {
                                val epName = episodeEntries
                                    .firstOrNull { it.episodeNumber == targetWatchPair.second }
                                    ?.name
                                    .orEmpty()
                                onDownloadClick(targetWatchPair.first, targetWatchPair.second, epName)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.primaryContainer.copy(alpha = 0.55f),
                                contentColor = colors.textPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.softRose.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("details_download_moviebox_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Download via MovieBox API",
                                tint = colors.softRose,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (media.mediaType == "tv") {
                                    "Download S${targetWatchPair.first}:E${targetWatchPair.second} (MovieBox HD)"
                                } else {
                                    "Download Offline (MovieBox HD)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

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
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border.copy(alpha = 0.7f)),
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
            item(key = "tv_seasons_header_${media.id}") {
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

                    // Seasons pills (automatically populated from TMDB)
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

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            items(
                items = episodeEntries,
                key = { epInfo -> "ep_${media.id}_${selectedSeason}_${epInfo.episodeNumber}" }
            ) { epInfo ->
                val epNum = epInfo.episodeNumber
                val metaParts = buildList {
                    epInfo.runtimeMinutes?.let { add("${it}m") }
                    if (epInfo.airDate.isNotBlank()) add(formatReleaseDate(epInfo.airDate))
                }
                val subtitleText = if (metaParts.isNotEmpty()) {
                    "S$selectedSeason • E$epNum • ${metaParts.joinToString(" • ")}"
                } else {
                    "Season $selectedSeason • Vidly Stream Ready"
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("details_episode_${selectedSeason}_$epNum")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(colors.background.copy(alpha = 0.65f), RoundedCornerShape(15.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$epNum",
                                        color = colors.gold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
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
                                        color = colors.textSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { onDownloadClick(selectedSeason, epNum, epInfo.name) },
                                modifier = Modifier.testTag("details_download_ep_${selectedSeason}_$epNum")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Episode $epNum via MovieBox",
                                    tint = colors.gold
                                )
                            }

                            CineGradientButton(
                                onClick = { onWatchClick(selectedSeason, epNum) },
                                shape = RoundedCornerShape(6.dp),
                                minHeight = 36.dp,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (epInfo.overview.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = epInfo.overview,
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Similar Movies & Series Row
        if (similarMedia.isNotEmpty()) {
            item(key = "details_similar_section_${media.id}") {
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
                        items(similarMedia, key = { "sim_${it.id}" }) { simItem ->
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
