package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaItem
import com.example.data.model.WatchlistEntity
import com.example.data.model.toMediaItem
import com.example.ui.components.CategoryPill
import com.example.ui.components.CinetraEmptyState
import com.example.ui.components.MovieCard
import com.example.ui.theme.CinetraTheme

@Composable
fun WatchlistScreen(
    watchlist: List<WatchlistEntity>,
    onMediaClick: (MediaItem) -> Unit,
    onRemoveFromWatchlist: (MediaItem) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
    onWatchClick: (MediaItem) -> Unit = onMediaClick
) {
    val colors = CinetraTheme.colors
    val bottomClearance = CinetraTheme.bottomBarClearance
    var typeFilter by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("recent") }

    val displayedItems = remember(watchlist, typeFilter, sortBy) {
        val filtered = when (typeFilter) {
            "movie" -> watchlist.filter { it.mediaType.equals("movie", ignoreCase = true) }
            "tv" -> watchlist.filter { it.mediaType.equals("tv", ignoreCase = true) }
            else -> watchlist
        }
        when (sortBy) {
            "rating" -> filtered.sortedByDescending { it.voteAverage }
            "newest" -> filtered.sortedByDescending { it.releaseDate }
            else -> filtered
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag("watchlist_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Saved",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                Text(
                    text = "${watchlist.size} saved titles",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
        }

        if (watchlist.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("All" to "All (${watchlist.size})", "movie" to "Movies", "tv" to "TV Series").forEach { (key, label) ->
                    val selected = typeFilter == key
                    CategoryPill(
                        label = label,
                        selected = selected,
                        onClick = { typeFilter = key }
                    )
                }

                CategoryPill(
                    label = when (sortBy) {
                        "rating" -> "Highest Rated"
                        "newest" -> "Newest Release"
                        else -> "Recently Added"
                    },
                    selected = sortBy != "recent",
                    leadingIcon = Icons.Default.FilterList,
                    onClick = {
                        sortBy = when (sortBy) {
                            "recent" -> "rating"
                            "rating" -> "newest"
                            else -> "recent"
                        }
                    }
                )
            }
        }

        if (watchlist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomClearance),
                contentAlignment = Alignment.Center
            ) {
                CinetraEmptyState(
                    icon = Icons.Default.Bookmark,
                    title = "Your Watchlist is Empty",
                    subtitle = "Save movies and TV series to watch later. TV shows in your watchlist also track new episodes.",
                    ctaLabel = "Browse Movies & Series",
                    ctaTestTag = "explore_catalog_from_watchlist",
                    onCtaClick = onExploreClick
                )
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columnCount = if (maxWidth >= 600.dp) 4 else 3
                val horizontalSpacing = 10.dp
                val totalGap = 32.dp + horizontalSpacing * (columnCount - 1)
                val colWidth = ((maxWidth - totalGap) / columnCount).coerceAtLeast(96.dp)
                val colHeight = colWidth * 1.5f

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = bottomClearance + 24.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = displayedItems,
                        key = { it.id },
                        contentType = { "watchlist_card" }
                    ) { item ->
                        val media = item.toMediaItem()
                        Column(modifier = Modifier.width(colWidth)) {
                            MovieCard(
                                media = media,
                                onClick = { onMediaClick(media) },
                                cardWidth = colWidth,
                                cardHeight = colHeight,
                                showTypePill = typeFilter == "All",
                                modifier = Modifier.animateItem()
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.border, RoundedCornerShape(10.dp)),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onWatchClick(media) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("watch_watchlist_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Watch ${media.title}",
                                        tint = colors.primary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveFromWatchlist(media) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("remove_watchlist_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove ${media.title}",
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(17.dp)
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
