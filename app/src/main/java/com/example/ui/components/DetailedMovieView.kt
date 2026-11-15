package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.ui.theme.CineTheme
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Reusable detailed movie view component displaying backdrop image,
 * plot summary, formatted release date, rating, genres, and optional actions.
 */
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

    val formattedRating = remember(media.voteAverage) {
        if (media.voteAverage > 0.0) {
            String.format(Locale.US, "%.1f / 10", media.voteAverage)
        } else {
            "Not Rated"
        }
    }

    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .testTag("detailed_movie_view")
    ) {
        // Backdrop Image Header
        Box(
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
            if (media.fullBackdropUrl.isNotBlank()) {
                AsyncImage(
                    model = media.fullBackdropUrl,
                    contentDescription = "${media.title} backdrop",
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

            // Gradient Fade Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.55f),
                            0.45f to Color.Transparent,
                            0.82f to colors.background.copy(alpha = 0.90f),
                            1.0f to colors.background
                        )
                    )
            )

            // Navigation / Dismiss button
            if (showBackButton && onDismissOrBack != null) {
                IconButton(
                    onClick = onDismissOrBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 12.dp, start = 12.dp)
                        .minimumInteractiveComponentSize()
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .testTag("detailed_movie_back_button")
                ) {
                    Icon(
                        imageVector = if (useCloseIcon) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (useCloseIcon) "Close Details" else "Back",
                        tint = Color.White
                    )
                }
            }

            // Media Type & Certification Badges in bottom-right of backdrop
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (media.certification.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .border(0.75.dp, colors.gold.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("detailed_movie_certification")
                    ) {
                        Text(
                            text = media.certification,
                            color = colors.gold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(gradients.softBadge)
                        .border(0.75.dp, colors.softRose.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (media.mediaType == "tv") "TV SERIES" else "MOVIE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        // Main Details Body
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Poster + Title & Genres Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(150.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .background(colors.surfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (media.fullPosterUrl.isNotBlank()) {
                        AsyncImage(
                            model = media.fullPosterUrl,
                            contentDescription = "${media.title} poster",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            lineHeight = 26.sp
                        ),
                        color = colors.textPrimary,
                        modifier = Modifier.testTag("detailed_movie_title")
                    )

                    if (media.genres.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = media.genres.joinToString(" • "),
                            color = colors.gold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Stream ID: ${media.streamId}",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Key Metadata Pills: Rating & Release Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Rating Card
                Surface(
                    color = colors.surfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .testTag("detailed_movie_rating")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = colors.gold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Rating",
                                fontSize = 10.sp,
                                color = colors.textSecondary
                            )
                            Text(
                                text = formattedRating,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                // Release Date Card
                Surface(
                    color = colors.surfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .testTag("detailed_movie_release_date")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Release Date",
                            tint = colors.softRose,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Release Date",
                                fontSize = 10.sp,
                                color = colors.textSecondary
                            )
                            Text(
                                text = formattedReleaseDate,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons Slot
            actionsContent()

            Spacer(modifier = Modifier.height(18.dp))

            // Plot Summary Section
            Text(
                text = "Plot Summary",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = media.overview.ifBlank { "No plot summary available for this title." },
                style = MaterialTheme.typography.bodyMedium.copy(
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
