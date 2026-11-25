package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AdminAnnouncementEntity
import com.example.data.model.BrowseLanguageOption
import com.example.data.model.CatalogBatchPolicy
import com.example.data.model.ContinueWatchingEntity
import com.example.data.model.ContinueWatchingPolicy
import com.example.data.model.MediaItem
import com.example.data.model.TmdbFilterCatalog
import com.example.data.model.mergeWithCatalog
import com.example.data.model.toMediaItem
import com.example.ui.components.AnnouncementBanner
import com.example.ui.components.CatalogCountChip
import com.example.ui.components.CineEase
import com.example.ui.components.CineGradientButton
import com.example.ui.components.GenreFilterBar
import com.example.ui.components.HeroBannerCarousel
import com.example.ui.components.HeroBannerSkeleton
import com.example.ui.components.HomeCatalogSkeleton
import com.example.ui.components.HomeGenreCatalog
import com.example.ui.components.HorizontalMovieListSkeleton
import com.example.ui.components.MovieCard
import com.example.ui.components.TopTenCard
import com.example.ui.components.cinePressScale
import com.example.ui.theme.CineTheme

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
    onLanguageClick: (code: String, name: String) -> Unit = { _, _ -> }
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    val isCatalogEmpty = trending.isEmpty() && popularMovies.isEmpty() && topRatedTv.isEmpty() && trendingMovies.isEmpty()
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
    val heroItems = remember(trending, popularMovies) {
        (trending.ifEmpty { popularMovies }).distinctBy { it.id }.take(6)
    }
    val availableGenres = remember(trending) { HomeGenreCatalog.availableGenres(trending) }
    val filteredTrending = remember(trending, selectedHomeGenre) {
        HomeGenreCatalog.filterByGenre(trending, selectedHomeGenre)
    }
    val topTenMoviesList = remember(trendingMovies, popularMovies) {
        (trendingMovies.ifEmpty { popularMovies }).take(10)
    }
    val topTenTvList = remember(trendingTv, topRatedTv) {
        (trendingTv.ifEmpty { topRatedTv }).take(10)
    }
    val searchBarInteraction = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradients.appBackdrop)
            .verticalScroll(rememberScrollState())
    ) {
        // Clickable Search Bar opening Dedicated Search & Results Page
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .minimumInteractiveComponentSize()
                .cinePressScale(interactionSource = searchBarInteraction)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceElevated)
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = searchBarInteraction,
                    indication = ripple(),
                    onClick = onSearchClick
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("home_search_bar"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = colors.softRose,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Search movies, TV series, anime...",
                color = colors.textMuted,
                fontSize = 14.sp
            )
        }

        // Hero Banner Carousel (or skeleton while live TMDb data is loading)
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

        // Content Feed Header with Live Database / Filtered Item Count Chip
        val allLoadedUnique = remember(
            trending, trendingMovies, trendingTv, popularMovies, upcomingMovies, topRatedMovies, topRatedTv, customMedia
        ) {
            (trending + trendingMovies + trendingTv + popularMovies + upcomingMovies + topRatedMovies + topRatedTv + customMedia)
                .distinctBy { it.id }
        }
        val isHomeGenreFiltered = !selectedHomeGenre.equals("All", ignoreCase = true)
        val homeFeedTotalAvailable = remember(
            isHomeGenreFiltered,
            selectedHomeGenre,
            popularMoviesTotalResults,
            topRatedTvTotalResults,
            trendingTotalResults,
            customMedia.size,
            allLoadedUnique
        ) {
            if (isHomeGenreFiltered) {
                HomeGenreCatalog.filterByGenre(allLoadedUnique, selectedHomeGenre).size
            } else {
                CatalogBatchPolicy.resolveHomeDatabaseTotal(
                    moviesTotalResults = popularMoviesTotalResults,
                    tvTotalResults = topRatedTvTotalResults,
                    trendingTotalResults = trendingTotalResults,
                    customMediaCount = customMedia.size,
                    loadedUniqueCount = allLoadedUnique.size
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("home_feed_header"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isHomeGenreFiltered) "Content Feed • $selectedHomeGenre" else "Content Feed",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = colors.textPrimary
            )
            CatalogCountChip(
                totalAvailable = homeFeedTotalAvailable,
                isFiltered = isHomeGenreFiltered,
                isLoading = isLoading && isCatalogEmpty,
                testTag = "home_catalog_total_chip"
            )
        }

        // Real TMDB Network Error / Empty Catalog State (never fake fallback content)
        if (isCatalogEmpty && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                    .padding(24.dp)
                    .testTag("home_network_error_card"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = colors.softRose,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (errorMessage != null) "Unable to Load Live Catalog" else "No Titles Available Yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage
                            ?: "Connect to the internet to stream live movies and series from TMDb, or add custom titles in Admin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CineGradientButton(
                        onClick = onRetry,
                        shape = RoundedCornerShape(50),
                        minHeight = 48.dp,
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

        // Continue Watching Row (if any)
        if (continueWatching.isNotEmpty()) {
            val catalogById = remember(allLoadedUnique) {
                allLoadedUnique.associateBy { it.id }
            }
            MediaSectionRow(
                title = "Continue Watching",
                icon = Icons.Default.Tv,
                iconColor = colors.gold,
                onSeeAllClick = { onSeeAllClick("Continue Watching") }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = continueWatching,
                        key = { it.id },
                        contentType = { "continue_watching_card" }
                    ) { item ->
                        val media = remember(item, catalogById) {
                            item.toMediaItem().mergeWithCatalog(catalogById[item.id])
                        }
                        val cardLabel = remember(item.mediaType, item.season, item.episode, item.progressPercent, item.watchedSeconds) {
                            val (safeSeason, safeEp) = ContinueWatchingPolicy.normalizeSeasonEpisode(
                                mediaType = item.mediaType,
                                season = item.season,
                                episode = item.episode
                            )
                            val watchedLabel = ContinueWatchingPolicy.formatWatchedDuration(item.watchedSeconds)
                            when {
                                item.mediaType == "tv" && item.progressPercent > 0 ->
                                    "S$safeSeason:E$safeEp • ${item.progressPercent}%"
                                item.mediaType == "tv" && watchedLabel.isNotBlank() ->
                                    "S$safeSeason:E$safeEp • $watchedLabel"
                                item.mediaType == "tv" ->
                                    "S$safeSeason:E$safeEp"
                                item.progressPercent > 0 ->
                                    "${item.progressPercent}% watched"
                                watchedLabel.isNotBlank() ->
                                    "$watchedLabel watched"
                                else -> "Resume"
                            }
                        }
                        MovieCard(
                            media = media,
                            onClick = { onMediaClick(media) },
                            progressPercent = item.progressPercent.takeIf { it > 0 },
                            episodeLabel = cardLabel,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        if (isLoading && isCatalogEmpty) {
            HorizontalMovieListSkeleton(
                titleWidth = 148.dp,
                showFilterChips = true
            )
            HorizontalMovieListSkeleton(titleWidth = 136.dp)
            HorizontalMovieListSkeleton(titleWidth = 162.dp)
        }

        // New Releases (Daily Updated Movies & TV Series)
        if (newReleases.isNotEmpty()) {
            MediaSectionRow(
                title = "New Releases",
                countSummary = "DAILY • " + CatalogBatchPolicy.formatCompactHeaderTotal(
                    shownCount = newReleases.size,
                    totalAvailable = maxOf(newReleasesTotalResults, newReleases.size)
                ),
                icon = Icons.Default.NewReleases,
                iconColor = colors.gold,
                onSeeAllClick = { onSeeAllClick("NewReleases") }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("home_new_releases_row")
                ) {
                    items(
                        items = newReleases,
                        key = { "new_${it.id}" },
                        contentType = { "movie_card" }
                    ) { item ->
                        MovieCard(
                            media = item,
                            onClick = { onMediaClick(item) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // Top 10 Movies (Netflix-style numbered poster cards)
        if (topTenMoviesList.isNotEmpty()) {
            MediaSectionRow(
                title = "Top 10 Movies",
                countSummary = "TOP 10",
                icon = Icons.Default.EmojiEvents,
                iconColor = colors.gold,
                onSeeAllClick = { onSeeAllClick("Top10Movies") }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("home_top10_movies_row")
                ) {
                    items(
                        count = topTenMoviesList.size,
                        key = { idx -> "top10_m_${topTenMoviesList[idx].id}" },
                        contentType = { "top_ten_card" }
                    ) { idx ->
                        val item = topTenMoviesList[idx]
                        TopTenCard(
                            rank = idx + 1,
                            media = item,
                            onClick = { onMediaClick(item) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // Top 10 TV Series (Netflix-style numbered poster cards)
        if (topTenTvList.isNotEmpty()) {
            MediaSectionRow(
                title = "Top 10 TV Series",
                countSummary = "TOP 10",
                icon = Icons.Default.EmojiEvents,
                iconColor = colors.softRose,
                onSeeAllClick = { onSeeAllClick("Top10Tv") }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("home_top10_tv_row")
                ) {
                    items(
                        count = topTenTvList.size,
                        key = { idx -> "top10_tv_${topTenTvList[idx].id}" },
                        contentType = { "top_ten_card" }
                    ) { idx ->
                        val item = topTenTvList[idx]
                        TopTenCard(
                            rank = idx + 1,
                            media = item,
                            onClick = { onMediaClick(item) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // Smart Recommendations: Recommended For You
        if (recommendedForYou.isNotEmpty()) {
            MediaSectionRow(
                title = "Recommended For You",
                countSummary = "SMART PICK",
                icon = Icons.Default.AutoAwesome,
                iconColor = colors.softRose,
                onSeeAllClick = { onSeeAllClick("TopRatedMovies") }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("home_recommended_for_you_row")
                ) {
                    items(
                        items = recommendedForYou,
                        key = { "rec_${it.id}" },
                        contentType = { "movie_card" }
                    ) { item ->
                        MovieCard(
                            media = item,
                            onClick = { onMediaClick(item) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // Trending Now Section with Genre-Based Filtering UI
        if (trending.isNotEmpty()) {
            TrendingGenreSection(
                filteredTrending = filteredTrending,
                availableGenres = availableGenres,
                selectedGenre = selectedHomeGenre,
                countSummary = CatalogBatchPolicy.formatCompactHeaderTotal(
                    shownCount = filteredTrending.size,
                    totalAvailable = if (selectedHomeGenre.equals("All", ignoreCase = true)) {
                        maxOf(trendingTotalResults, trending.size)
                    } else {
                        filteredTrending.size
                    }
                ),
                onGenreSelect = onHomeGenreSelect,
                onMediaClick = onMediaClick,
                onSeeAllClick = { onSeeAllClick("Trending") },
                onBrowseGenreInExplore = { onBrowseGenreInExplore(selectedHomeGenre) }
            )
        }

        // Trending Movies Row
        if (trendingMovies.isNotEmpty()) {
            MediaSectionRow(
                title = "Trending Movies",
                countSummary = CatalogBatchPolicy.formatCompactHeaderTotal(
                    shownCount = trendingMovies.size,
                    totalAvailable = maxOf(trendingMoviesTotalResults, trendingMovies.size)
                ),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconColor = colors.softRose,
                onSeeAllClick = { onSeeAllClick("TrendingMovies") }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = trendingMovies,
                        key = { "tm_${it.id}" },
                        contentType = { "movie_card" }
                    ) { item ->
                        MovieCard(
                            media = item,
                            onClick = { onMediaClick(item) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // Trending TV Shows Row
        if (trendingTv.isNotEmpty()) {
            MediaSectionRow(
                title = "Trending TV Shows",
                countSummary = CatalogBatchPolicy.formatCompactHeaderTotal(
                    shownCount = trendingTv.size,
                    totalAvailable = maxOf(trendingTvTotalResults, trendingTv.size)
                ),
                icon = Icons.Default.Tv,
                iconColor = colors.info,
                onSeeAllClick = { onSeeAllClick("TrendingTv") }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = trendingTv,
                        key = { "ttv_${it.id}" },
                        contentType = { "movie_card" }
                    ) { item ->
                        MovieCard(
                            media = item,
                            onClick = { onMediaClick(item) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // Smart Recommendations: Because You Watched
        if (becauseYouWatchedItems.isNotEmpty()) {
            val becauseTitle = if (becauseYouWatchedTitle.isNotBlank()) {
                "Because You Watched $becauseYouWatchedTitle"
            } else {
                "Critically Acclaimed Picks"
            }
            MediaSectionRow(
                title = becauseTitle,
                icon = Icons.Default.ThumbUp,
                iconColor = colors.gold,
                onSeeAllClick = { onSeeAllClick("TopRatedMovies") }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = becauseYouWatchedItems,
                        key = { "bcw_${it.id}" },
                        contentType = { "movie_card" }
                    ) { item ->
                        MovieCard(
                            media = item,
                            onClick = { onMediaClick(item) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // Smart Recommendations: Popular In Your Genre
        if (popularInGenreItems.isNotEmpty()) {
            MediaSectionRow(
                title = "Popular in $popularInGenreName",
                icon = Icons.Default.MovieFilter,
                iconColor = colors.softRose,
                onSeeAllClick = { onBrowseGenreInExplore(popularInGenreName) }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = popularInGenreItems,
                        key = { "pig_${it.id}" },
                        contentType = { "movie_card" }
                    ) { item ->
                        MovieCard(
                            media = item,
                            onClick = { onMediaClick(item) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // Custom Cinetra / Admin Added Titles
        if (customMedia.isNotEmpty()) {
            MediaSectionRow(
                title = "Cinetra Specials & Admin Picks",
                countSummary = CatalogBatchPolicy.formatCount(customMedia.size),
                icon = Icons.Default.Star,
                iconColor = colors.gold,
                onSeeAllClick = { onSeeAllClick("Specials") }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = customMedia,
                        key = { it.id },
                        contentType = { "movie_card" }
                    ) { item ->
                        MovieCard(
                            media = item,
                            onClick = { onMediaClick(item) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // Popular Movies Row
        if (popularMovies.isNotEmpty()) {
            MediaSectionRow(
                title = "Popular Movies",
                countSummary = CatalogBatchPolicy.formatCompactHeaderTotal(
                    shownCount = popularMovies.size,
                    totalAvailable = maxOf(popularMoviesTotalResults, popularMovies.size)
                ),
                icon = Icons.Default.Movie,
                iconColor = colors.info,
                onSeeAllClick = { onSeeAllClick("Movies") }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = popularMovies,
                        key = { it.id },
                        contentType = { "movie_card" }
                    ) { item ->
                        MovieCard(
                            media = item,
                            onClick = { onMediaClick(item) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // Upcoming Movies & TV Series Row (Strict Future Releases + High Popularity)
        if (upcomingMovies.isNotEmpty()) {
            MediaSectionRow(
                title = "Upcoming Movies & Series",
                countSummary = CatalogBatchPolicy.formatCompactHeaderTotal(
                    shownCount = upcomingMovies.size,
                    totalAvailable = maxOf(upcomingMoviesTotalResults, upcomingMovies.size)
                ),
                icon = Icons.Default.Event,
                iconColor = colors.gold,
                onSeeAllClick = { onSeeAllClick("UpcomingMovies") }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("home_upcoming_row")
                ) {
                    items(
                        items = upcomingMovies,
                        key = { "upc_${it.id}" },
                        contentType = { "movie_card" }
                    ) { item ->
                        MovieCard(
                            media = item,
                            onClick = { onMediaClick(item) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // Browse by Language Section directly below Upcoming
        LanguagesBrowseSection(
            languages = TmdbFilterCatalog.browseLanguages,
            onLanguageClick = onLanguageClick
        )

        // Top Rated Movies Row
        if (topRatedMovies.isNotEmpty()) {
            MediaSectionRow(
                title = "Top Rated Movies",
                countSummary = CatalogBatchPolicy.formatCompactHeaderTotal(
                    shownCount = topRatedMovies.size,
                    totalAvailable = maxOf(topRatedMoviesTotalResults, topRatedMovies.size)
                ),
                icon = Icons.Default.Star,
                iconColor = colors.gold,
                onSeeAllClick = { onSeeAllClick("TopRatedMovies") }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = topRatedMovies,
                        key = { "trm_${it.id}" },
                        contentType = { "movie_card" }
                    ) { item ->
                        MovieCard(
                            media = item,
                            onClick = { onMediaClick(item) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        // Top-Rated TV Series Row
        if (topRatedTv.isNotEmpty()) {
            MediaSectionRow(
                title = "Top-Rated TV Series",
                countSummary = CatalogBatchPolicy.formatCompactHeaderTotal(
                    shownCount = topRatedTv.size,
                    totalAvailable = maxOf(topRatedTvTotalResults, topRatedTv.size)
                ),
                icon = Icons.Default.Tv,
                iconColor = colors.softRose,
                onSeeAllClick = { onSeeAllClick("TV") }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = topRatedTv,
                        key = { it.id },
                        contentType = { "movie_card" }
                    ) { item ->
                        MovieCard(
                            media = item,
                            onClick = { onMediaClick(item) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
        }
    }
}

@Composable
private fun TrendingGenreSection(
    filteredTrending: List<MediaItem>,
    availableGenres: List<String>,
    selectedGenre: String,
    countSummary: String?,
    onGenreSelect: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onSeeAllClick: () -> Unit,
    onBrowseGenreInExplore: () -> Unit
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    val isFiltered = !selectedGenre.equals("All", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("home_trending_section")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = colors.softRose,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.trending_now_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = colors.textPrimary
                )
                if (!countSummary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = countSummary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                }

                if (isFiltered) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(gradients.softBadge)
                            .border(1.dp, colors.softRose.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("home_trending_active_badge")
                    ) {
                        Text(
                            text = "$selectedGenre · ${filteredTrending.size}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isFiltered) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(50))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, RoundedCornerShape(50))
                            .clickable { onGenreSelect("All") }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("home_trending_reset_genre")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.genre_filter_reset),
                            tint = colors.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = stringResource(id = R.string.genre_filter_reset),
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onSeeAllClick)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("home_trending_see_all")
                ) {
                    Text(
                        text = stringResource(id = R.string.genre_filter_see_all),
                        color = colors.gold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = colors.gold,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        GenreFilterBar(
            genres = availableGenres,
            selectedGenre = selectedGenre,
            onGenreSelect = onGenreSelect
        )

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedContent(
            targetState = selectedGenre to filteredTrending,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
            },
            label = "trending_genre_transition"
        ) { (activeGenre, itemsForGenre) ->
            if (itemsForGenre.isEmpty()) {
                TrendingGenreEmptyState(
                    genre = activeGenre,
                    onResetGenre = { onGenreSelect("All") },
                    onBrowseExplore = onBrowseGenreInExplore
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("home_trending_list")
                ) {
                    items(
                        items = itemsForGenre,
                        key = { it.id },
                        contentType = { "movie_card" }
                    ) { item ->
                        MovieCard(
                            media = item,
                            onClick = { onMediaClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingGenreEmptyState(
    genre: String,
    onResetGenre: () -> Unit,
    onBrowseExplore: () -> Unit
) {
    val colors = CineTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(20.dp)
            .testTag("home_trending_empty_state")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.MovieFilter,
                contentDescription = null,
                tint = colors.gold,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(id = R.string.genre_empty_title, genre),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.genre_empty_subtitle, genre),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onResetGenre,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("home_empty_reset_button")
                ) {
                    Text(
                        text = stringResource(id = R.string.genre_show_all_trending),
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                CineGradientButton(
                    onClick = onBrowseExplore,
                    shape = RoundedCornerShape(50),
                    minHeight = 40.dp,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("home_empty_explore_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.genre_browse_in_explore, genre),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaSectionRow(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onSeeAllClick: () -> Unit,
    countSummary: String? = null,
    content: @Composable () -> Unit
) {
    val colors = CineTheme.colors

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = colors.textPrimary
                )
                if (!countSummary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = countSummary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onSeeAllClick)
                    .padding(4.dp)
            ) {
                Text(
                    text = "See All",
                    color = colors.gold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.gold,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun LanguagesBrowseSection(
    languages: List<BrowseLanguageOption>,
    onLanguageClick: (code: String, name: String) -> Unit
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("home_languages_section")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = colors.softRose,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Browse by Language",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${languages.size} languages",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.testTag("home_languages_row")
        ) {
            items(
                items = languages,
                key = { "lang_${it.code}" },
                contentType = { "language_card" }
            ) { lang ->
                val langInteraction = remember { MutableInteractionSource() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .widthIn(min = 148.dp)
                        .minimumInteractiveComponentSize()
                        .cinePressScale(interactionSource = langInteraction)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = langInteraction,
                            indication = ripple()
                        ) { onLanguageClick(lang.code, lang.name) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("home_language_card_${lang.code}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(gradients.softBadge)
                            .border(1.dp, colors.softRose.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lang.badgeCode,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = lang.name,
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = lang.nativeName,
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Browse ${lang.name}",
                        tint = colors.gold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
