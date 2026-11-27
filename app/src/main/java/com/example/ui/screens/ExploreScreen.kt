package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CatalogBatchPolicy
import com.example.data.model.DiscoverFilters
import com.example.data.model.MediaItem
import com.example.data.model.TmdbSearchHistoryEntity
import com.example.ui.components.AdvancedFilterSheet
import com.example.ui.components.BatchSizeChip
import com.example.ui.components.CatalogCountChip
import com.example.ui.components.CategoryPill
import com.example.ui.components.CineEase
import com.example.ui.components.CinetraEmptyState
import com.example.ui.components.LoadMoreBatchFooter
import com.example.ui.components.MovieCard
import com.example.ui.components.RedButton
import com.example.ui.components.SearchResultsSkeletonGrid
import com.example.ui.components.cinePressScale
import com.example.ui.theme.CinetraTheme

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
    val colors = CinetraTheme.colors
    val bottomClearance = CinetraTheme.bottomBarClearance
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

    val effectiveHasMore = if (useDiscoverMode) hasMoreDiscover else hasMore
    val effectiveIsLoadingMore = if (useDiscoverMode) isLoadingMoreDiscover else isLoadingMore
    val effectiveOnLoadMore: () -> Unit = if (useDiscoverMode) onLoadMoreDiscover else onLoadMore

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
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .testTag("explore_feed_header"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isSearchActive -> "Search Results"
                        hasActiveFilterOrSearch -> "Filtered Feed"
                        else -> "Explore Catalog"
                    },
                    style = MaterialTheme.typography.titleMedium, // Bold 18.sp
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
                    .height(50.dp)
                    .cinePressScale(searchBarInteraction)
                    .clip(RoundedCornerShape(50))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, RoundedCornerShape(50))
                    .clickable(
                        interactionSource = searchBarInteraction,
                        indication = ripple(bounded = true),
                        onClick = onOpenSearch
                    )
                    .padding(horizontal = 16.dp)
                    .testTag("search_input_field"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colors.textMuted,
                    modifier = Modifier.size(19.dp)
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

            if (searchHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Search History",
                            tint = colors.textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Recent Searches",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textSecondary
                        )
                    }
                    Text(
                        text = "Clear All",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primary,
                        modifier = Modifier
                            .clickable { onClearSearchHistory() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("explore_clear_search_history")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = searchHistory,
                        key = { it.query },
                        contentType = { "recent_search_chip" }
                    ) { item ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(colors.surfaceElevated)
                                .border(1.dp, colors.border, RoundedCornerShape(50))
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
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category, Filters & Sort Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val label = when (cat) {
                        "movie" -> "Movies"
                        "tv" -> "TV Series"
                        else -> "All"
                    }
                    CategoryPill(
                        label = label,
                        selected = isSelected,
                        onClick = { onCategorySelect(cat) },
                        modifier = Modifier.testTag("filter_cat_$cat")
                    )
                }

                val advCount = discoverFilters.activeFilterCount()
                CategoryPill(
                    label = if (advCount > 0) "Filters ($advCount)" else "Filters",
                    selected = advCount > 0,
                    leadingIcon = Icons.Default.Tune,
                    onClick = { showFilterSheet = true },
                    modifier = Modifier.testTag("explore_advanced_filters_button")
                )

                CategoryPill(
                    label = when (sortBy) {
                        "rating" -> "Top Rated"
                        "newest" -> "Newest"
                        else -> "Popular"
                    },
                    selected = sortBy != "popular",
                    leadingIcon = Icons.Default.FilterList,
                    onClick = {
                        val next = when (sortBy) {
                            "popular" -> "rating"
                            "rating" -> "newest"
                            else -> "popular"
                        }
                        onSortSelect(next)
                    },
                    modifier = Modifier.testTag("sort_filter_chip")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Genre & Batch Size Horizontal Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BatchSizeChip(
                    batchSize = batchSize,
                    onToggleBatchSize = onBatchSizeChange,
                    testTag = "explore_batch_size_chip"
                )
                if (discoverFilters.releaseYear != null) {
                    CategoryPill(
                        label = "Year: ${discoverFilters.releaseYear} ✕",
                        selected = true,
                        onClick = { onApplyDiscoverFilters(discoverFilters.copy(releaseYear = null)) }
                    )
                }
                if (discoverFilters.minRating > 0.0) {
                    CategoryPill(
                        label = "${discoverFilters.minRating.toInt()}+ ★ ✕",
                        selected = true,
                        onClick = { onApplyDiscoverFilters(discoverFilters.copy(minRating = 0.0)) }
                    )
                }
                if (discoverFilters.originalLanguage.isNotBlank()) {
                    CategoryPill(
                        label = "Lang: ${discoverFilters.originalLanguage.uppercase()} ✕",
                        selected = true,
                        onClick = { onApplyDiscoverFilters(discoverFilters.copy(originalLanguage = "")) }
                    )
                }
                if (discoverFilters.originCountry.isNotBlank()) {
                    CategoryPill(
                        label = "Country: ${discoverFilters.originCountry} ✕",
                        selected = true,
                        onClick = { onApplyDiscoverFilters(discoverFilters.copy(originCountry = "")) }
                    )
                }
                genres.forEach { genre ->
                    val isSelected = selectedGenre == genre || genre in discoverFilters.selectedGenres
                    CategoryPill(
                        label = genre,
                        selected = isSelected,
                        onClick = { onGenreSelect(genre) },
                        modifier = Modifier.testTag("genre_chip_$genre")
                    )
                }
            }
        }

        // Status row
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
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = colors.textMuted
            )

            if (isSearching || effectiveIsLoadingMore || isLoadingDiscover || (isLoadingCatalog && filteredList.isEmpty())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = colors.primary,
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
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
                        color = colors.textSecondary
                    )
                }
            }
        }

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
                "skeleton" -> SearchResultsSkeletonGrid()
                "error" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomClearance)
                            .testTag("explore_search_error_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CinetraEmptyState(
                                icon = Icons.Default.WifiOff,
                                title = "TMDb Search Unavailable",
                                subtitle = searchErrorMessage.orEmpty()
                            )
                            RedButton(
                                onClick = onRetrySearch,
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
                            .padding(bottom = bottomClearance)
                            .testTag("explore_empty_state"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CinetraEmptyState(
                                icon = Icons.Default.Search,
                                title = if (searchQuery.isNotBlank()) {
                                    "No results found for \"$searchQuery\""
                                } else {
                                    "No matching titles found"
                                },
                                subtitle = if (searchQuery.isNotBlank()) {
                                    "Try another search query or reset your category and genre filters."
                                } else {
                                    "Reset your active filters or search TMDb above to discover more movies and series."
                                }
                            )
                            if (hasActiveFilterOrSearch) {
                                OutlinedButton(
                                    onClick = onResetFilters,
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = colors.surfaceElevated,
                                        contentColor = colors.textPrimary
                                    ),
                                    border = BorderStroke(1.dp, colors.border),
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

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columnCount = if (maxWidth >= 600.dp) 4 else 3
                        val horizontalSpacing = 10.dp
                        val totalGap = 32.dp + horizontalSpacing * (columnCount - 1)
                        val colWidth = ((maxWidth - totalGap) / columnCount).coerceAtLeast(96.dp)
                        val colHeight = colWidth * 1.5f
                        val showTypePill = selectedCategory.equals("All", ignoreCase = true)

                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(columnCount),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 12.dp,
                                bottom = bottomClearance + 24.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
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
                                    cardWidth = colWidth,
                                    cardHeight = colHeight,
                                    showTypePill = showTypePill,
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
    }

    if (showFilterSheet) {
        AdvancedFilterSheet(
            currentFilters = discoverFilters,
            onApplyFilters = onApplyDiscoverFilters,
            onDismiss = { showFilterSheet = false }
        )
    }
}
