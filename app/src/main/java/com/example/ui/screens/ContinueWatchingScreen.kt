package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.components.CinetraEmptyState
import com.example.ui.components.StylishProgressBar
import com.example.ui.components.cinePressScale
import com.example.ui.components.rememberCineImageRequest
import com.example.ui.components.rememberDarkImagePainter
import com.example.ui.theme.CinetraTheme

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
    val colors = CinetraTheme.colors
    val bottomClearance = CinetraTheme.bottomBarClearance
    val catalogById = remember(allCatalogItems) {
        allCatalogItems.associateBy { it.id }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
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
                tint = colors.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Continue Watching",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                Text(
                    text = "${items.size} in-progress streams",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomClearance),
                contentAlignment = Alignment.Center
            ) {
                CinetraEmptyState(
                    icon = Icons.Default.History,
                    title = "No Streams In Progress",
                    subtitle = "Start playing any movie or TV series and Cinetra will automatically remember your place.",
                    ctaLabel = "Browse Catalog",
                    ctaTestTag = "browse_catalog_from_continue_watching",
                    onCtaClick = onBrowseClick
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = bottomClearance + 24.dp
                ),
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
                    val remainingMin = remember(item.durationSeconds, item.watchedSeconds) {
                        if (item.durationSeconds > item.watchedSeconds && item.durationSeconds > 0L) {
                            ((item.durationSeconds - item.watchedSeconds) / 60L).coerceAtLeast(1L)
                        } else {
                            null
                        }
                    }
                    val subtitleText = remember(item.mediaType, safeSeason, safeEpisode, releaseYear, remainingMin) {
                        val prefix = if (item.mediaType == "tv") {
                            "S$safeSeason:E$safeEpisode"
                        } else {
                            releaseYear.ifBlank { "Movie" }
                        }
                        if (remainingMin != null) "$prefix • ${remainingMin}m left" else prefix
                    }
                    val statusText = remember(watchedLabel, relativeTime) {
                        if (watchedLabel.isNotBlank()) {
                            "Watched $watchedLabel • $relativeTime"
                        } else {
                            "Started • $relativeTime"
                        }
                    }
                    val cardInteraction = remember { MutableInteractionSource() }
                    val thumbUrl = remember(media.fullBackdropUrl, media.fullPosterUrl) {
                        media.fullBackdropUrl.ifBlank { media.fullPosterUrl }
                    }
                    val darkPlaceholder = rememberDarkImagePainter(colors.surfaceElevated)

                    Surface(
                        color = colors.surface,
                        shape = MaterialTheme.shapes.medium, // 14.dp
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .cinePressScale(cardInteraction)
                            .clip(MaterialTheme.shapes.medium)
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
                            // 16:9 landscape thumbnail
                            Box(
                                modifier = Modifier
                                    .size(width = 128.dp, height = 72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surfaceElevated)
                                    .clickable { onResumeWatching(media, safeSeason, safeEpisode) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (thumbUrl.isNotBlank()) {
                                    val thumbReq = rememberCineImageRequest(
                                        url = thumbUrl,
                                        widthDp = 128.dp,
                                        heightDp = 72.dp
                                    )
                                    AsyncImage(
                                        model = thumbReq,
                                        placeholder = darkPlaceholder,
                                        error = darkPlaceholder,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = item.title,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .align(Alignment.Center)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume",
                                        tint = colors.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details & Meta
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    ),
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (item.progressPercent > 0) {
                                    Spacer(modifier = Modifier.height(5.dp))
                                    StylishProgressBar(
                                        progress = (item.progressPercent / 100f).coerceIn(0f, 1f),
                                        label = "${item.progressPercent}%",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                Text(
                                    text = if (item.progressPercent > 0) relativeTime else statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { onRemoveItem(item.id) },
                                modifier = Modifier.testTag("delete_continue_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove from history",
                                    tint = colors.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
