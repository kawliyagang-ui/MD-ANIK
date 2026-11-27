package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CatalogBatchPolicy
import com.example.data.model.DiscoverFilters
import com.example.data.model.MediaItem
import com.example.data.model.SearchSuggestionPolicy
import com.example.data.model.TmdbSearchHistoryEntity
import com.example.ui.components.AdvancedFilterSheet
import com.example.ui.components.BatchSizeChip
import com.example.ui.components.CatalogCountChip
import com.example.ui.components.CategoryPill
import com.example.ui.components.CineEase
import com.example.ui.components.CinetraEmptyState
import com.example.ui.components.LoadMoreBatchFooter
import com.example.ui.components.MovieCard
import com.example.ui.components.RatingInlineLabel
import com.example.ui.components.RedButton
import com.example.ui.components.SearchResultsSkeletonGrid
import com.example.ui.components.cinePressScale
import com.example.ui.components.rememberCineImageRequest
import com.example.ui.components.rememberDarkImagePainter
import com.example.ui.theme.CinetraTheme

private sealed interface SearchViewMode {
    data object Idle : SearchViewMode
    data object Loading : SearchViewMode
    data class Error(val message: String) : SearchViewMode
    data class Empty(val query: String) : SearchViewMode
    data class Results(val items: List<MediaItem>) : SearchViewMode
}

