package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminAnnouncementEntity
import com.example.data.model.ContinueWatchingEntity
import com.example.data.model.ContinueWatchingPolicy
import com.example.data.model.MediaItem
import com.example.data.model.UpcomingReleasePolicy
import com.example.data.model.mergeWithCatalog
import com.example.data.model.toMediaItem
import com.example.ui.components.AnnouncementBanner
import com.example.ui.components.CineEase
import com.example.ui.components.HeroBannerCarousel
import com.example.ui.components.HeroBannerSkeleton
import com.example.ui.components.HomeCatalogSkeleton
import com.example.ui.components.HorizontalMovieListSkeleton
import com.example.ui.components.LandscapeCard
import com.example.ui.components.PosterCard
import com.example.ui.components.RankedPosterCard
import com.example.ui.components.RedButton
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CinetraTheme

@Composable
fun HomeScreen(
    trending: List<MediaItem>,
    popularMovies: List<MediaItem>,
    topRatedTv: List<MediaItem>,
    customMedia: List<MediaItem>,
    continueWatching: List<ContinueWatchingEntity>,
    announcements: List<AdminAnnouncementEntity>,
    isLoading: Boolean,
    isItemInWatchlist: (String) -> Boolean,
    onMediaClick: (MediaItem) -> Unit,
    onWatchClick: (MediaItem) -> Unit,
    onWatchlistToggle: (MediaItem) -> Unit,
    onTrailerClick: (MediaItem) -> Unit,
    onSeeAllClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    onRetry: () -> Unit = {},
    selectedHomeGenre: String = "All",
    onHomeGenreSelect: (String) -> Unit = {},
    onBrowseGenreInExplore: (String) -> Unit = { onSeeAllClick("Trending") },
    onResumeWatching: (MediaItem, Int, Int) -> Unit = { m, _, _ -> onWatchClick(m) },
    onSearchClick: () -> Unit = {},
    trendingTotalResults: Int = trending.size,
    popularMoviesTotalResults: Int = popularMovies.size,
    topRatedTvTotalResults: Int = topRatedTv.size,
    trendingMovies: List<MediaItem> = emptyList(),
    trendingMoviesTotalResults: Int = trendingMovies.size,
    trendingTv: List<MediaItem> = emptyList(),
    trendingTvTotalResults: Int = trendingTv.size,
    newReleases: List<MediaItem> = emptyList(),
    newReleasesTotalResults: Int = newReleases.size,
    upcomingMovies: List<MediaItem> = emptyList(),
    upcomingMoviesTotalResults: Int = upcomingMovies.size,
    topRatedMovies: List<MediaItem> = emptyList(),
    topRatedMoviesTotalResults: Int = topRatedMovies.size,
    recommendedForYou: List<MediaItem> = emptyList(),
    becauseYouWatchedTitle: String = "",
    becauseYouWatchedItems: List<MediaItem> = emptyList(),
    popularInGenreName: String = "Action",
    popularInGenreItems: List<MediaItem> = emptyList(),
    homeLatestReleased: List<MediaItem> = emptyList(),
    homeBollywoodMovies: List<MediaItem> = emptyList(),
    homeSouthIndianMovies: List<MediaItem> = emptyList(),
    homeBanglaMixed: List<MediaItem> = emptyList(),
    homeHollywoodMovies: List<MediaItem> = emptyList(),
    homeKDramaSeries: List<MediaItem> = emptyList(),
    homeCDramaSeries: List<MediaItem> = emptyList(),
    homeUpcomingSoonest: List<MediaItem> = emptyList(),
    onLanguageClick: (code: String, name: String) -> Unit = { _, _ -> }
) {
    val colors = CinetraTheme.colors
    val bottomClearance = CinetraTheme.bottomBarClearance
    val isCatalogEmpty = trending.isEmpty() &&
        popularMovies.isEmpty() &&
        topRatedTv.isEmpty() &&
        trendingMovies.isEmpty() &&
        homeLatestReleased.isEmpty()
    val showInitialSkeleton = isLoading && isCatalogEmpty && continueWatching.isEmpty() && customMedia.isEmpty()

    AnimatedContent(
        targetState = showInitialSkeleton,
        transitionSpec = {
            fadeIn(tween(durationMillis = 260, easing = CineEase)) togetherWith
                fadeOut(tween(durationMillis = 180, easing = CineEase))
        },
        modifier = modifier.fillMaxSize(),
        label = "home_skeleton_to_catalog"
    ) { showingSkeleton ->
        if (showingSkeleton) {
            HomeCatalogSkeleton(modifier = Modifier.fillMaxSize())
        } else {
            val heroItems = remember(trending, popularMovies, homeLatestReleased) {
                val base = trending.ifEmpty { homeLatestReleased }.ifEmpty { popularMovies }
                base.filter { !UpcomingReleasePolicy.isStrictlyFutureRelease(it.releaseDate) }
                    .distinctBy { it.id }
                    .take(6)
            }
            val latestList = remember(homeLatestReleased, newReleases) {
                homeLatestReleased.ifEmpty {
                    newReleases.filter { !UpcomingReleasePolicy.isStrictlyFutureRelease(it.releaseDate) }
                }
            }
            val hotMoviesList = remember(trendingMovies, popularMovies) {
                trendingMovies.ifEmpty { popularMovies }
                    .filter { !UpcomingReleasePolicy.isStrictlyFutureRelease(it.releaseDate) }
                    .distinctBy { it.id }
                    .take(10)
            }
            val hotSeriesList = remember(trendingTv, topRatedTv) {
                trendingTv.ifEmpty { topRatedTv }
                    .filter { !UpcomingReleasePolicy.isStrictlyFutureRelease(it.releaseDate) }
                    .distinctBy { it.id }
            }
            val upcomingList = remember(homeUpcomingSoonest, upcomingMovies) {
                homeUpcomingSoonest.ifEmpty {
                    upcomingMovies.filter { UpcomingReleasePolicy.isStrictlyFutureRelease(it.releaseDate) }
                        .sortedBy { it.releaseDate }
                }
            }
            val allLoadedUnique = remember(
                trending,
                trendingMovies,
                trendingTv,
                popularMovies,
                upcomingMovies,
                topRatedMovies,
                topRatedTv,
                customMedia,
                latestList
            ) {
                (trending + trendingMovies + trendingTv + popularMovies + upcomingMovies + topRatedMovies + topRatedTv + customMedia + latestList)
                    .distinctBy { it.id }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Centered 3:4 Hero Poster Carousel with 12dp neighbor peek
                if (heroItems.isNotEmpty()) {
                    HeroBannerCarousel(
                        items = heroItems,
                        isItemInWatchlist = isItemInWatchlist,
                        onWatchClick = onWatchClick,
                        onWatchlistClick = onWatchlistToggle,
                        onTrailerClick = onTrailerClick,
                        onCardClick = onMediaClick
                    )
                } else if (isLoading) {
                    HeroBannerSkeleton()
                }

                // Active Admin Announcement Notices
                AnnouncementBanner(announcements = announcements)

                // Real TMDB Network Error / Empty Catalog State
                if (isCatalogEmpty && !isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(colors.surface)
                            .border(1.dp, colors.border, MaterialTheme.shapes.medium)
                            .padding(24.dp)
                            .testTag("home_network_error_card"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (errorMessage != null) "Unable to Load Live Catalog" else "No Titles Available Yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage
                                    ?: "Connect to the internet to stream live movies and series from TMDb, or add custom titles in Admin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            RedButton(
                                onClick = onRetry,
                                modifier = Modifier.testTag("home_retry_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry Connection", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (isLoading && isCatalogEmpty) {
                    HorizontalMovieListSkeleton(titleWidth = 120.dp)
                    HorizontalMovieListSkeleton(titleWidth = 148.dp)
                    HorizontalMovieListSkeleton(titleWidth = 136.dp)
                }

                // 1. Latest (newly released movies & series mixed, newest first)
                if (latestList.isNotEmpty()) {
                    HomePosterRow(
                        title = "Latest",
                        items = latestList,
                        keyPrefix = "latest",
                        rowTestTag = "home_latest_row",
                        onSeeAllClick = { onSeeAllClick("Latest") },
                        onMediaClick = onMediaClick
                    )
                }

                // 2. Continue Watching (only if user has watch history)
                if (continueWatching.isNotEmpty()) {
                    val catalogById = remember(allLoadedUnique) {
                        allLoadedUnique.associateBy { it.id }
                    }
                    HomeSectionBlock(
                        title = "Continue Watching",
                        onSeeAllClick = { onSeeAllClick("Continue Watching") }
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.testTag("home_continue_watching_row")
                        ) {
                            items(
                                items = continueWatching,
                                key = { it.id },
                                contentType = { "continue_watching_card" }
                            ) { item ->
                                val media = remember(item, catalogById) {
                                    item.toMediaItem().mergeWithCatalog(catalogById[item.id])
                                }
                                val (episodeMeta, progressLabel) = remember(
                                    item.mediaType,
                                    item.season,
                                    item.episode,
                                    item.progressPercent,
                                    item.watchedSeconds,
                                    item.durationSeconds
                                ) {
                                    formatContinueWatchingMetaAndLabel(item)
                                }
                                val progressFraction = remember(item.progressPercent, item.watchedSeconds, item.durationSeconds) {
                                    when {
                                        item.progressPercent > 0 -> (item.progressPercent / 100f).coerceIn(0.02f, 1f)
                                        item.durationSeconds > 0L -> (item.watchedSeconds.toFloat() / item.durationSeconds.toFloat()).coerceIn(0.02f, 1f)
                                        item.watchedSeconds > 0L -> 0.1f
                                        else -> 0f
                                    }
                                }
                                LandscapeCard(
                                    title = media.title,
                                    imageUrl = media.fullBackdropUrl.ifBlank { media.fullPosterUrl },
                                    metaText = episodeMeta,
                                    progressFraction = progressFraction.takeIf { it > 0f },
                                    progressLabel = progressLabel,
                                    onClick = { onMediaClick(media) },
                                    testTag = "movie_card_${media.id}",
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }

                // 3. Hot Movies (top 10 ranked posters 1–10)
                if (hotMoviesList.isNotEmpty()) {
                    HomeSectionBlock(
                        title = "Hot Movies",
                        onSeeAllClick = { onSeeAllClick("HotMovies") }
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.testTag("home_top10_movies_row")
                        ) {
                            items(
                                count = hotMoviesList.size,
                                key = { idx -> "hot_m_${hotMoviesList[idx].id}" },
                                contentType = { "ranked_poster_card" }
                            ) { idx ->
                                val item = hotMoviesList[idx]
                                RankedPosterCard(
                                    rank = idx + 1,
                                    media = item,
                                    onClick = { onMediaClick(item) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }

                // 4. Hot Series (trending TV shows, regular posters)
                if (hotSeriesList.isNotEmpty()) {
                    HomePosterRow(
                        title = "Hot Series",
                        items = hotSeriesList,
                        keyPrefix = "hot_series",
                        rowTestTag = "home_hot_series_row",
                        onSeeAllClick = { onSeeAllClick("HotSeries") },
                        onMediaClick = onMediaClick
                    )
                }

                // 5. Bollywood (Hindi released movies)
                if (homeBollywoodMovies.isNotEmpty()) {
                    HomePosterRow(
                        title = "Bollywood",
                        items = homeBollywoodMovies,
                        keyPrefix = "bollywood",
                        rowTestTag = "home_bollywood_row",
                        onSeeAllClick = { onSeeAllClick("Bollywood") },
                        onMediaClick = onMediaClick
                    )
                }

                // 6. South Indian (Telugu / Tamil / Malayalam / Kannada released movies)
                if (homeSouthIndianMovies.isNotEmpty()) {
                    HomePosterRow(
                        title = "South Indian",
                        items = homeSouthIndianMovies,
                        keyPrefix = "south_indian",
                        rowTestTag = "home_south_indian_row",
                        onSeeAllClick = { onSeeAllClick("SouthIndian") },
                        onMediaClick = onMediaClick
                    )
                }

                // 7. Bangla (Bengali released movies & series)
                if (homeBanglaMixed.isNotEmpty()) {
                    HomePosterRow(
                        title = "Bangla",
                        items = homeBanglaMixed,
                        keyPrefix = "bangla",
                        rowTestTag = "home_bangla_row",
                        onSeeAllClick = { onSeeAllClick("Bangla") },
                        onMediaClick = onMediaClick
                    )
                }

                // 8. Hollywood (English released movies)
                if (homeHollywoodMovies.isNotEmpty()) {
                    HomePosterRow(
                        title = "Hollywood",
                        items = homeHollywoodMovies,
                        keyPrefix = "hollywood",
                        rowTestTag = "home_hollywood_row",
                        onSeeAllClick = { onSeeAllClick("Hollywood") },
                        onMediaClick = onMediaClick
                    )
                }

                // 9. Original K-Drama (Korean released series)
                if (homeKDramaSeries.isNotEmpty()) {
                    HomePosterRow(
                        title = "Original K-Drama",
                        items = homeKDramaSeries,
                        keyPrefix = "kdrama",
                        rowTestTag = "home_kdrama_row",
                        onSeeAllClick = { onSeeAllClick("KDrama") },
                        onMediaClick = onMediaClick
                    )
                }

                // 10. Original C-Drama (Chinese released series)
                if (homeCDramaSeries.isNotEmpty()) {
                    HomePosterRow(
                        title = "Original C-Drama",
                        items = homeCDramaSeries,
                        keyPrefix = "cdrama",
                        rowTestTag = "home_cdrama_row",
                        onSeeAllClick = { onSeeAllClick("CDrama") },
                        onMediaClick = onMediaClick
                    )
                }

                // 11. Upcoming (not-yet-released titles, sorted soonest first)
                if (upcomingList.isNotEmpty()) {
                    HomePosterRow(
                        title = "Upcoming",
                        items = upcomingList,
                        keyPrefix = "upcoming",
                        rowTestTag = "home_upcoming_row",
                        onSeeAllClick = { onSeeAllClick("UpcomingSoonest") },
                        onMediaClick = onMediaClick
                    )
                }

                Spacer(modifier = Modifier.height(bottomClearance + 24.dp))
            }
        }
    }
}

@Composable
private fun HomePosterRow(
    title: String,
    items: List<MediaItem>,
    keyPrefix: String,
    rowTestTag: String,
    onSeeAllClick: () -> Unit,
    onMediaClick: (MediaItem) -> Unit
) {
    HomeSectionBlock(
        title = title,
        onSeeAllClick = onSeeAllClick
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag(rowTestTag)
        ) {
            items(
                items = items,
                key = { "${keyPrefix}_${it.id}" },
                contentType = { "poster_card" }
            ) { item ->
                PosterCard(
                    media = item,
                    onClick = { onMediaClick(item) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun HomeSectionBlock(
    title: String,
    onSeeAllClick: () -> Unit,
    seeAllTestTag: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {
        SectionHeader(
            title = title,
            onSeeAllClick = onSeeAllClick,
            seeAllTestTag = seeAllTestTag
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

private fun formatContinueWatchingMetaAndLabel(item: ContinueWatchingEntity): Pair<String, String?> {
    val (safeSeason, safeEp) = ContinueWatchingPolicy.normalizeSeasonEpisode(
        mediaType = item.mediaType,
        season = item.season,
        episode = item.episode
    )
    val remainingMin = if (item.durationSeconds > item.watchedSeconds && item.durationSeconds > 0L) {
        ((item.durationSeconds - item.watchedSeconds) / 60L).coerceAtLeast(1L)
    } else {
        null
    }
    val progressLabel = when {
        remainingMin != null -> "${remainingMin}m left"
        item.progressPercent > 0 -> "${item.progressPercent}%"
        else -> null
    }
    val metaText = if (item.mediaType == "tv") "S$safeSeason:E$safeEp" else ""
    return metaText to progressLabel
}
