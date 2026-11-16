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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import com.example.ui.components.CineGradientButton
import com.example.ui.components.LoadMoreBatchFooter
import com.example.ui.components.MovieCard
import com.example.ui.components.SearchResultsSkeletonGrid
import com.example.ui.theme.CineTheme
import java.util.Locale

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
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
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
            .background(gradients.appBackdrop)
            .testTag("dedicated_search_screen")
    ) {
        // Top Bar Header with Back and Search Input
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .background(gradients.topBarBackdrop)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
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
                                tint = colors.softRose
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
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.softRose,
                            unfocusedBorderColor = colors.border,
                            focusedContainerColor = colors.surfaceElevated,
                            unfocusedContainerColor = colors.surfaceElevated,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && searchQuery.isNotBlank()) {
                                    onSearchFieldFocusWithQuery()
                                }
                            }
                            .testTag("search_screen_input_field")
                    )
                }

                // Type Filter + Advanced Filters Pills when querying or results exist
                AnimatedVisibility(
                    visible = searchQuery.isNotBlank(),
                    enter = fadeIn(tween(180)) + expandVertically(),
                    exit = fadeOut(tween(140)) + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 8.dp, end = 8.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("All" to "All", "movie" to "Movies", "tv" to "TV Series").forEach { (key, label) ->
                            val isSelected = searchFilters.mediaType == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { searchFilters = searchFilters.copy(mediaType = key) },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.accentMid,
                                    selectedLabelColor = Color.White,
                                    containerColor = colors.surfaceElevated,
                                    labelColor = colors.textPrimary
                                ),
                                modifier = Modifier.testTag("search_type_filter_$key")
                            )
                        }
                        val advCount = searchFilters.activeFilterCount()
                        FilterChip(
                            selected = advCount > 0,
                            onClick = { showFilterSheet = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Filters",
                                    tint = if (advCount > 0) Color.White else colors.gold,
                                    modifier = Modifier.size(14.dp)
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
                    enter = fadeIn(tween(180)) + expandVertically(),
                    exit = fadeOut(tween(140)) + shrinkVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = colors.gold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Recent Searches:",
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
                                    .testTag("search_clear_history_button")
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(searchHistory, key = { it.query }) { item ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(colors.surface)
                                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                                        .clickable { onSearchQueryChange(item.query) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                        .testTag("search_history_chip_${item.query}")
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
                                                .testTag("search_delete_chip_${item.query}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(11.dp)
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
            enter = fadeIn(tween(180)) + expandVertically(tween(220)),
            exit = fadeOut(tween(140)) + shrinkVertically(tween(180))
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
                fontSize = 12.sp,
                color = colors.textSecondary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            if (isSearching || isLoadingMore) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = colors.softRose,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLoadingMore) "Loading batch" else "Searching",
                        fontSize = 11.sp,
                        color = colors.gold
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
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(160))
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
                            .padding(32.dp),
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
                                text = "Search Unavailable",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = mode.message,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            CineGradientButton(
                                onClick = onRetrySearch,
                                shape = RoundedCornerShape(50),
                                minHeight = 48.dp
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Search", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                SearchViewMode.Idle -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = colors.textMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Discover Anything",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Search across thousands of movies, TV series, anime, and Cinetra specials instantly.",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                is SearchViewMode.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
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
                                text = "No results found for \"${mode.query}\"",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Check your spelling, switch type filter, or try another title.",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
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

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 135.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("search_results_grid")
                    ) {
                        items(mode.items, key = { it.id }) { item ->
                            MovieCard(
                                media = item,
                                onClick = { onMediaClick(item) },
                                cardWidth = 140.dp,
                                cardHeight = 205.dp,
                                modifier = Modifier.animateItem()
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }, key = "search_load_more_footer") {
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

    if (showFilterSheet) {
        AdvancedFilterSheet(
            currentFilters = searchFilters,
            onApplyFilters = { searchFilters = it },
            onDismiss = { showFilterSheet = false }
        )
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
    val colors = CineTheme.colors

    Surface(
        color = colors.surfaceElevated,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(1.dp, colors.border.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
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
                        tint = colors.softRose,
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
                        color = colors.gold
                    )
                    if (isLoading) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            color = colors.softRose,
                            strokeWidth = 1.8.dp,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Text(
                    text = "Hide",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onHideSuggestions)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("search_suggestions_hide_button")
                )
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.45f))

            if (suggestions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 310.dp)
                        .testTag("search_suggestions_list")
                ) {
                    items(suggestions, key = { "sug_${it.id}" }) { item ->
                        SuggestionRowItem(
                            item = item,
                            query = query,
                            onClick = { onSuggestionClick(item) },
                            onFillClick = { onFillQueryClick(item.title) }
                        )
                        HorizontalDivider(
                            color = colors.border.copy(alpha = 0.25f),
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
                        color = colors.softRose,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Finding matches on TMDb...",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }

            // Footer: See All Full Grid Results
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onSeeAllResults)
                    .background(colors.surface.copy(alpha = 0.7f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("search_suggestions_see_all_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "See all results for \"$query\"",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.softRose,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "See all results",
                    tint = colors.softRose,
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
    val colors = CineTheme.colors
    val year = remember(item.releaseDate) { item.releaseDate.take(4) }
    val typeLabel = if (item.mediaType.equals("tv", ignoreCase = true)) "TV Series" else "Movie"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
            .testTag("search_suggestion_item_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster Thumbnail
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (item.fullPosterUrl.isNotBlank()) {
                AsyncImage(
                    model = item.fullPosterUrl,
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
                    highlightColor = colors.softRose
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (item.mediaType.equals("tv", ignoreCase = true)) {
                                colors.gold.copy(alpha = 0.16f)
                            } else {
                                colors.softRose.copy(alpha = 0.16f)
                            }
                        )
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = typeLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.mediaType.equals("tv", ignoreCase = true)) colors.gold else colors.softRose
                    )
                }

                if (year.isNotBlank()) {
                    Text(
                        text = year,
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }

                if (item.voteAverage > 0.0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = colors.gold,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", item.voteAverage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.gold
                        )
                    }
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
                tint = colors.textSecondary,
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

