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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CatalogBatchPolicy
import com.example.data.model.MediaItem
import com.example.data.model.TmdbFilterCatalog
import com.example.ui.components.BatchSizeChip
import com.example.ui.components.CatalogCountChip
import com.example.ui.components.CategoryPill
import com.example.ui.components.CineEase
import com.example.ui.components.CinetraEmptyState
import com.example.ui.components.LoadMoreBatchFooter
import com.example.ui.components.MovieCard
import com.example.ui.components.SearchResultsSkeletonGrid
import com.example.ui.theme.CinetraTheme

@Composable
fun LanguageCatalogScreen(
    languageCode: String,
    languageName: String,
    mediaItems: List<MediaItem>,
    selectedMediaType: String,
    sortBy: String,
    isLoading: Boolean,
    errorMessage: String?,
    totalAvailableCount: Int,
    batchSize: Int,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onMediaTypeChange: (String) -> Unit,
    onSortByChange: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onBatchSizeChange: (Int) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CinetraTheme.colors
    val langMeta = remember(languageCode) { TmdbFilterCatalog.browseLanguageByCode(languageCode) }
    val nativeSubtitle = langMeta?.nativeName?.takeIf { !it.equals(languageName, ignoreCase = true) }
    val badgeCode = langMeta?.badgeCode ?: languageCode.uppercase()

    val effectiveTotal = maxOf(totalAvailableCount, mediaItems.size)
    val isFiltered = selectedMediaType != "All"

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("language_catalog_screen")
    ) {
        // Header Bar
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
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("language_catalog_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeCode,
                            color = colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (nativeSubtitle != null) {
                                "$languageName · $nativeSubtitle"
                            } else {
                                "$languageName Titles"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isLoading && mediaItems.isEmpty()) {
                                "Loading $languageName movies & series..."
                            } else {
                                CatalogBatchPolicy.formatAvailabilitySummary(
                                    shownCount = mediaItems.size,
                                    totalAvailable = effectiveTotal
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    CategoryPill(
                        label = when (sortBy) {
                            "rating" -> "Top Rated"
                            "newest" -> "Latest"
                            else -> "Popular"
                        },
                        selected = sortBy != "popular",
                        leadingIcon = Icons.Default.FilterList,
                        onClick = {
                            val nextSort = when (sortBy) {
                                "popular" -> "rating"
                                "rating" -> "newest"
                                else -> "popular"
                            }
                            onSortByChange(nextSort)
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("language_catalog_sort_chip")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("All" to "All Titles", "movie" to "Movies", "tv" to "TV Series").forEach { (key, label) ->
                        CategoryPill(
                            label = label,
                            selected = selectedMediaType == key,
                            onClick = { onMediaTypeChange(key) },
                            modifier = Modifier.testTag("language_type_chip_$key")
                        )
                    }

                    BatchSizeChip(
                        batchSize = batchSize,
                        onToggleBatchSize = onBatchSizeChange,
                        testTag = "language_batch_size_chip"
                    )

                    CatalogCountChip(
                        totalAvailable = effectiveTotal,
                        isFiltered = isFiltered,
                        isLoading = isLoading,
                        testTag = "language_header_count_chip"
                    )
                }

                HorizontalDivider(thickness = 1.dp, color = colors.border)
            }
        }

        val contentState = when {
            isLoading && mediaItems.isEmpty() -> "loading"
            errorMessage != null && mediaItems.isEmpty() -> "error"
            mediaItems.isEmpty() -> "empty"
            else -> "content"
        }

        AnimatedContent(
            targetState = contentState,
            transitionSpec = {
                fadeIn(animationSpec = tween(220, easing = CineEase)) togetherWith
                    fadeOut(animationSpec = tween(160, easing = CineEase))
            },
            label = "language_catalog_content_transition",
            modifier = Modifier.fillMaxSize()
        ) { state ->
            when (state) {
                "loading" -> {
                    SearchResultsSkeletonGrid()
                }
                "error" -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CinetraEmptyState(
                            icon = Icons.Default.WifiOff,
                            title = "Couldn't Load $languageName Content",
                            subtitle = errorMessage ?: "Please check your connection and try again.",
                            ctaLabel = "Retry",
                            ctaTestTag = "language_retry_button",
                            onCtaClick = onRetry
                        )
                    }
                }
                "empty" -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CinetraEmptyState(
                            icon = Icons.Default.Language,
                            title = "No $languageName titles found",
                            subtitle = if (selectedMediaType != "All") {
                                "Try switching back to All Titles to see movies and series in $languageName."
                            } else {
                                "No catalog entries matched $languageName right now."
                            }
                        )
                    }
                }
                else -> {
                    val gridState = rememberLazyGridState()
                    val shouldAutoLoadMore by remember(hasMore, isLoadingMore, mediaItems.size) {
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
                            .testTag("language_catalog_grid")
                    ) {
                        items(
                            items = mediaItems,
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
                            key = "language_load_more_footer",
                            contentType = "load_more_footer"
                        ) {
                            LoadMoreBatchFooter(
                                shownCount = mediaItems.size,
                                totalAvailable = effectiveTotal,
                                batchSize = batchSize,
                                hasMore = hasMore,
                                isLoadingMore = isLoadingMore,
                                onLoadMore = onLoadMore,
                                testTag = "language_load_more_button"
                            )
                        }
                    }
                }
            }
        }
    }
}
