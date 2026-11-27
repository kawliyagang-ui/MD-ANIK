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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.ui.theme.CinetraTheme

@Composable
fun RankedPosterCard(
    rank: Int,
    media: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CinetraTheme.colors
    val posterShape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val darkPlaceholder = rememberDarkImagePainter(colors.surface)
    val yearText = remember(media.releaseDate) { media.releaseDate.take(4) }

    Column(
        modifier = modifier
            .width(168.dp)
            .cinePressScale(interactionSource = interactionSource)
            .clip(posterShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .testTag("top_ten_card_${rank}_${media.id}")
    ) {
        Box(
            modifier = Modifier
                .width(168.dp)
                .height(194.dp)
        ) {
            // Giant outlined red numeral behind/beside the left edge of the 2:3 poster
            Text(
                text = "$rank",
                style = TextStyle(
                    fontSize = if (rank >= 10) 76.sp else 92.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-5).sp,
                    color = colors.primary,
                    drawStyle = Stroke(
                        width = 4.5f,
                        join = StrokeJoin.Round
                    )
                ),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 0.dp, y = 12.dp)
            )

            // Clean 2:3 poster (128x192dp)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(128.dp)
                    .height(192.dp)
                    .clip(posterShape)
                    .background(colors.surface)
                    .border(1.dp, colors.border, posterShape)
            ) {
                if (media.fullPosterUrl.isNotBlank()) {
                    val imageRequest = rememberCineImageRequest(
                        url = media.fullPosterUrl,
                        widthDp = 128.dp,
                        heightDp = 192.dp
                    )
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "#$rank ${media.title}",
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
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = media.title,
            style = MaterialTheme.typography.bodyMedium, // Medium 13sp
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 40.dp)
        )

        if (yearText.isNotBlank() || media.voteAverage > 0.0) {
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 40.dp)
            ) {
                if (yearText.isNotBlank()) {
                    Text(
                        text = yearText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                        maxLines = 1
                    )
                }
                if (yearText.isNotBlank() && media.voteAverage > 0.0) {
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
                if (media.voteAverage > 0.0) {
                    RatingInlineLabel(
                        rating = media.voteAverage,
                        textColor = colors.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun TopTenCard(
    rank: Int,
    media: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RankedPosterCard(
        rank = rank,
        media = media,
        onClick = onClick,
        modifier = modifier
    )
}
