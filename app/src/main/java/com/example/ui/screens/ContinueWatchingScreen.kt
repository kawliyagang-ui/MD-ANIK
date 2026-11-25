package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ContinueWatchingEntity
import com.example.data.model.ContinueWatchingPolicy
import com.example.data.model.MediaItem
import com.example.data.model.mergeWithCatalog
import com.example.data.model.toMediaItem
import com.example.ui.components.CineGradientButton
import com.example.ui.components.CineGradientProgressBar
import com.example.ui.components.cinePressScale
import com.example.ui.components.rememberCineImageRequest
import com.example.ui.components.rememberDarkImagePainter
import com.example.ui.theme.CineTheme

@Composable
fun ContinueWatchingScreen(
    items: List<ContinueWatchingEntity>,
    onResumeWatching: (MediaItem, Int, Int) -> Unit,
    onRemoveItem: (String) -> Unit,
    onBrowseClick: () -> Unit,
    modifier: Modifier = Modifier,
    allCatalogItems: List<MediaItem> = emptyList(),
    onMediaClick: (MediaItem) -> Unit = { media -> onResumeWatching(media, 1, 1) }
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    val catalogById = remember(allCatalogItems) {
        allCatalogItems.associateBy { it.id }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradients.appBackdrop)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = colors.gold,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Continue Watching",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Text(
                    text = "${items.size} in-progress streams • Automatic progress tracker",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Streams In Progress",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Start playing any movie or TV series and Cinetra will automatically remember your place.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    CineGradientButton(
                        onClick = onBrowseClick,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("browse_catalog_from_continue_watching")
                    ) {
                        Text("Browse Catalog", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = items,
                    key = { it.id },
                    contentType = { "continue_item" }
                ) { item ->
                    val media = remember(item, catalogById) {
                        item.toMediaItem().mergeWithCatalog(catalogById[item.id])
                    }
                    val (safeSeason, safeEpisode) = remember(item.mediaType, item.season, item.episode) {
                        ContinueWatchingPolicy.normalizeSeasonEpisode(
                            mediaType = item.mediaType,
                            season = item.season,
                            episode = item.episode
                        )
                    }
                    val watchedLabel = remember(item.watchedSeconds) {
                        ContinueWatchingPolicy.formatWatchedDuration(item.watchedSeconds)
                    }
                    val relativeTime = remember(item.lastWatchedAt) {
                        ContinueWatchingPolicy.formatRelativeTime(item.lastWatchedAt)
                    }
                    val releaseYear = remember(media.releaseDate) { media.releaseDate.take(4) }
                    val subtitleText = remember(item.mediaType, safeSeason, safeEpisode, releaseYear) {
                        if (item.mediaType == "tv") {
                            val seasonPrefix = if (safeSeason == 0) "Specials" else "Season $safeSeason"
                            "$seasonPrefix • Episode $safeEpisode"
                        } else {
                            if (releaseYear.isNotBlank()) "Feature Movie • $releaseYear" else "Feature Movie"
                        }
                    }
                    val statusText = remember(watchedLabel, relativeTime) {
                        if (watchedLabel.isNotBlank()) {
                            "Watched $watchedLabel • $relativeTime"
                        } else {
                            "Started • $relativeTime"
                        }
                    }
                    val cardInteraction = remember { MutableInteractionSource() }
                    val posterDarkPlaceholder = rememberDarkImagePainter(colors.surface)

                    Surface(
                        color = colors.surfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .cinePressScale(cardInteraction)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = cardInteraction,
                                indication = ripple(bounded = true)
                            ) { onMediaClick(media) }
                            .testTag("continue_card_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Poster thumbnail (tap play badge to resume immediately)
                            Box(
                                modifier = Modifier
                                    .size(width = 65.dp, height = 95.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surface)
                                    .clickable { onResumeWatching(media, safeSeason, safeEpisode) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (media.fullPosterUrl.isNotBlank()) {
                                    val posterRequest = rememberCineImageRequest(
                                        url = media.fullPosterUrl,
                                        widthDp = 65.dp,
                                        heightDp = 95.dp
                                    )
                                    AsyncImage(
                                        model = posterRequest,
                                        placeholder = posterDarkPlaceholder,
                                        error = posterDarkPlaceholder,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = item.title,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .align(Alignment.Center)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details & Progress
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = subtitleText,
                                    fontSize = 12.sp,
                                    color = colors.gold,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                if (item.progressPercent > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CineGradientProgressBar(
                                            progress = (item.progressPercent / 100f).coerceIn(0f, 1f),
                                            height = 4.dp,
                                            cornerRadius = 2.dp,
                                            trackColor = colors.border,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${item.progressPercent}% • $relativeTime",
                                            fontSize = 11.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                } else {
                                    Text(
                                        text = statusText,
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            // Delete button
                            IconButton(
                                onClick = { onRemoveItem(item.id) },
                                modifier = Modifier.testTag("delete_continue_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove from history",
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
