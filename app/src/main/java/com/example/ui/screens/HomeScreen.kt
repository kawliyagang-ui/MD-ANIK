package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import com.example.data.model.ContinueWatchingEntity
import com.example.data.model.ContinueWatchingPolicy
import com.example.data.model.MediaItem
import com.example.data.model.mergeWithCatalog
import com.example.data.model.toMediaItem
import com.example.ui.components.AnnouncementBanner
import com.example.ui.components.CineGradientButton
import com.example.ui.components.GenreFilterBar
import com.example.ui.components.HeroBanner
import com.example.ui.components.HeroBannerSkeleton
import com.example.ui.components.HomeCatalogSkeleton
import com.example.ui.components.HomeGenreCatalog
import com.example.ui.components.HorizontalMovieListSkeleton
import com.example.ui.components.MovieCard
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
    onResumeWatching: (MediaItem, Int, Int) -> Unit = { m, _, _ -> onWatchClick(m) }
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    val isCatalogEmpty = trending.isEmpty() && popularMovies.isEmpty() && topRatedTv.isEmpty()

    if (isLoading && isCatalogEmpty && continueWatching.isEmpty() && customMedia.isEmpty()) {
        HomeCatalogSkeleton(modifier = modifier)
        return
    }

    val heroItem = trending.firstOrNull() ?: popularMovies.firstOrNull()
    val availableGenres = remember(trending) { HomeGenreCatalog.availableGenres(trending) }
    val filteredTrending = remember(trending, selectedHomeGenre) {
        HomeGenreCatalog.filterByGenre(trending, selectedHomeGenre)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradients.appBackdrop)
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Banner (or skeleton while live TMDb data is loading)
        if (heroItem != null) {
            HeroBanner(
                media = heroItem,
                isInWatchlist = isItemInWatchlist(heroItem.id),
                onWatchClick = { onWatchClick(heroItem) },
                onWatchlistClick = { onWatchlistToggle(heroItem) },
                onTrailerClick = { onTrailerClick(heroItem) },
                onCardClick = { onMediaClick(heroItem) }
            )
        } else if (isLoading) {
            HeroBannerSkeleton()
        }

        // Active Admin Announcement Notices
        AnnouncementBanner(announcements = announcements)

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
            val catalogById = remember(trending, popularMovies, topRatedTv, customMedia) {
                (trending + popularMovies + topRatedTv + customMedia).associateBy { it.id }
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
                    items(continueWatching, key = { it.id }) { item ->
                        val media = item.toMediaItem().mergeWithCatalog(catalogById[item.id])
                        val (safeSeason, safeEp) = ContinueWatchingPolicy.normalizeSeasonEpisode(
                            mediaType = item.mediaType,
                            season = item.season,
                            episode = item.episode
                        )
                        val watchedLabel = ContinueWatchingPolicy.formatWatchedDuration(item.watchedSeconds)
                        val cardLabel = when {
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
                        MovieCard(
                            media = media,
                            onClick = { onResumeWatching(media, safeSeason, safeEp) },
                            progressPercent = item.progressPercent.takeIf { it > 0 },
                            episodeLabel = cardLabel
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

        // Trending Now Section with Genre-Based Filtering UI
        if (trending.isNotEmpty()) {
            TrendingGenreSection(
                filteredTrending = filteredTrending,
                availableGenres = availableGenres,
                selectedGenre = selectedHomeGenre,
                onGenreSelect = onHomeGenreSelect,
                onMediaClick = onMediaClick,
                onSeeAllClick = {
                    if (selectedHomeGenre.equals("All", ignoreCase = true)) {
                        onSeeAllClick("Trending")
                    } else {
                        onBrowseGenreInExplore(selectedHomeGenre)
                    }
                },
                onBrowseGenreInExplore = { onBrowseGenreInExplore(selectedHomeGenre) }
            )
        }

        // Custom Cinetra / Admin Added Titles
        if (customMedia.isNotEmpty()) {
            MediaSectionRow(
                title = "Cinetra Specials & Admin Picks",
                icon = Icons.Default.Star,
                iconColor = colors.gold,
                onSeeAllClick = { onSeeAllClick("Specials") }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(customMedia, key = { it.id }) { item ->
                        MovieCard(
                            media = item,
                            onClick = { onMediaClick(item) }
                        )
                    }
                }
            }
        }

        // Popular Movies Row
        if (popularMovies.isNotEmpty()) {
            MediaSectionRow(
                title = "Popular Movies",
                icon = Icons.Default.Movie,
                iconColor = colors.info,
                onSeeAllClick = { onSeeAllClick("Movies") }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(popularMovies, key = { it.id }) { item ->
                        MovieCard(
                            media = item,
                            onClick = { onMediaClick(item) }
                        )
                    }
                }
            }
        }

        // Top-Rated TV Series Row
        if (topRatedTv.isNotEmpty()) {
            MediaSectionRow(
                title = "Top-Rated TV Series",
                icon = Icons.Default.Tv,
                iconColor = colors.softRose,
                onSeeAllClick = { onSeeAllClick("TV") }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(topRatedTv, key = { it.id }) { item ->
                        MovieCard(
                            media = item,
                            onClick = { onMediaClick(item) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun TrendingGenreSection(
    filteredTrending: List<MediaItem>,
    availableGenres: List<String>,
    selectedGenre: String,
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
                    items(itemsForGenre, key = { it.id }) { item ->
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
