package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CatalogBatchPolicy
import com.example.data.model.MediaItem
import com.example.ui.components.BatchSizeChip
import com.example.ui.components.CatalogCountChip
import com.example.ui.components.CineEase
import com.example.ui.components.LoadMoreBatchFooter
import com.example.ui.components.MovieCard
import com.example.ui.theme.CineTheme

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
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients

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
            .background(gradients.appBackdrop)
            .testTag("category_list_screen")
    ) {
        // Top Header Bar
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .background(gradients.topBarBackdrop)
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
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = CatalogBatchPolicy.formatAvailabilitySummary(
                                shownCount = displayedItems.size,
                                totalAvailable = effectiveTotal
                            ),
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }

                    // Sort Toggle
                    FilterChip(
                        selected = sortBy != "default",
                        onClick = {
                            sortBy = when (sortBy) {
                                "default" -> "rating"
                                "rating" -> "newest"
                                else -> "default"
                            }
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
                                    "rating" -> "Top Rated"
                                    "newest" -> "Newest"
                                    else -> "Featured"
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
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedType = key },
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
                        .background(colors.border.copy(alpha = 0.5f))
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MovieFilter,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No titles in $title",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Check back later or try clearing your category filter.",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
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
