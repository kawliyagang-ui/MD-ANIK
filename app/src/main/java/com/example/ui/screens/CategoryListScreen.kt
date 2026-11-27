package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CatalogBatchPolicy
import com.example.data.model.MediaItem
import com.example.ui.components.BatchSizeChip
import com.example.ui.components.CatalogCountChip
import com.example.ui.components.CategoryPill
import com.example.ui.components.CineEase
import com.example.ui.components.CinetraEmptyState
import com.example.ui.components.LoadMoreBatchFooter
import com.example.ui.components.MovieCard
import com.example.ui.theme.CinetraTheme

@Composable
fun CategoryListScreen(
    title: String,
    mediaItems: List<MediaItem>,
    onMediaClick: (MediaItem) -> Unit,
    onBack: () -> Unit,
    totalAvailableCount: Int = mediaItems.size,
    batchSize: Int = CatalogBatchPolicy.DEFAULT_BATCH_SIZE,
    hasMore: Boolean = false,
    isLoadingMore: Boolean = false,
    onBatchSizeChange: (Int) -> Unit = {},
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = CinetraTheme.colors

    var selectedType by remember(title) { mutableStateOf("All") }
    var sortBy by remember(title) { mutableStateOf("default") }

    val hasMovies = remember(mediaItems) { mediaItems.any { it.mediaType.equals("movie", ignoreCase = true) } }
    val hasTv = remember(mediaItems) { mediaItems.any { it.mediaType.equals("tv", ignoreCase = true) } }
    val showTypeFilter = hasMovies && hasTv

    val displayedItems = remember(mediaItems, selectedType, sortBy) {
        val typeFiltered = when (selectedType) {
            "movie" -> mediaItems.filter { it.mediaType.equals("movie", ignoreCase = true) }
            "tv" -> mediaItems.filter { it.mediaType.equals("tv", ignoreCase = true) }
            else -> mediaItems
        }
        when (sortBy) {
            "rating" -> typeFiltered.sortedByDescending { it.voteAverage }
            "newest" -> typeFiltered.sortedByDescending { it.releaseDate }
            else -> typeFiltered
        }
    }

    val isTypeFiltered = selectedType != "All"
    val effectiveTotal = if (isTypeFiltered) {
        displayedItems.size
    } else {
        maxOf(totalAvailableCount, displayedItems.size)
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("category_list_screen")
    ) {
        // Top Header Bar
        Surface(
            color = colors.background,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("category_list_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium, // Bold 18.sp
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = CatalogBatchPolicy.formatAvailabilitySummary(
                                shownCount = displayedItems.size,
                                totalAvailable = effectiveTotal
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }

                    CategoryPill(
                        label = when (sortBy) {
                            "rating" -> "Top Rated"
                            "newest" -> "Newest"
                            else -> "Featured"
                        },
                        selected = sortBy != "default",
                        leadingIcon = Icons.Default.FilterList,
                        onClick = {
                            sortBy = when (sortBy) {
                                "default" -> "rating"
                                "rating" -> "newest"
                                else -> "default"
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("category_sort_chip")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showTypeFilter) {
                        listOf("All" to "All Titles", "movie" to "Movies", "tv" to "TV Series").forEach { (key, label) ->
                            val isSelected = selectedType == key
                            CategoryPill(
                                label = label,
                                selected = isSelected,
                                onClick = { selectedType = key },
                                modifier = Modifier.testTag("category_type_chip_$key")
                            )
                        }
                    }

                    BatchSizeChip(
                        batchSize = batchSize,
                        onToggleBatchSize = onBatchSizeChange,
                        testTag = "category_batch_size_chip"
                    )

                    CatalogCountChip(
                        totalAvailable = effectiveTotal,
                        isFiltered = isTypeFiltered,
                        testTag = "category_header_count_chip"
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border)
                )
            }
        }

        AnimatedContent(
            targetState = displayedItems.isEmpty(),
            transitionSpec = {
                fadeIn(animationSpec = tween(220, easing = CineEase)) togetherWith
                    fadeOut(animationSpec = tween(160, easing = CineEase))
            },
            label = "category_list_content_transition",
            modifier = Modifier.fillMaxSize()
        ) { isEmptyState ->
            if (isEmptyState) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CinetraEmptyState(
                        icon = Icons.Default.MovieFilter,
                        title = "No titles in $title",
                        subtitle = "Check back later or try clearing your category filter."
                    )
                }
            } else {
                val gridState = rememberLazyGridState()
                val shouldAutoLoadMore by remember(hasMore, isLoadingMore, displayedItems.size) {
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("category_list_grid")
                ) {
                    items(
                        items = displayedItems,
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
                        key = "category_load_more_footer",
                        contentType = "load_more_footer"
                    ) {
                        LoadMoreBatchFooter(
                            shownCount = displayedItems.size,
                            totalAvailable = maxOf(totalAvailableCount, displayedItems.size),
                            batchSize = batchSize,
                            hasMore = hasMore,
                            isLoadingMore = isLoadingMore,
                            onLoadMore = onLoadMore,
                            testTag = "category_load_more_button"
                        )
                    }
                }
            }
        }
    }
}
