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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.data.model.WatchlistEntity
import com.example.data.model.toMediaItem
import com.example.ui.components.CineGradientButton
import com.example.ui.components.MovieCard
import com.example.ui.theme.CineTheme

@Composable
fun WatchlistScreen(
    watchlist: List<WatchlistEntity>,
    onMediaClick: (MediaItem) -> Unit,
    onRemoveFromWatchlist: (MediaItem) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
    onWatchClick: (MediaItem) -> Unit = onMediaClick
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
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
            .background(gradients.appBackdrop)
            .testTag("watchlist_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = colors.gold,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "My Watchlist",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Text(
                    text = "${watchlist.size} saved titles • Posters, ratings & instant resume",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        if (watchlist.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("All" to "All (${watchlist.size})", "movie" to "Movies", "tv" to "TV Series").forEach { (key, label) ->
                    val selected = typeFilter == key
                    FilterChip(
                        selected = selected,
                        onClick = { typeFilter = key },
                        label = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accentMid,
                            selectedLabelColor = Color.White,
                            containerColor = colors.surfaceElevated,
                            labelColor = colors.textPrimary
                        )
                    )
                }

                FilterChip(
                    selected = sortBy != "recent",
                    onClick = {
                        sortBy = when (sortBy) {
                            "recent" -> "rating"
                            "rating" -> "newest"
                            else -> "recent"
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
                                "rating" -> "Highest Rated"
                                "newest" -> "Newest Release"
                                else -> "Recently Added"
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
                    )
                )
            }
        }

        if (watchlist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your Watchlist is Empty",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Save movies and TV series to watch later. TV shows in your watchlist also track new episodes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    CineGradientButton(
                        onClick = onExploreClick,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("explore_catalog_from_watchlist")
                    ) {
                        Text("Browse Movies & Series", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = displayedItems,
                    key = { it.id },
                    contentType = { "watchlist_card" }
                ) { item ->
                    val media = item.toMediaItem()
                    Column {
                        MovieCard(
                            media = media,
                            onClick = { onMediaClick(media) },
                            cardWidth = 144.dp,
                            cardHeight = 205.dp,
                            modifier = Modifier.animateItem()
                        )
                        Row(
                            modifier = Modifier.width(144.dp).padding(top = 4.dp),
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
                                    tint = colors.gold,
                                    modifier = Modifier.size(20.dp)
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
                                    tint = colors.softRose,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
