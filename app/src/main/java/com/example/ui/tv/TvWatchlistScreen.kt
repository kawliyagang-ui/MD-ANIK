package com.example.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.example.data.model.MediaItem
import com.example.data.model.WatchlistEntity
import com.example.data.model.toMediaItem
import com.example.ui.theme.CineTheme
import kotlinx.coroutines.delay

@Composable
fun TvWatchlistScreen(
    watchlist: List<WatchlistEntity>,
    onMediaClick: (MediaItem) -> Unit,
    onBrowseHomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    var typeFilter by remember { mutableStateOf("All") }

    val displayedItems = remember(watchlist, typeFilter) {
        when (typeFilter) {
            "movie" -> watchlist.filter { it.mediaType.equals("movie", ignoreCase = true) }
            "tv" -> watchlist.filter { it.mediaType.equals("tv", ignoreCase = true) }
            else -> watchlist
        }.map { it.toMediaItem() }
    }

    val firstFocusRequester = remember { FocusRequester() }
    LaunchedEffect(watchlist.isEmpty()) {
        delay(50L)
        runCatching { firstFocusRequester.requestFocus() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradients.appBackdrop)
            .padding(horizontal = TvOverscanHorizontal, vertical = 12.dp)
            .testTag("tv_watchlist_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "My List",
                    color = colors.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${watchlist.size} saved titles",
                    color = colors.gold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (watchlist.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val options = listOf("All" to "All (${watchlist.size})", "movie" to "Movies", "tv" to "TV Series")
                items(options.size) { index ->
                    val (key, label) = options[index]
                    TvFilterPill(
                        text = label,
                        selected = typeFilter == key,
                        onClick = { typeFilter = key },
                        modifier = if (index == 0) Modifier.focusRequester(firstFocusRequester) else Modifier
                    )
                }
            }
        }

        if (displayedItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                        .padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = colors.gold,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Your My List is Empty",
                        color = colors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add movies or TV shows from Home or Details to find them quickly here.",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                    TvActionButton(
                        text = "Browse Home",
                        onClick = onBrowseHomeClick,
                        modifier = Modifier.focusRequester(firstFocusRequester)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 154.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 6.dp, bottom = TvOverscanVertical + 20.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(
                    items = displayedItems,
                    key = { "tv_wl_${it.id}" },
                    contentType = { "tv_poster_card" }
                ) { item ->
                    TvPosterCard(
                        media = item,
                        onClick = { onMediaClick(item) }
                    )
                }
            }
        }
    }
}
