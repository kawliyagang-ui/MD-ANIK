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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.CineTheme
import java.util.Locale

private val TopTenPosterShape = RoundedCornerShape(14.dp)
private val TopTenBadgeShape = RoundedCornerShape(4.dp)

@Composable
fun TopTenCard(
    rank: Int,
    media: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    val interactionSource = remember { MutableInteractionSource() }
    val darkPlaceholder = rememberDarkImagePainter(colors.surfaceElevated)
    val posterFadeBrush = rememberVerticalGradientBrush(
        Color.Transparent,
        Color.Transparent,
        Color.Black.copy(alpha = 0.85f)
    )
    val numeralBrush = rememberVerticalGradientBrush(
        colors.textPrimary,
        colors.softRose
    )
    val ratingText = remember(media.voteAverage) {
        if (media.voteAverage > 0.0) String.format(Locale.US, "%.1f", media.voteAverage) else ""
    }

    Column(
        modifier = modifier
            .width(166.dp)
            .cinePressScale(interactionSource = interactionSource)
            .clip(TopTenPosterShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .testTag("top_ten_card_${rank}_${media.id}")
    ) {
        Box(
            modifier = Modifier
                .width(166.dp)
                .height(198.dp)
        ) {
            // Poster shifted right so bold rank numeral sits in front on the bottom-left
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(130.dp)
                    .height(192.dp)
                    .clip(TopTenPosterShape)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border.copy(alpha = 0.8f), TopTenPosterShape)
            ) {
                if (media.fullPosterUrl.isNotBlank()) {
                    val imageRequest = rememberCineImageRequest(
                        url = media.fullPosterUrl,
                        widthDp = 130.dp,
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
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(posterFadeBrush)
                )

                // Top badges on poster
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(TopTenBadgeShape)
                            .background(gradients.softBadge)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "#$rank",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (ratingText.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(TopTenBadgeShape)
                                .background(Color.Black.copy(alpha = 0.72f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = colors.gold,
                                    modifier = Modifier.size(9.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = ratingText,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Large overlapping rank numeral
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-2).dp, y = 8.dp)
            ) {
                Text(
                    text = "$rank",
                    style = TextStyle(
                        fontSize = if (rank >= 10) 64.sp else 74.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-4).sp,
                        color = colors.background,
                        drawStyle = Stroke(width = 9f)
                    )
                )
                Text(
                    text = "$rank",
                    style = TextStyle(
                        fontSize = if (rank >= 10) 64.sp else 74.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-4).sp,
                        brush = numeralBrush
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = media.title,
            color = colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 34.dp)
        )
    }
}