@Composable
fun SearchScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<MediaItem>,
    isSearching: Boolean,
    searchHistory: List<TmdbSearchHistoryEntity>,
    onDeleteSearchHistory: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    searchErrorMessage: String?,
    onRetrySearch: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onBack: () -> Unit,
    totalResultsCount: Int = searchResults.size,
    batchSize: Int = CatalogBatchPolicy.DEFAULT_BATCH_SIZE,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onBatchSizeChange: (Int) -> Unit = {},
    onLoadMore: () -> Unit = {},
    searchSuggestions: List<MediaItem> = emptyList(),
    isFetchingSuggestions: Boolean = false,
    showSuggestions: Boolean = false,
    onSubmitSearch: (String) -> Unit = {},
    onSuggestionMediaClick: (MediaItem) -> Unit = onMediaClick,
    onSuggestionFillQuery: (String) -> Unit = onSearchQueryChange,
    onDismissSuggestions: () -> Unit = {},
    onSearchFieldFocusWithQuery: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = CinetraTheme.colors
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchFilters by remember { mutableStateOf(DiscoverFilters()) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val filteredResults = remember(searchResults, searchFilters) {
        val matched = searchResults.filter { searchFilters.matchesItem(it) }
        searchFilters.sortItems(matched)
    }

    val filteredSuggestions = remember(searchSuggestions, searchFilters.mediaType) {
        SearchSuggestionPolicy.filterByMediaType(searchSuggestions, searchFilters.mediaType)
    }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("dedicated_search_screen")
    ) {
        // Top Search Input Bar
        Surface(
            color = colors.background,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 22.dp, bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("search_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            "Search movies, TV shows, anime...",
                            color = colors.textMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.textMuted
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.testTag("search_screen_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onSubmitSearch(searchQuery)
                        }
                    ),
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border,
                        focusedContainerColor = colors.surfaceElevated,
                        unfocusedContainerColor = colors.surfaceElevated,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused && searchQuery.isNotBlank()) {
                                onSearchFieldFocusWithQuery()
                            }
                        }
                        .testTag("search_screen_input_field")
                )

                // Type Filter + Advanced Filters Pills when querying
                AnimatedVisibility(
                    visible = searchQuery.isNotBlank(),
                    enter = fadeIn(tween(180, easing = CineEase)) + expandVertically(tween(200, easing = CineEase)),
                    exit = fadeOut(tween(140, easing = CineEase)) + shrinkVertically(tween(160, easing = CineEase))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 4.dp, end = 4.dp, top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("All" to "All", "movie" to "Movies", "tv" to "TV Series").forEach { (key, label) ->
                            val isSelected = searchFilters.mediaType == key
                            CategoryPill(
                                label = label,
                                selected = isSelected,
                                onClick = { searchFilters = searchFilters.copy(mediaType = key) },
                                modifier = Modifier.testTag("search_type_filter_$key")
                            )
                        }
                        val advCount = searchFilters.activeFilterCount()
                        CategoryPill(
                            label = if (advCount > 0) "Filters ($advCount)" else "Filters",
                            selected = advCount > 0,
                            leadingIcon = Icons.Default.Tune,
                            onClick = { showFilterSheet = true },
                            modifier = Modifier.testTag("search_advanced_filters_button")
                        )
                        BatchSizeChip(
                            batchSize = batchSize,
                            onToggleBatchSize = onBatchSizeChange,
                            testTag = "search_batch_size_chip"
                        )
                    }
                }

                // Recent Searches Row
                AnimatedVisibility(
                    visible = searchHistory.isNotEmpty() && searchQuery.isBlank(),
                    enter = fadeIn(tween(180, easing = CineEase)) + expandVertically(tween(200, easing = CineEase)),
                    exit = fadeOut(tween(140, easing = CineEase)) + shrinkVertically(tween(160, easing = CineEase))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
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
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onClearSearchHistory() }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("search_clear_history_button")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                items = searchHistory,
                                key = { it.query },
                                contentType = { "search_history_chip" }
                            ) { item ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(colors.surfaceElevated)
                                        .border(1.dp, colors.border, RoundedCornerShape(50))
                                        .clickable { onSearchQueryChange(item.query) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .testTag("search_history_chip_${item.query}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                                .testTag("search_delete_chip_${item.query}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = colors.textMuted,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Real-Time TMDb Search Suggestions Dropdown Panel
        val shouldRenderSuggestions = showSuggestions &&
            searchQuery.isNotBlank() &&
            (filteredSuggestions.isNotEmpty() || isFetchingSuggestions)
        AnimatedVisibility(
            visible = shouldRenderSuggestions,
            enter = fadeIn(tween(180, easing = CineEase)) + expandVertically(tween(220, easing = CineEase)),
            exit = fadeOut(tween(140, easing = CineEase)) + shrinkVertically(tween(180, easing = CineEase))
        ) {
            RealTimeSuggestionsPanel(
                query = searchQuery.trim(),
                suggestions = filteredSuggestions,
                isLoading = isFetchingSuggestions,
                onSuggestionClick = { item ->
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSuggestionMediaClick(item)
                },
                onFillQueryClick = { title ->
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSuggestionFillQuery(title)
                },
                onSeeAllResults = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSubmitSearch(searchQuery)
                },
                onHideSuggestions = onDismissSuggestions
            )
        }

        // Status / Results Count Bar
        val isSearchOrTypeFiltered = searchQuery.isNotBlank() || searchFilters.hasAdvancedFilters()
        val effectiveSearchTotal = if (!searchFilters.hasAdvancedFilters()) {
            maxOf(totalResultsCount, filteredResults.size)
        } else {
            filteredResults.size
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = when {
                    isSearching -> "Searching TMDb..."
                    searchQuery.isBlank() -> "Type a query above to explore movies & TV series"
                    else -> CatalogBatchPolicy.formatAvailabilitySummary(
                        shownCount = filteredResults.size,
                        totalAvailable = effectiveSearchTotal,
                        singular = "result",
                        plural = "results"
                    ) + " for \"$searchQuery\""
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = colors.textMuted,
                modifier = Modifier.weight(1f)
            )

            if (isSearching || isLoadingMore) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = colors.primary,
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLoadingMore) "Loading batch" else "Searching",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }

            if (searchQuery.isNotBlank()) {
                CatalogCountChip(
                    totalAvailable = effectiveSearchTotal,
                    isFiltered = isSearchOrTypeFiltered,
                    isLoading = isSearching,
                    testTag = "search_header_count_chip"
                )
            }
        }

        // Animated Content States
        val viewMode: SearchViewMode = when {
            isSearching && filteredResults.isEmpty() -> SearchViewMode.Loading
            searchErrorMessage != null && filteredResults.isEmpty() -> SearchViewMode.Error(searchErrorMessage)
            searchQuery.isBlank() -> SearchViewMode.Idle
            filteredResults.isEmpty() -> SearchViewMode.Empty(searchQuery)
            else -> SearchViewMode.Results(filteredResults)
        }

        AnimatedContent(
            targetState = viewMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(220, easing = CineEase)) togetherWith
                    fadeOut(animationSpec = tween(160, easing = CineEase))
            },
            contentKey = { mode ->
                when (mode) {
                    SearchViewMode.Idle -> "idle"
                    SearchViewMode.Loading -> "loading"
                    is SearchViewMode.Error -> "error"
                    is SearchViewMode.Empty -> "empty"
                    is SearchViewMode.Results -> "results"
                }
            },
            label = "search_results_transition",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { mode ->
            when (mode) {
                SearchViewMode.Loading -> {
                    SearchResultsSkeletonGrid()
                }
                is SearchViewMode.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CinetraEmptyState(
                                icon = Icons.Default.WifiOff,
                                title = "Search Unavailable",
                                subtitle = mode.message
                            )
                            RedButton(onClick = onRetrySearch) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Search", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                SearchViewMode.Idle -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CinetraEmptyState(
                            icon = Icons.Default.Search,
                            title = "Discover Anything",
                            subtitle = "Search across thousands of movies, TV series, anime, and Cinetra specials instantly."
                        )
                    }
                }
                is SearchViewMode.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CinetraEmptyState(
                            icon = Icons.Default.Search,
                            title = "No results found for \"${mode.query}\"",
                            subtitle = "Check your spelling, switch type filter, or try another title."
                        )
                    }
                }
                is SearchViewMode.Results -> {
                    val gridState = rememberLazyGridState()
                    val shouldAutoLoadMore by remember(hasMore, isLoadingMore, mode.items.size) {
                        derivedStateOf {
                            val total = gridState.layoutInfo.totalItemsCount
                            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                            hasMore && !isLoadingMore && total > 8 && lastVisible >= total - 4
                        }
                    }
                    LaunchedEffect(shouldAutoLoadMore) {
                        if (shouldAutoLoadMore) {
                            onLoadMore()
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val columnCount = if (maxWidth >= 600.dp) 4 else 3
                        val horizontalSpacing = 10.dp
                        val totalGap = 32.dp + horizontalSpacing * (columnCount - 1)
                        val colWidth = ((maxWidth - totalGap) / columnCount).coerceAtLeast(96.dp)
                        val colHeight = colWidth * 1.5f
                        val topResult = mode.items.firstOrNull()
                        val remainingItems = remember(mode.items) {
                            if (mode.items.size > 1) mode.items.drop(1) else emptyList()
                        }

                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(columnCount),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("search_results_grid")
                        ) {
                            if (topResult != null) {
                                item(
                                    span = { GridItemSpan(maxLineSpan) },
                                    key = "top_result_${topResult.id}",
                                    contentType = "top_result_card"
                                ) {
                                    SearchTopResultCard(
                                        item = topResult,
                                        onClick = { onMediaClick(topResult) },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }

                            items(
                                items = remainingItems,
                                key = { it.id },
                                contentType = { "movie_card" }
                            ) { item ->
                                MovieCard(
                                    media = item,
                                    onClick = { onMediaClick(item) },
                                    cardWidth = colWidth,
                                    cardHeight = colHeight,
                                    showTypePill = true,
                                    modifier = Modifier.animateItem()
                                )
                            }

                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "search_load_more_footer",
                                contentType = "load_more_footer"
                            ) {
                                LoadMoreBatchFooter(
                                    shownCount = mode.items.size,
                                    totalAvailable = effectiveSearchTotal,
                                    batchSize = batchSize,
                                    hasMore = hasMore,
                                    isLoadingMore = isLoadingMore,
                                    onLoadMore = onLoadMore,
                                    testTag = "search_load_more_button"
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
            currentFilters = searchFilters,
            onApplyFilters = { searchFilters = it },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun SearchTopResultCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CinetraTheme.colors
    val cardShape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val darkPlaceholder = rememberDarkImagePainter(colors.surface)
    val imageUrl = remember(item.fullBackdropUrl, item.fullPosterUrl) {
        item.fullBackdropUrl.ifBlank { item.fullPosterUrl }
    }
    val yearText = remember(item.releaseDate) { item.releaseDate.take(4) }
    val typeLabel = remember(item.mediaType) {
        if (item.mediaType.equals("tv", ignoreCase = true)) "TV SERIES" else "MOVIE"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .cinePressScale(interactionSource)
            .clip(cardShape)
            .background(colors.surface)
            .border(1.dp, colors.border, cardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .testTag("movie_card_${item.id}")
    ) {
        if (imageUrl.isNotBlank()) {
            val req = rememberCineImageRequest(
                url = imageUrl,
                widthDp = 360.dp,
                heightDp = 202.dp
            )
            AsyncImage(
                model = req,
                contentDescription = item.title,
                placeholder = darkPlaceholder,
                error = darkPlaceholder,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.28f),
                        1.0f to colors.background.copy(alpha = 0.94f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.primary)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "TOP RESULT",
                        color = colors.onPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = typeLabel,
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (yearText.isNotBlank() || item.voteAverage > 0.0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (yearText.isNotBlank()) {
                        Text(
                            text = yearText,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                    if (yearText.isNotBlank() && item.voteAverage > 0.0) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                    if (item.voteAverage > 0.0) {
                        RatingInlineLabel(
                            rating = item.voteAverage,
                            fontSize = 12.sp,
                            textColor = colors.textPrimary,
                            iconSize = 12.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RealTimeSuggestionsPanel(
    query: String,
    suggestions: List<MediaItem>,
    isLoading: Boolean,
    onSuggestionClick: (MediaItem) -> Unit,
    onFillQueryClick: (String) -> Unit,
    onSeeAllResults: () -> Unit,
    onHideSuggestions: () -> Unit
) {
    val colors = CinetraTheme.colors

    Surface(
        color = colors.surface,
        shape = MaterialTheme.shapes.medium, // 14.dp
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(1.dp, colors.border, MaterialTheme.shapes.medium)
            .testTag("search_suggestions_panel")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Suggestions Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLoading && suggestions.isEmpty()) {
                            "Searching TMDb..."
                        } else {
                            "Live Suggestions (${suggestions.size})"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )
                    if (isLoading) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            color = colors.primary,
                            strokeWidth = 1.8.dp,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Text(
                    text = "Hide",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onHideSuggestions)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("search_suggestions_hide_button")
                )
            }

            HorizontalDivider(color = colors.border)

            if (suggestions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 310.dp)
                        .testTag("search_suggestions_list")
                ) {
                    items(
                        items = suggestions,
                        key = { "sug_${it.id}" },
                        contentType = { "suggestion_row" }
                    ) { item ->
                        SuggestionRowItem(
                            item = item,
                            query = query,
                            onClick = { onSuggestionClick(item) },
                            onFillClick = { onFillQueryClick(item.title) }
                        )
                        HorizontalDivider(
                            color = colors.border.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                }
            } else if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = colors.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Finding matches on TMDb...",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }
            }

            // Footer: See All Full Grid Results
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onSeeAllResults)
                    .background(colors.surfaceElevated)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("search_suggestions_see_all_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "See all results for \"$query\"",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "See all results",
                    tint = colors.primary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun SuggestionRowItem(
    item: MediaItem,
    query: String,
    onClick: () -> Unit,
    onFillClick: () -> Unit
) {
    val colors = CinetraTheme.colors
    val year = remember(item.releaseDate) { item.releaseDate.take(4) }
    val typeLabel = if (item.mediaType.equals("tv", ignoreCase = true)) "TV Series" else "Movie"
    val rowInteraction = remember { MutableInteractionSource() }
    val posterDarkPlaceholder = rememberDarkImagePainter(colors.surfaceElevated)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .cinePressScale(rowInteraction)
            .clickable(
                interactionSource = rowInteraction,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
            .testTag("search_suggestion_item_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster Thumbnail
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceElevated)
                .border(1.dp, colors.border, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (item.fullPosterUrl.isNotBlank()) {
                val posterRequest = rememberCineImageRequest(
                    url = item.fullPosterUrl,
                    widthDp = 36.dp,
                    heightDp = 52.dp
                )
                AsyncImage(
                    model = posterRequest,
                    placeholder = posterDarkPlaceholder,
                    error = posterDarkPlaceholder,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Highlighted Title + Type / Year / Rating
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildHighlightedSuggestionTitle(
                    title = item.title,
                    query = query,
                    highlightColor = colors.primary
                ),
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = typeLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )

                if (year.isNotBlank()) {
                    Text(
                        text = "• $year",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }

                if (item.voteAverage > 0.0) {
                    Text(text = "•", fontSize = 11.sp, color = colors.textMuted)
                    RatingInlineLabel(
                        rating = item.voteAverage,
                        textColor = colors.textMuted
                    )
                }
            }
        }

        IconButton(
            onClick = onFillClick,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("search_suggestion_fill_${item.id}")
        ) {
            Icon(
                imageVector = Icons.Default.NorthWest,
                contentDescription = "Fill ${item.title}",
                tint = colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun buildHighlightedSuggestionTitle(
    title: String,
    query: String,
    highlightColor: Color
): AnnotatedString {
    val cleanQuery = query.trim()
    if (cleanQuery.isBlank()) return AnnotatedString(title)
    val startIndex = title.indexOf(cleanQuery, ignoreCase = true)
    if (startIndex < 0) return AnnotatedString(title)
    val endIndex = (startIndex + cleanQuery.length).coerceAtMost(title.length)
    return buildAnnotatedString {
        append(title.substring(0, startIndex))
        withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.ExtraBold)) {
            append(title.substring(startIndex, endIndex))
        }
        if (endIndex < title.length) {
            append(title.substring(endIndex))
        }
    }
}
