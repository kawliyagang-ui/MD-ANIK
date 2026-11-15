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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CatalogBatchPolicy
import com.example.data.model.MediaItem
import com.example.data.model.TmdbSearchHistoryEntity
import com.example.ui.components.BatchSizeChip
import com.example.ui.components.CineGradientButton
import com.example.ui.components.LoadMoreBatchFooter
import com.example.ui.components.MovieCard
import com.example.ui.components.SearchResultsSkeletonGrid
import com.example.ui.theme.CineTheme

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
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    val focusRequester = remember { FocusRequester() }
    var selectedTypeFilter by remember { mutableStateOf("All") }

    val filteredResults = remember(searchResults, selectedTypeFilter) {
        when (selectedTypeFilter) {
            "movie" -> searchResults.filter { it.mediaType.equals("movie", ignoreCase = true) }
            "tv" -> searchResults.filter { it.mediaType.equals("tv", ignoreCase = true) }
            else -> searchResults
        }
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
                            .testTag("search_screen_input_field")
                    )
                }

                // Type Filter Pills when querying or results exist
                AnimatedVisibility(
                    visible = searchQuery.isNotBlank(),
                    enter = fadeIn(tween(180)) + expandVertically(),
                    exit = fadeOut(tween(140)) + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("All" to "All", "movie" to "Movies", "tv" to "TV Series").forEach { (key, label) ->
                            val isSelected = selectedTypeFilter == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTypeFilter = key },
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
                        Spacer(modifier = Modifier.weight(1f))
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

        // Status / Results Count Bar
        val effectiveSearchTotal = if (selectedTypeFilter == "All") {
            maxOf(totalResultsCount, filteredResults.size)
        } else {
            filteredResults.size
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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
}
