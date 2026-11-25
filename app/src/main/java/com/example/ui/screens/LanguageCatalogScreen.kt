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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CatalogBatchPolicy
import com.example.data.model.MediaItem
import com.example.data.model.TmdbFilterCatalog
import com.example.ui.components.BatchSizeChip
import com.example.ui.components.CatalogCountChip
import com.example.ui.components.CineEase
import com.example.ui.components.CineGradientButton
import com.example.ui.components.LoadMoreBatchFooter
import com.example.ui.components.MovieCard
import com.example.ui.components.SearchResultsSkeletonGrid
import com.example.ui.theme.CineTheme

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
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    val langMeta = remember(languageCode) { TmdbFilterCatalog.browseLanguageByCode(languageCode) }
    val nativeSubtitle = langMeta?.nativeName?.takeIf { !it.equals(languageName, ignoreCase = true) }
    val badgeCode = langMeta?.badgeCode ?: languageCode.uppercase()

    val effectiveTotal = maxOf(totalAvailableCount, mediaItems.size)
    val isFiltered = selectedMediaType != "All"

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradients.appBackdrop)
            .testTag("language_catalog_screen")
    ) {
        // Header Bar
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
                        modifier = Modifier.testTag("language_catalog_back_button")
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
                            .background(gradients.softBadge)
                            .border(1.dp, colors.softRose.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeCode,
                            color = Color.White,
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
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
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
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }

                    FilterChip(
                        selected = sortBy != "popular",
                        onClick = {
                            val nextSort = when (sortBy) {
                                "popular" -> "rating"
                                "rating" -> "newest"
                                else -> "popular"
                            }
                            onSortByChange(nextSort)
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
                                    "newest" -> "Latest"
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
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("All" to "All Titles", "movie" to "Movies", "tv" to "TV Series").forEach { (key, label) ->
                        val isSelected = selectedMediaType == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { onMediaTypeChange(key) },
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

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border.copy(alpha = 0.5f))
                )
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
                                text = "Couldn't Load $languageName Content",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage ?: "Please check your connection and try again.",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            CineGradientButton(
                                onClick = onRetry,
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("language_retry_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                "empty" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No $languageName titles found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (selectedMediaType != "All") {
                                    "Try switching back to All Titles to see movies and series in $languageName."
                                } else {
                                    "No catalog entries matched $languageName right now."
                                },
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
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
