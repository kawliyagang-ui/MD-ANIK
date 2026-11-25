package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CatalogBatchPolicy
import com.example.data.model.MediaItem
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.example.data.model.DiscoverFilters
import com.example.data.model.TmdbSearchHistoryEntity
import com.example.ui.components.AdvancedFilterSheet
import com.example.ui.components.BatchSizeChip
import com.example.ui.components.CatalogCountChip
import com.example.ui.components.CineEase
import com.example.ui.components.CineGradientButton
import com.example.ui.components.LoadMoreBatchFooter
import com.example.ui.components.MovieCard
import com.example.ui.components.SearchResultsSkeletonGrid
import com.example.ui.components.cinePressScale
import com.example.ui.theme.CineTheme

@Composable
fun ExploreScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    selectedGenre: String,
    onGenreSelect: (String) -> Unit,
    sortBy: String,
    onSortSelect: (String) -> Unit,
    allTitles: List<MediaItem>,
    searchResults: List<MediaItem>,
    isSearching: Boolean,
    onMediaClick: (MediaItem) -> Unit,
    searchHistory: List<TmdbSearchHistoryEntity> = emptyList(),
    onDeleteSearchHistory: (String) -> Unit = {},
    onClearSearchHistory: () -> Unit = {},
    searchErrorMessage: String? = null,
    onRetrySearch: () -> Unit = {},
    onResetFilters: () -> Unit = {},
    isLoadingCatalog: Boolean = false,
    onOpenSearch: () -> Unit = {},
    totalAvailableCount: Int = allTitles.size,
    batchSize: Int = CatalogBatchPolicy.DEFAULT_BATCH_SIZE,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onBatchSizeChange: (Int) -> Unit = {},
    onLoadMore: () -> Unit = {},
    discoverFilters: DiscoverFilters = DiscoverFilters(),
    discoverResults: List<MediaItem> = emptyList(),
    discoverTotalResults: Int = 0,
    isLoadingDiscover: Boolean = false,
    isLoadingMoreDiscover: Boolean = false,
    hasMoreDiscover: Boolean = false,
    onApplyDiscoverFilters: (DiscoverFilters) -> Unit = {},
    onLoadMoreDiscover: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    var showFilterSheet by remember { mutableStateOf(false) }
    val genres = remember {
        listOf(
            "All", "Action", "Adventure", "Animation", "Comedy", "Crime",
            "Drama", "Fantasy", "Horror", "Mystery", "Sci-Fi", "Thriller"
        )
    }

    val categories = remember {
        listOf("All", "movie", "tv")
    }

    // Determine displayed items: Search > TMDB Discover API (when advanced criteria active) > Loaded Catalog
    val isSearchActive = searchQuery.isNotBlank()
    val useDiscoverMode = !isSearchActive && (discoverFilters.hasDiscoverApiCriteria() || discoverResults.isNotEmpty())
    val baseList = when {
        isSearchActive -> searchResults
        useDiscoverMode -> discoverResults
        else -> allTitles
    }

    val filteredList = remember(baseList, discoverFilters, selectedCategory, selectedGenre, sortBy, useDiscoverMode) {
        if (useDiscoverMode) {
            discoverFilters.sortItems(baseList)
        } else {
            var list = baseList
            if (selectedCategory != "All") {
                list = list.filter { it.mediaType.equals(selectedCategory, ignoreCase = true) }
            }
            if (selectedGenre != "All") {
                list = list.filter { item ->
                    item.genres.any { it.contains(selectedGenre, ignoreCase = true) }
                }
            }
            discoverFilters.sortItems(list.filter { discoverFilters.matchesItem(it) })
        }
    }

    val hasActiveFilterOrSearch = isSearchActive || discoverFilters.hasAdvancedFilters() ||
        !selectedCategory.equals("All", ignoreCase = true) ||
        !selectedGenre.equals("All", ignoreCase = true)

    val effectiveHasMore = when {
        useDiscoverMode -> hasMoreDiscover
        else -> hasMore
    }
    val effectiveIsLoadingMore = when {
        useDiscoverMode -> isLoadingMoreDiscover
        else -> isLoadingMore
    }
    val effectiveOnLoadMore: () -> Unit = when {
        useDiscoverMode -> onLoadMoreDiscover
        else -> onLoadMore
    }

    val displayTotal = remember(
        searchQuery,
        selectedCategory,
        selectedGenre,
        totalAvailableCount,
        filteredList.size,
        useDiscoverMode,
        discoverTotalResults
    ) {
        if (useDiscoverMode) {
            maxOf(discoverTotalResults, filteredList.size)
        } else {
            CatalogBatchPolicy.resolveExploreFilteredTotal(
                searchQuery = searchQuery,
                selectedCategory = selectedCategory,
                selectedGenre = selectedGenre,
                catalogTotalAvailable = totalAvailableCount,
                searchTotalResults = totalAvailableCount,
                filteredShownCount = filteredList.size
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradients.appBackdrop)
    ) {
        // Content Feed Header with Live Database Total Chip & Search Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("explore_feed_header"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isSearchActive -> "Search Results"
                        hasActiveFilterOrSearch -> "Filtered Feed"
                        else -> "Catalog Feed"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = colors.textPrimary
                )
                CatalogCountChip(
                    totalAvailable = displayTotal,
                    isFiltered = hasActiveFilterOrSearch,
                    isLoading = isSearching || (isLoadingCatalog && filteredList.isEmpty()),
                    testTag = "explore_catalog_total_chip"
                )
            }
            val searchBarInteraction = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .cinePressScale(searchBarInteraction)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = searchBarInteraction,
                        indication = ripple(bounded = true),
                        onClick = onOpenSearch
                    )
                    .padding(horizontal = 14.dp)
                    .testTag("search_input_field"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colors.softRose
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (searchQuery.isNotBlank()) searchQuery else "Search movies, TV shows, anime...",
                    color = if (searchQuery.isNotBlank()) colors.textPrimary else colors.textMuted,
                    fontSize = 14.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.testTag("clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = colors.textSecondary
                        )
                    }
                }
            }

            // Search History (Last 5 movie searches from Room DB)
            if (searchHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Search History",
                            tint = colors.gold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Recent Searches (${searchHistory.size}/5):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.gold
                        )
                    }
                    Text(
                        text = "Clear All",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .clickable { onClearSearchHistory() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("explore_clear_search_history")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = searchHistory,
                        key = { it.query },
                        contentType = { "recent_search_chip" }
                    ) { item ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                                .clickable {
                                    onSearchQueryChange(item.query)
                                    onOpenSearch()
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("explore_recent_chip_${item.query}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = item.query,
                                    fontSize = 12.sp,
                                    color = colors.textPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onDeleteSearchHistory(item.query) }
                                        .testTag("explore_delete_chip_${item.query}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove ${item.query}",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Pills (All / Movies / TV Series)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val label = when (cat) {
                        "movie" -> "Movies"
                        "tv" -> "TV Series"
                        else -> "All Types"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelect(cat) },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accentMid,
                            selectedLabelColor = Color.White,
                            containerColor = colors.surfaceElevated,
                            labelColor = colors.textPrimary
                        ),
                        modifier = Modifier.testTag("filter_cat_$cat")
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Advanced Filter Panel button
                val advCount = discoverFilters.activeFilterCount()
                FilterChip(
                    selected = advCount > 0,
                    onClick = { showFilterSheet = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Advanced Filters",
                            tint = if (advCount > 0) Color.White else colors.softRose,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (advCount > 0) "Filters ($advCount)" else "Filters",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.accentMid,
                        selectedLabelColor = Color.White,
                        containerColor = colors.surfaceElevated,
                        labelColor = colors.textPrimary
                    ),
                    modifier = Modifier.testTag("explore_advanced_filters_button")
                )

                // Sort toggle (Popular / Rating / Newest)
                FilterChip(
                    selected = sortBy != "popular",
                    onClick = {
                        val next = when (sortBy) {
                            "popular" -> "rating"
                            "rating" -> "newest"
                            else -> "popular"
                        }
                        onSortSelect(next)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = colors.gold,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = {
                        Text(
                            text = when (sortBy) {
                                "rating" -> "★ Rating"
                                "newest" -> "Newest"
                                else -> "Popular"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.surfaceElevated,
                        selectedLabelColor = colors.gold,
                        containerColor = colors.surfaceElevated,
                        labelColor = colors.textPrimary
                    ),
                    modifier = Modifier.testTag("sort_filter_chip")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Genre & Batch Size Horizontal Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BatchSizeChip(
                    batchSize = batchSize,
                    onToggleBatchSize = onBatchSizeChange,
                    testTag = "explore_batch_size_chip"
                )
                if (discoverFilters.releaseYear != null) {
                    FilterChip(
                        selected = true,
                        onClick = { onApplyDiscoverFilters(discoverFilters.copy(releaseYear = null)) },
                        label = { Text("Year: ${discoverFilters.releaseYear} ✕", fontSize = 11.sp) }
                    )
                }
                if (discoverFilters.minRating > 0.0) {
                    FilterChip(
                        selected = true,
                        onClick = { onApplyDiscoverFilters(discoverFilters.copy(minRating = 0.0)) },
                        label = { Text("${discoverFilters.minRating.toInt()}+ ★ ✕", fontSize = 11.sp) }
                    )
                }
                if (discoverFilters.originalLanguage.isNotBlank()) {
                    FilterChip(
                        selected = true,
                        onClick = { onApplyDiscoverFilters(discoverFilters.copy(originalLanguage = "")) },
                        label = { Text("Lang: ${discoverFilters.originalLanguage.uppercase()} ✕", fontSize = 11.sp) }
                    )
                }
                if (discoverFilters.originCountry.isNotBlank()) {
                    FilterChip(
                        selected = true,
                        onClick = { onApplyDiscoverFilters(discoverFilters.copy(originCountry = "")) },
                        label = { Text("Country: ${discoverFilters.originCountry} ✕", fontSize = 11.sp) }
                    )
                }
                genres.forEach { genre ->
                    val isSelected = selectedGenre == genre || genre in discoverFilters.selectedGenres
                    FilterChip(
                        selected = isSelected,
                        onClick = { onGenreSelect(genre) },
                        label = { Text(genre, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isSelected) colors.gold else colors.surfaceElevated,
                            selectedLabelColor = if (isSelected) Color.Black else colors.textPrimary,
                            containerColor = colors.surfaceElevated,
                            labelColor = colors.textPrimary
                        ),
                        modifier = Modifier.testTag("genre_chip_$genre")
                    )
                }
            }
        }

        // Search status & result count
        val showSkeletonGrid = (isSearching && filteredList.isEmpty()) ||
            (isLoadingDiscover && filteredList.isEmpty()) ||
            (isLoadingCatalog && searchQuery.isBlank() && filteredList.isEmpty())

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when {
                    isSearching -> "Searching TMDb..."
                    isLoadingCatalog && filteredList.isEmpty() -> "Loading catalog..."
                    else -> CatalogBatchPolicy.formatAvailabilitySummary(
                        shownCount = filteredList.size,
                        totalAvailable = displayTotal
                    )
                },
                fontSize = 12.sp,
                color = colors.textSecondary,
                fontWeight = FontWeight.Medium
            )

            if (isSearching || effectiveIsLoadingMore || isLoadingDiscover || (isLoadingCatalog && filteredList.isEmpty())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = colors.softRose,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isSearching -> "Fetching results"
                            isLoadingDiscover -> "Filtering catalog"
                            effectiveIsLoadingMore -> "Loading batch"
                            else -> "Syncing TMDb"
                        },
                        fontSize = 11.sp,
                        color = colors.gold
                    )
                }
            }
        }

        // Grid of results / Skeleton / Error / Empty state
        val exploreMode = when {
            showSkeletonGrid -> "skeleton"
            searchErrorMessage != null && filteredList.isEmpty() && !isSearching -> "error"
            filteredList.isEmpty() && !isSearching && !isLoadingDiscover -> "empty"
            else -> "grid"
        }

        AnimatedContent(
            targetState = exploreMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(220, easing = CineEase)) togetherWith
                    fadeOut(animationSpec = tween(160, easing = CineEase))
            },
            label = "explore_content_transition",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { mode ->
        when (mode) {
            "skeleton" -> {
            SearchResultsSkeletonGrid()
            }
            "error" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .testTag("explore_search_error_state"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = colors.softRose,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "TMDb Search Unavailable",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = searchErrorMessage.orEmpty(),
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CineGradientButton(
                        onClick = onRetrySearch,
                        shape = RoundedCornerShape(50),
                        minHeight = 48.dp,
                        modifier = Modifier.testTag("explore_retry_search_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Search", fontWeight = FontWeight.Bold)
                    }
                }
            }
            }
            "empty" -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .testTag("explore_empty_state"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) {
                            "No results found for \"$searchQuery\""
                        } else {
                            "No matching titles found"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) {
                            "Try another search query or reset your category and genre filters."
                        } else {
                            "Reset your active filters or search TMDb above to discover more movies and series."
                        },
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    if (hasActiveFilterOrSearch) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onResetFilters,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("explore_reset_filters_button")
                        ) {
                            Text(
                                text = "Reset Filters",
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            }
            else -> {
            val gridState = rememberLazyGridState()
            val shouldAutoLoadMore by remember(effectiveHasMore, effectiveIsLoadingMore, filteredList.size) {
                derivedStateOf {
                    val total = gridState.layoutInfo.totalItemsCount
                    val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                    effectiveHasMore && !effectiveIsLoadingMore && total > 8 && lastVisible >= total - 4
                }
            }
            LaunchedEffect(shouldAutoLoadMore) {
                if (shouldAutoLoadMore) {
                    effectiveOnLoadMore()
                }
            }

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 135.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = filteredList,
                    key = { it.id },
                    contentType = { "movie_card" }
                ) { item ->
                    MovieCard(
                        media = item,
                        onClick = { onMediaClick(item) },
                        cardWidth = 140.dp,
                        cardHeight = 205.dp,
                        modifier = Modifier.animateItem()
                    )
                }

                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "explore_load_more_footer",
                    contentType = "load_more_footer"
                ) {
                    LoadMoreBatchFooter(
                        shownCount = filteredList.size,
                        totalAvailable = displayTotal,
                        batchSize = batchSize,
                        hasMore = effectiveHasMore,
                        isLoadingMore = effectiveIsLoadingMore,
                        onLoadMore = effectiveOnLoadMore,
                        testTag = "explore_load_more_button"
                    )
                }
            }
            }
        }
        }
    }

    if (showFilterSheet) {
        AdvancedFilterSheet(
            currentFilters = discoverFilters,
            onApplyFilters = onApplyDiscoverFilters,
            onDismiss = { showFilterSheet = false }
        )
    }
}
