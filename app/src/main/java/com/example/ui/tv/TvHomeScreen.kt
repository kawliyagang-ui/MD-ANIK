package com.example.ui.tv

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.data.model.ContinueWatchingEntity
import com.example.data.model.ContinueWatchingPolicy
import com.example.data.model.MediaItem
import com.example.data.model.WatchlistEntity
import com.example.data.model.mergeWithCatalog
import com.example.data.model.toMediaItem
import com.example.ui.components.HomeGenreCatalog
import com.example.ui.components.rememberCineImageRequest
import com.example.ui.components.rememberDarkImagePainter
import com.example.ui.theme.CineTheme
import kotlinx.coroutines.delay
import java.util.Locale

private data class TvShelfData(
    val key: String,
    val title: String,
    val items: List<MediaItem>,
    val isTopTen: Boolean = false,
    val showGenresBefore: Boolean = false
)

@Composable
fun TvHomeScreen(
    trending: List<MediaItem>,
    trendingMovies: List<MediaItem>,
    trendingTv: List<MediaItem>,
    newReleases: List<MediaItem>,
    popularMovies: List<MediaItem>,
    upcomingMovies: List<MediaItem>,
    topRatedMovies: List<MediaItem>,
    topRatedTv: List<MediaItem>,
    customMedia: List<MediaItem>,
    continueWatching: List<ContinueWatchingEntity>,
    watchlist: List<WatchlistEntity>,
    recommendedForYou: List<MediaItem>,
    becauseYouWatchedTitle: String,
    becauseYouWatchedItems: List<MediaItem>,
    selectedHomeGenre: String,
    isLoading: Boolean,
    errorMessage: String?,
    isItemInWatchlist: (String) -> Boolean,
    focusMemory: TvHomeFocusMemory,
    onHomeGenreSelect: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onWatchClick: (MediaItem) -> Unit,
    onResumeWatching: (MediaItem, Int, Int) -> Unit,
    onWatchlistToggle: (MediaItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients

    val allCatalogUnique = remember(
        trending, trendingMovies, trendingTv, newReleases, popularMovies, upcomingMovies, topRatedMovies, topRatedTv, customMedia
    ) {
        (trending + trendingMovies + trendingTv + newReleases + popularMovies + upcomingMovies + topRatedMovies + topRatedTv + customMedia)
            .distinctBy { it.id }
    }
    val catalogById = remember(allCatalogUnique) { allCatalogUnique.associateBy { it.id } }

    val heroItems = remember(trending, popularMovies, customMedia) {
        (trending.ifEmpty { popularMovies }.ifEmpty { customMedia }).distinctBy { it.id }.take(5)
    }
    var heroIndex by remember(heroItems) { mutableIntStateOf(0) }
    val activeHero = heroItems.getOrNull(heroIndex.coerceIn(0, (heroItems.size - 1).coerceAtLeast(0)))

    val availableGenres = remember(trending) { HomeGenreCatalog.availableGenres(trending) }
    val filteredTrending = remember(trending, selectedHomeGenre) {
        HomeGenreCatalog.filterByGenre(trending, selectedHomeGenre)
    }
    val topTenMovies = remember(trendingMovies, popularMovies) {
        (trendingMovies.ifEmpty { popularMovies }).take(10)
    }
    val topTenTv = remember(trendingTv, topRatedTv) {
        (trendingTv.ifEmpty { topRatedTv }).take(10)
    }
    val watchlistMedia = remember(watchlist, catalogById) {
        watchlist.map { it.toMediaItem().mergeWithCatalog(catalogById[it.id]) }
    }
    val continueWatchingEntries = remember(continueWatching, catalogById) {
        continueWatching.map { cw ->
            val media = cw.toMediaItem().mergeWithCatalog(catalogById[cw.id])
            val (safeS, safeE) = ContinueWatchingPolicy.normalizeSeasonEpisode(media.mediaType, cw.season, cw.episode)
            val watchedLabel = ContinueWatchingPolicy.formatWatchedDuration(cw.watchedSeconds)
            val epLabel = when {
                media.mediaType == "tv" && cw.progressPercent > 0 -> "S$safeS:E$safeE • ${cw.progressPercent}%"
                media.mediaType == "tv" && watchedLabel.isNotBlank() -> "S$safeS:E$safeE • $watchedLabel"
                media.mediaType == "tv" -> "S$safeS:E$safeE"
                cw.progressPercent > 0 -> "${cw.progressPercent}% watched"
                watchedLabel.isNotBlank() -> "$watchedLabel watched"
                else -> "Resume"
            }
            Triple(media, cw, epLabel)
        }
    }

    val shelves = remember(
        newReleases, topTenMovies, topTenTv, recommendedForYou, filteredTrending, trending,
        trendingMovies, trendingTv, becauseYouWatchedItems, becauseYouWatchedTitle,
        popularMovies, upcomingMovies, topRatedMovies, topRatedTv, watchlistMedia
    ) {
        buildList {
            if (newReleases.isNotEmpty()) add(TvShelfData("new_releases", "New Releases", newReleases))
            if (topTenMovies.isNotEmpty()) add(TvShelfData("top10_movies", "Top 10 Movies", topTenMovies, isTopTen = true))
            if (topTenTv.isNotEmpty()) add(TvShelfData("top10_tv", "Top 10 TV Series", topTenTv, isTopTen = true))
            if (recommendedForYou.isNotEmpty()) add(TvShelfData("recommended", "Recommended For You", recommendedForYou))
            if (trending.isNotEmpty()) {
                add(
                    TvShelfData(
                        key = "trending",
                        title = if (selectedHomeGenre.equals("All", ignoreCase = true)) "Trending Now" else "Trending • $selectedHomeGenre",
                        items = filteredTrending,
                        showGenresBefore = true
                    )
                )
            }
            if (trendingMovies.isNotEmpty()) add(TvShelfData("trending_movies", "Trending Movies", trendingMovies))
            if (trendingTv.isNotEmpty()) add(TvShelfData("trending_tv", "Trending TV Shows", trendingTv))
            if (becauseYouWatchedItems.isNotEmpty()) {
                val label = if (becauseYouWatchedTitle.isNotBlank()) "Because You Watched $becauseYouWatchedTitle" else "Critically Acclaimed"
                add(TvShelfData("because_watched", label, becauseYouWatchedItems))
            }
            if (popularMovies.isNotEmpty()) add(TvShelfData("popular_movies", "Popular Movies", popularMovies))
            if (upcomingMovies.isNotEmpty()) add(TvShelfData("upcoming", "Upcoming Movies & Series", upcomingMovies))
            if (topRatedMovies.isNotEmpty()) add(TvShelfData("top_rated_movies", "Top Rated Movies", topRatedMovies))
            if (topRatedTv.isNotEmpty()) add(TvShelfData("top_rated_tv", "Top Rated TV Shows", topRatedTv))
            if (watchlistMedia.isNotEmpty()) add(TvShelfData("my_list", "My List", watchlistMedia))
        }
    }

    val columnListState = rememberLazyListState()
    val rowListStates = remember { mutableMapOf<String, LazyListState>() }
    fun getRowState(key: String): LazyListState = rowListStates.getOrPut(key) { LazyListState() }

    val itemFocusRequesters = remember { mutableMapOf<Pair<String, String>, FocusRequester>() }
    fun getItemRequester(rowKey: String, itemId: String): FocusRequester =
        itemFocusRequesters.getOrPut(rowKey to itemId) { FocusRequester() }

    val heroPlayFocusRequester = remember { FocusRequester() }
    val retryFocusRequester = remember { FocusRequester() }

    val isCatalogEmpty = allCatalogUnique.isEmpty() && continueWatching.isEmpty()
    if (isLoading && isCatalogEmpty) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradients.appBackdrop),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(color = colors.softRose)
                Text("Loading Cinetra catalog...", color = colors.textSecondary, fontSize = 15.sp)
            }
        }
        return
    }

    if (isCatalogEmpty) {
        LaunchedEffect(Unit) {
            runCatching { retryFocusRequester.requestFocus() }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradients.appBackdrop)
                .padding(horizontal = TvOverscanHorizontal, vertical = TvOverscanVertical),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = colors.softRose,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Unable to Load Catalog",
                    color = colors.textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = errorMessage ?: "Check your TV internet connection and retry.",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                TvActionButton(
                    text = "Retry Connection",
                    icon = Icons.Default.Refresh,
                    onClick = onRetry,
                    modifier = Modifier
                        .focusRequester(retryFocusRequester)
                        .testTag("tv_home_retry_button")
                )
            }
        }
        return
    }

    // Build ordered vertical item keys so focus restoration can scroll to the exact shelf index
    val orderedRowVerticalIndices = remember(activeHero, continueWatchingEntries, shelves) {
        val map = mutableMapOf<String, Int>()
        var cursor = 0
        if (activeHero != null) {
            map["hero"] = cursor
            cursor++
        }
        if (continueWatchingEntries.isNotEmpty()) {
            map["continue_watching"] = cursor
            cursor++
        }
        shelves.forEach { shelf ->
            if (shelf.showGenresBefore) {
                cursor++ // genre pills item precedes the trending shelf
            }
            map[shelf.key] = cursor
            cursor++
        }
        map
    }

    val availableRowsMap = remember(activeHero, continueWatchingEntries, shelves) {
        buildMap {
            if (activeHero != null) put("hero", listOf(activeHero.id))
            if (continueWatchingEntries.isNotEmpty()) {
                put("continue_watching", continueWatchingEntries.map { it.first.id })
            }
            shelves.forEach { shelf ->
                put(shelf.key, shelf.items.map { it.id })
            }
        }
    }

    // Restore focus to previously opened card when returning from Details, or focus Hero Play initially
    LaunchedEffect(availableRowsMap) {
        if (focusMemory.pendingRestore) {
            val target = focusMemory.consumeRestoreTarget(availableRowsMap)
            if (target != null) {
                if (target.rowKey == "hero") {
                    columnListState.scrollToItem(0)
                    delay(40L)
                    runCatching { heroPlayFocusRequester.requestFocus() }
                } else {
                    val vIndex = orderedRowVerticalIndices[target.rowKey]
                    if (vIndex != null) {
                        columnListState.scrollToItem(vIndex)
                        getRowState(target.rowKey).scrollToItem(target.itemIndex)
                        delay(55L)
                        runCatching { getItemRequester(target.rowKey, target.itemId).requestFocus() }
                    }
                }
                return@LaunchedEffect
            }
        }
        if (focusMemory.lastTarget == null && activeHero != null) {
            delay(40L)
            runCatching { heroPlayFocusRequester.requestFocus() }
        }
    }

    LazyColumn(
        state = columnListState,
        modifier = modifier
            .fillMaxSize()
            .background(gradients.appBackdrop)
            .testTag("tv_home_screen"),
        contentPadding = PaddingValues(bottom = TvOverscanVertical + 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Featured Hero Banner
        if (activeHero != null) {
            item(key = "tv_hero_banner", contentType = "tv_hero") {
                TvHeroBanner(
                    media = activeHero,
                    heroIndex = heroIndex,
                    heroCount = heroItems.size,
                    isInWatchlist = isItemInWatchlist(activeHero.id),
                    playButtonFocusRequester = heroPlayFocusRequester,
                    onPlayClick = {
                        focusMemory.recordSelection("hero", activeHero.id, 0)
                        onWatchClick(activeHero)
                    },
                    onDetailsClick = {
                        focusMemory.recordSelection("hero", activeHero.id, 0)
                        onMediaClick(activeHero)
                    },
                    onToggleWatchlist = { onWatchlistToggle(activeHero) },
                    onSelectHeroIndex = { heroIndex = it }
                )
            }
        }

        // Continue Watching Shelf
        if (continueWatchingEntries.isNotEmpty()) {
            item(key = "tv_shelf_continue_watching", contentType = "tv_shelf") {
                TvShelfSection(
                    title = "Continue Watching",
                    rowKey = "continue_watching",
                    rowState = getRowState("continue_watching")
                ) {
                    itemsIndexed(
                        items = continueWatchingEntries,
                        key = { _, triple -> "cw_${triple.first.id}" },
                        contentType = { _, _ -> "tv_poster_card" }
                    ) { idx, (media, cw, epLabel) ->
                        TvPosterCard(
                            media = media,
                            focusRequester = getItemRequester("continue_watching", media.id),
                            progressPercent = cw.progressPercent.takeIf { it > 0 },
                            episodeLabel = epLabel,
                            onFocused = { focusMemory.recordSelection("continue_watching", media.id, idx) },
                            onClick = {
                                focusMemory.recordSelection("continue_watching", media.id, idx)
                                onResumeWatching(media, cw.season, cw.episode)
                            }
                        )
                    }
                }
            }
        }

        // Catalog Shelves
        shelves.forEach { shelf ->
            if (shelf.showGenresBefore) {
                item(key = "tv_genre_filter_row", contentType = "tv_genre_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = TvOverscanHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.testTag("tv_home_genre_row")
                    ) {
                        itemsIndexed(
                            items = availableGenres,
                            key = { _, genre -> "genre_$genre" }
                        ) { _, genre ->
                            TvFilterPill(
                                text = genre,
                                selected = selectedHomeGenre.equals(genre, ignoreCase = true),
                                onClick = { onHomeGenreSelect(genre) },
                                modifier = Modifier.testTag("tv_genre_pill_$genre")
                            )
                        }
                    }
                }
            }

            item(key = "tv_shelf_${shelf.key}", contentType = "tv_shelf") {
                TvShelfSection(
                    title = shelf.title,
                    rowKey = shelf.key,
                    rowState = getRowState(shelf.key)
                ) {
                    itemsIndexed(
                        items = shelf.items,
                        key = { _, item -> "${shelf.key}_${item.id}" },
                        contentType = { _, _ -> "tv_poster_card" }
                    ) { idx, item ->
                        TvPosterCard(
                            media = item,
                            rank = if (shelf.isTopTen) idx + 1 else null,
                            focusRequester = getItemRequester(shelf.key, item.id),
                            onFocused = { focusMemory.recordSelection(shelf.key, item.id, idx) },
                            onClick = {
                                focusMemory.recordSelection(shelf.key, item.id, idx)
                                onMediaClick(item)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvHeroBanner(
    media: MediaItem,
    heroIndex: Int,
    heroCount: Int,
    isInWatchlist: Boolean,
    playButtonFocusRequester: FocusRequester,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onSelectHeroIndex: (Int) -> Unit
) {
    val colors = CineTheme.colors
    val darkPainter = rememberDarkImagePainter()
    val backdropRequest = rememberCineImageRequest(
        url = media.fullBackdropUrl.ifBlank { media.fullPosterUrl },
        widthDp = 960.dp,
        heightDp = 360.dp
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TvOverscanHorizontal)
            .height(340.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .testTag("tv_hero_banner")
    ) {
        AsyncImage(
            model = backdropRequest,
            contentDescription = media.title,
            placeholder = darkPainter,
            error = darkPainter,
            fallback = darkPainter,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Left & bottom dark cinematic scrims for 10-foot legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            colors.background.copy(alpha = 0.96f),
                            colors.background.copy(alpha = 0.72f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            colors.background.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.68f)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (media.mediaType.equals("tv", ignoreCase = true)) "SERIES SPOTLIGHT" else "FEATURED FILM",
                    color = colors.softRose,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
                if (media.voteAverage > 0.0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = colors.gold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", media.voteAverage),
                            color = colors.gold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (media.releaseDate.length >= 4) {
                    Text(
                        text = media.releaseDate.take(4),
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
                if (media.certification.isNotBlank()) {
                    Text(
                        text = media.certification,
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .border(1.dp, colors.border, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = media.title,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 36.sp
            )

            if (media.overview.isNotBlank()) {
                Text(
                    text = media.overview,
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TvActionButton(
                    text = "Play",
                    icon = Icons.Default.PlayArrow,
                    onClick = onPlayClick,
                    modifier = Modifier
                        .focusRequester(playButtonFocusRequester)
                        .testTag("tv_hero_play_button")
                )
                TvOutlinedActionButton(
                    text = "More Info",
                    icon = Icons.Default.Info,
                    onClick = onDetailsClick,
                    modifier = Modifier.testTag("tv_hero_details_button")
                )
                TvOutlinedActionButton(
                    text = if (isInWatchlist) "In My List" else "My List",
                    icon = if (isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    highlighted = isInWatchlist,
                    onClick = onToggleWatchlist,
                    modifier = Modifier.testTag("tv_hero_watchlist_button")
                )
            }
        }

        if (heroCount > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until heroCount) {
                    TvFilterPill(
                        text = "${i + 1}",
                        selected = i == heroIndex,
                        onClick = { onSelectHeroIndex(i) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvShelfSection(
    title: String,
    rowKey: String,
    rowState: LazyListState,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    val colors = CineTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = TvOverscanHorizontal)
                .testTag("tv_shelf_title_$rowKey")
        )
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = TvOverscanHorizontal, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.testTag("tv_shelf_row_$rowKey"),
            content = content
        )
    }
}
