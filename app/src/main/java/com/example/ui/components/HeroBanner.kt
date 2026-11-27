package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.ui.theme.CinetraTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HeroBannerCarousel(
    items: List<MediaItem>,
    isItemInWatchlist: (String) -> Boolean,
    onWatchClick: (MediaItem) -> Unit,
    onWatchlistClick: (MediaItem) -> Unit,
    onTrailerClick: (MediaItem) -> Unit,
    onCardClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val colors = CinetraTheme.colors
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress, items.size) {
        if (items.size > 1 && !pagerState.isScrollInProgress) {
            delay(5500L)
            val nextPage = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(
                page = nextPage,
                animationSpec = tween(durationMillis = 550, easing = CineEase)
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 12.dp,
            key = { index -> items[index].id },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hero_banner_pager")
        ) { page ->
            val media = items[page]
            val isCurrentPage = page == pagerState.currentPage
            HeroBanner(
                media = media,
                rank = page + 1,
                isInWatchlist = isItemInWatchlist(media.id),
                onWatchClick = { onWatchClick(media) },
                onWatchlistClick = { onWatchlistClick(media) },
                onTrailerClick = { onTrailerClick(media) },
                onCardClick = { onCardClick(media) },
                attachTestTags = isCurrentPage
            )
        }

        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 2.dp)
                    .testTag("hero_pager_indicators"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(items.size) { index ->
                    val isSelected = index == pagerState.currentPage
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 20.dp else 6.dp,
                        animationSpec = tween(durationMillis = 240, easing = CineEase),
                        label = "heroDotWidth"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isSelected) colors.primary else colors.textMuted.copy(alpha = 0.45f),
                        animationSpec = tween(durationMillis = 240, easing = CineEase),
                        label = "heroDotColor"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(5.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        page = index,
                                        animationSpec = tween(durationMillis = 420, easing = CineEase)
                                    )
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun HeroBanner(
    media: MediaItem,
    isInWatchlist: Boolean,
    onWatchClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onTrailerClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    rank: Int = 1,
    attachTestTags: Boolean = true
) {
    val colors = CinetraTheme.colors
    val gradients = CinetraTheme.gradients
    val heroShape = RoundedCornerShape(20.dp)
    val cardInteraction = remember { MutableInteractionSource() }
    val darkPlaceholder = rememberDarkImagePainter(colors.surface)
    val fadeOverlayBrush = rememberVerticalGradientBrush(
        colors.background.copy(alpha = 0.0f),
        colors.background.copy(alpha = 0.25f),
        colors.background.copy(alpha = 0.78f),
        colors.background.copy(alpha = 0.98f)
    )
    val yearText = remember(media.releaseDate) { media.releaseDate.take(4) }
    val firstGenre = remember(media.genres) { media.genres.firstOrNull().orEmpty() }
    val imageUrl = remember(media.fullPosterUrl, media.fullBackdropUrl) {
        media.fullPosterUrl.ifBlank { media.fullBackdropUrl }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .aspectRatio(3f / 4f)
                .clip(heroShape)
                .background(colors.surface)
                .border(1.dp, colors.border, heroShape)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = ripple(),
                    onClick = onCardClick
                )
                .then(if (attachTestTags) Modifier.testTag("hero_banner_card") else Modifier)
        ) {
            val targetHeight = maxWidth * (4f / 3f)
            if (imageUrl.isNotBlank()) {
                val posterRequest = rememberCineImageRequest(
                    url = imageUrl,
                    widthDp = maxWidth,
                    heightDp = targetHeight,
                    crossfadeMs = 250
                )
                AsyncImage(
                    model = posterRequest,
                    contentDescription = media.title,
                    placeholder = darkPlaceholder,
                    error = darkPlaceholder,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradients.appBackdrop)
                )
            }

            // Bottom scrim fade into pure black #000000
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fadeOverlayBrush)
            )

            // Bottom Content: rank pill + bold 22sp title + single meta line
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.primary)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "#$rank this week",
                        color = colors.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = media.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp
                    ),
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (media.voteAverage > 0.0 || yearText.isNotBlank() || firstGenre.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var hasPrefix = false
                        if (media.voteAverage > 0.0) {
                            RatingInlineLabel(
                                rating = media.voteAverage,
                                fontSize = 12.sp,
                                textColor = colors.textPrimary,
                                iconSize = 13.dp
                            )
                            hasPrefix = true
                        }
                        if (yearText.isNotBlank()) {
                            if (hasPrefix) {
                                Text(
                                    text = " • ",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                    color = colors.textSecondary
                                )
                            }
                            Text(
                                text = yearText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = colors.textSecondary,
                                maxLines = 1
                            )
                            hasPrefix = true
                        }
                        if (firstGenre.isNotBlank()) {
                            if (hasPrefix) {
                                Text(
                                    text = " • ",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                    color = colors.textSecondary
                                )
                            }
                            Text(
                                text = firstGenre,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
