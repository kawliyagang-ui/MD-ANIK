package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.ui.theme.CinetraTheme
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailedMovieView(
    media: MediaItem,
    modifier: Modifier = Modifier,
    backdropHeight: Dp? = null,
    showBackButton: Boolean = true,
    useCloseIcon: Boolean = false,
    onDismissOrBack: (() -> Unit)? = null,
    actionsContent: @Composable ColumnScope.() -> Unit = {}
) {
    val formattedReleaseDate = remember(media.releaseDate) {
        formatReleaseDate(media.releaseDate)
    }

    val yearText = remember(media.releaseDate) {
        media.releaseDate.take(4).ifBlank { formattedReleaseDate }
    }

    val colors = CinetraTheme.colors
    val gradients = CinetraTheme.gradients
    val darkPlaceholder = rememberDarkImagePainter(colors.surface)
    val backdropFadeBrush = rememberVerticalGradientBrush(
        colors.background.copy(alpha = 0.20f),
        colors.background.copy(alpha = 0.45f),
        colors.background.copy(alpha = 0.88f),
        colors.background
    )

    val heroReveal = remember(media.id) { Animatable(0f) }
    LaunchedEffect(media.id) {
        heroReveal.snapTo(0f)
        heroReveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 260, easing = CineEase)
        )
    }

    val resolvedBackdropHeight = backdropHeight ?: 240.dp
    val posterOverlapOffset = 48.dp
    val posterCardShape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .testTag("detailed_movie_view")
    ) {
        // Backdrop + Overlapping Poster Thumbnail & Title Header
        Box(modifier = Modifier.fillMaxWidth()) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (backdropHeight != null) {
                            Modifier.height(backdropHeight)
                        } else {
                            Modifier.aspectRatio(16f / 9f)
                        }
                    )
            ) {
                val targetBackdropWidth = maxWidth
                val targetBackdropHeight = backdropHeight ?: (targetBackdropWidth * 9f / 16f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = heroReveal.value
                            alpha = p
                            val s = 0.98f + (0.02f * p)
                            scaleX = s
                            scaleY = s
                        }
                ) {
                    val imageUrl = media.fullBackdropUrl.ifBlank { media.fullPosterUrl }
                    if (imageUrl.isNotBlank()) {
                        val backdropReq = rememberCineImageRequest(
                            url = imageUrl,
                            widthDp = targetBackdropWidth,
                            heightDp = targetBackdropHeight,
                            crossfadeMs = 240
                        )
                        AsyncImage(
                            model = backdropReq,
                            contentDescription = "${media.title} backdrop",
                            placeholder = darkPlaceholder,
                            error = darkPlaceholder,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("detailed_movie_backdrop")
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(gradients.appBackdrop)
                                .testTag("detailed_movie_backdrop")
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backdropFadeBrush)
                    )
                }

                if (showBackButton && onDismissOrBack != null) {
                    GlassIconButton(
                        onClick = onDismissOrBack,
                        icon = if (useCloseIcon) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (useCloseIcon) "Close Details" else "Back",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(top = 10.dp, start = 14.dp)
                            .testTag("detailed_movie_back_button")
                    )
                }
            }

            // Overlapping Poster (2:3) + Title + Single Meta Line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = (resolvedBackdropHeight - posterOverlapOffset).coerceAtLeast(120.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 96.dp, height = 144.dp)
                        .clip(posterCardShape)
                        .background(colors.surface)
                        .border(1.dp, colors.border, posterCardShape),
                    contentAlignment = Alignment.Center
                ) {
                    val posterUrl = media.fullPosterUrl.ifBlank { media.fullBackdropUrl }
                    if (posterUrl.isNotBlank()) {
                        val posterReq = rememberCineImageRequest(
                            url = posterUrl,
                            widthDp = 96.dp,
                            heightDp = 144.dp,
                            crossfadeMs = 220
                        )
                        AsyncImage(
                            model = posterReq,
                            contentDescription = "${media.title} poster",
                            placeholder = darkPlaceholder,
                            error = darkPlaceholder,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 4.dp)
                ) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 25.sp
                        ),
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("detailed_movie_title")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Single crisp meta row: ★ Rating • Year • Certification
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var hasPrefix = false
                        if (media.voteAverage > 0.0) {
                            RatingInlineLabel(
                                rating = media.voteAverage,
                                fontSize = 12.sp,
                                textColor = colors.textPrimary,
                                iconSize = 13.dp,
                                modifier = Modifier.testTag("detailed_movie_rating")
                            )
                            hasPrefix = true
                        }

                        if (yearText.isNotBlank() && yearText != "TBA") {
                            if (hasPrefix) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textMuted
                                )
                            }
                            Text(
                                text = yearText,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary,
                                modifier = Modifier.testTag("detailed_movie_release_date")
                            )
                            hasPrefix = true
                        }

                        if (media.certification.isNotBlank()) {
                            if (hasPrefix) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textMuted
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                    .testTag("detailed_movie_certification")
                            ) {
                                Text(
                                    text = media.certification,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Genre Chips + Action Buttons + Synopsis
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (media.genres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    media.genres.take(5).forEach { genre ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(colors.surfaceElevated)
                                .border(1.dp, colors.border, RoundedCornerShape(50))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Play button + 2-column ActionCard grid slot
            actionsContent()

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Synopsis",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = media.overview.ifBlank { "No plot summary available for this title." },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                ),
                color = colors.textSecondary,
                modifier = Modifier.testTag("detailed_movie_plot_summary")
            )
        }
    }
}

fun formatReleaseDate(rawDate: String): String {
    val trimmed = rawDate.trim()
    if (trimmed.isEmpty()) return "TBA"
    return try {
        if (trimmed.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val parsed = parser.parse(trimmed)
            if (parsed != null) formatter.format(parsed) else trimmed
        } else {
            trimmed
        }
    } catch (_: Exception) {
        trimmed
    }
}
