package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.example.data.model.TmdbSearchHistoryEntity
import com.example.ui.components.CineGradientButton
import com.example.ui.components.MovieCard
import com.example.ui.components.SearchResultsSkeletonGrid
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
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    val genres = remember {
        listOf(
            "All", "Action", "Adventure", "Animation", "Comedy", "Crime",
            "Drama", "Fantasy", "Horror", "Mystery", "Sci-Fi", "Thriller"
        )
    }

    val categories = remember {
        listOf("All", "movie", "tv")
    }

    // Determine displayed items
    val baseList = if (searchQuery.isNotBlank()) searchResults else allTitles

    val filteredList = remember(baseList, selectedCategory, selectedGenre, sortBy) {
        var list = baseList

        if (selectedCategory != "All") {
            list = list.filter { it.mediaType.equals(selectedCategory, ignoreCase = true) }
        }

        if (selectedGenre != "All") {
            list = list.filter { item ->
                item.genres.any { it.contains(selectedGenre, ignoreCase = true) }
            }
        }

        when (sortBy) {
            "rating" -> list.sortedByDescending { it.voteAverage }
            "newest" -> list.sortedByDescending { it.releaseDate }
            else -> list // Default order
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradients.appBackdrop)
    ) {
        // Search Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search movies, TV shows, anime...", color = colors.textMuted, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.softRose
                    )
                },
                trailingIcon = {
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
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.softRose,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surfaceElevated,
                    unfocusedContainerColor = colors.surfaceElevated,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input_field")
            )

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
                    items(searchHistory, key = { it.query }) { item ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                                .clickable { onSearchQueryChange(item.query) }
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

            // Genre Horizontal Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                genres.forEach { genre ->
                    val isSelected = selectedGenre == genre
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
                    else -> "${filteredList.size} titles found"
                },
                fontSize = 12.sp,
                color = colors.textSecondary,
                fontWeight = FontWeight.Medium
            )

            if (isSearching || (isLoadingCatalog && filteredList.isEmpty())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = colors.softRose,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSearching) "Fetching results" else "Syncing TMDb",
                        fontSize = 11.sp,
                        color = colors.gold
                    )
                }
            }
        }

        // Grid of results / Skeleton / Error / Empty state
        if (showSkeletonGrid) {
            SearchResultsSkeletonGrid()
        } else if (searchErrorMessage != null && filteredList.isEmpty() && !isSearching) {
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
                        text = searchErrorMessage,
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
        } else if (filteredList.isEmpty() && !isSearching) {
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
                    if (selectedCategory != "All" || selectedGenre != "All" || searchQuery.isNotBlank()) {
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
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 135.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.id }) { item ->
                    MovieCard(
                        media = item,
                        onClick = { onMediaClick(item) },
                        cardWidth = 140.dp,
                        cardHeight = 205.dp
                    )
                }
            }
        }
    }
}
