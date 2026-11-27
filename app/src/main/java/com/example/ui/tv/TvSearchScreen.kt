package com.example.ui.tv

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.example.data.model.MediaItem
import com.example.data.model.TmdbSearchHistoryEntity
import com.example.ui.theme.CineTheme
import kotlinx.coroutines.delay

@Composable
fun TvSearchScreen(
    searchQuery: String,
    searchResults: List<MediaItem>,
    isSearching: Boolean,
    searchHistory: List<TmdbSearchHistoryEntity>,
    searchErrorMessage: String?,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    allFallbackCatalog: List<MediaItem>,
    onSearchQueryChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onRetrySearch: () -> Unit,
    onLoadMore: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFieldRequester = remember { FocusRequester() }
    var isFieldFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50L)
        runCatching { searchFieldRequester.requestFocus() }
    }

    val shownItems = remember(searchQuery, searchResults, allFallbackCatalog) {
        if (searchQuery.isBlank()) allFallbackCatalog.take(30) else searchResults
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradients.appBackdrop)
            .padding(horizontal = TvOverscanHorizontal, vertical = 12.dp)
            .testTag("tv_search_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Search movies, series, anime...",
                        color = colors.textMuted,
                        fontSize = 16.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colors.softRose
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        onSubmitSearch(searchQuery)
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.softRose,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surfaceElevated,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.softRose
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(searchFieldRequester)
                    .onFocusChanged { state ->
                        isFieldFocused = state.isFocused
                        if (state.isFocused) {
                            keyboardController?.show()
                        }
                    }
                    .border(
                        width = if (isFieldFocused) 2.dp else 0.dp,
                        color = if (isFieldFocused) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .testTag("tv_search_input")
            )

            if (searchQuery.isNotEmpty()) {
                TvOutlinedActionButton(
                    text = "Clear",
                    icon = Icons.Default.Clear,
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.testTag("tv_search_clear_button")
                )
            }
        }

        // Recent Search History Chips
        if (searchQuery.isBlank() && searchHistory.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Recent:",
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = searchHistory.take(10),
                        key = { "hist_${it.query}" }
                    ) { hist ->
                        TvFilterPill(
                            text = hist.query,
                            selected = false,
                            onClick = {
                                onSearchQueryChange(hist.query)
                                onSubmitSearch(hist.query)
                            }
                        )
                    }
                }
                TvOutlinedActionButton(
                    text = "Clear History",
                    icon = Icons.Default.DeleteOutline,
                    onClick = onClearSearchHistory,
                    modifier = Modifier.testTag("tv_search_clear_history_button")
                )
            }
        }

        // Status / Results Area
        when {
            isSearching && searchResults.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(color = colors.softRose)
                        Text("Searching \"$searchQuery\"...", color = colors.textSecondary, fontSize = 15.sp)
                    }
                }
            }
            searchQuery.isNotBlank() && searchErrorMessage != null && searchResults.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(searchErrorMessage, color = colors.softRose, fontSize = 16.sp)
                        TvActionButton(
                            text = "Retry Search",
                            icon = Icons.Default.Refresh,
                            onClick = onRetrySearch
                        )
                    }
                }
            }
            searchQuery.isNotBlank() && searchResults.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No results found for \"$searchQuery\".",
                        color = colors.textSecondary,
                        fontSize = 18.sp
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 154.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("tv_search_results_grid"),
                    contentPadding = PaddingValues(top = 6.dp, bottom = TvOverscanVertical + 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "tv_search_section_label") {
                        Text(
                            text = if (searchQuery.isBlank()) "Popular Titles to Explore" else "Search Results (${searchResults.size})",
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(
                        items = shownItems,
                        key = { "tv_search_${it.id}" },
                        contentType = { "tv_poster_card" }
                    ) { item ->
                        TvPosterCard(
                            media = item,
                            onClick = { onMediaClick(item) }
                        )
                    }

                    if (searchQuery.isNotBlank() && hasMore) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "tv_search_load_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TvOutlinedActionButton(
                                    text = if (isLoadingMore) "Loading more..." else "Load More Results",
                                    enabled = !isLoadingMore,
                                    onClick = onLoadMore,
                                    modifier = Modifier.testTag("tv_search_load_more_button")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
