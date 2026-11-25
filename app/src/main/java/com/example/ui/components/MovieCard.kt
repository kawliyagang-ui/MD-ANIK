package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.ui.theme.CineTheme
import java.util.Locale

val LocalDownloadedMediaIds = staticCompositionLocalOf<Set<String>> { emptySet() }

private val CardShape = RoundedCornerShape(12.dp)
private val BadgeShape = RoundedCornerShape(4.dp)
private val CertShape = RoundedCornerShape(3.dp)

@Composable
fun MovieCard(
    media: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 140.dp,
    cardHeight: Dp = 210.dp,
    progressPercent: Int? = null,
    episodeLabel: String? = null
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    val isDownloaded = LocalDownloadedMediaIds.current.contains(media.id)
    val interactionSource = remember { MutableInteractionSource() }
    val darkPlaceholder = rememberDarkImagePainter(colors.surfaceElevated)
    val overlayBrush = rememberVerticalGradientBrush(
        Color.Transparent,
        Color.Transparent,
        Color.Black.copy(alpha = 0.85f)
    )
    val progressBackdropBrush = rememberVerticalGradientBrush(
        Color.Transparent,
        Color.Black.copy(alpha = 0.72f)
    )

    val typeBadgeText = remember(media.mediaType, media.seasons.size, media.numberOfSeasons) {
        if (media.mediaType == "tv") {
            if (media.seasons.isNotEmpty() || media.numberOfSeasons > 1) {
                "TV • ${media.numberOfSeasons}S"
            } else {
                "TV"
            }
        } else {
            "MOVIE"
        }
    }
    val ratingText = remember(media.voteAverage) {
        if (media.voteAverage > 0.0) String.format(Locale.US, "%.1f", media.voteAverage) else ""
    }
    val metaLineText = remember(media.releaseDate, media.genres) {
        val year = media.releaseDate.take(4)
        val genre = media.genres.firstOrNull().orEmpty()
        when {
            year.isNotBlank() && genre.isNotBlank() -> "$year • $genre"
            year.isNotBlank() -> year
            genre.isNotBlank() -> genre
            else -> ""
        }
    }

    Column(
        modifier = modifier
            .width(cardWidth)
            .cinePressScale(interactionSource = interactionSource)
            .clip(CardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .testTag("movie_card_${media.id}")
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .clip(CardShape)
                .background(colors.surfaceElevated)
                .border(1.dp, colors.border.copy(alpha = 0.7f), CardShape)
        ) {
            if (media.fullPosterUrl.isNotBlank()) {
                val imageRequest = rememberCineImageRequest(
                    url = media.fullPosterUrl,
                    widthDp = cardWidth,
                    heightDp = cardHeight
                )
                AsyncImage(
                    model = imageRequest,
                    contentDescription = media.title,
                    placeholder = darkPlaceholder,
                    error = darkPlaceholder,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Bottom subtle gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayBrush)
            )

            // Top Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(BadgeShape)
                        .then(
                            if (media.mediaType == "tv") {
                                Modifier.background(gradients.softBadge)
                            } else {
                                Modifier.background(Color.Black.copy(alpha = 0.75f))
                            }
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = typeBadgeText,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (ratingText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(BadgeShape)
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = colors.gold,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = ratingText,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Bottom Continue Watching label and progress bar if applicable
            if (progressPercent != null || episodeLabel != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(progressBackdropBrush)
                ) {
                    if (episodeLabel != null) {
                        Text(
                            text = episodeLabel,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (progressPercent != null && progressPercent > 0) {
                        CineGradientProgressBar(
                            progress = (progressPercent / 100f).coerceIn(0f, 1f),
                            height = 4.dp,
                            cornerRadius = 0.dp,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (isDownloaded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = 6.dp,
                            bottom = if (progressPercent != null || episodeLabel != null) 28.dp else 6.dp
                        )
                        .clip(BadgeShape)
                        .background(colors.success.copy(alpha = 0.9f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                        .testTag("poster_downloaded_badge_${media.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = "Downloaded",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Downloaded",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = media.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (media.certification.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(CertShape)
                        .border(0.75.dp, colors.gold.copy(alpha = 0.7f), CertShape)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = media.certification,
                        color = colors.gold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
            }
            if (metaLineText.isNotEmpty()) {
                Text(
                    text = metaLineText,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
