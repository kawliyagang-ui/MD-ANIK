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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.ui.theme.CineTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private val HeroBadgeShape = RoundedCornerShape(6.dp)
private val HeroSmallBadgeShape = RoundedCornerShape(4.dp)
private val HeroButtonShape = RoundedCornerShape(8.dp)

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
    val colors = CineTheme.colors
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
            key = { index -> items[index].id },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hero_banner_pager")
        ) { page ->
            val media = items[page]
            val isCurrentPage = page == pagerState.currentPage
            HeroBanner(
                media = media,
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
                    .padding(top = 6.dp, bottom = 4.dp)
                    .testTag("hero_pager_indicators"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(items.size) { index ->
                    val isSelected = index == pagerState.currentPage
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 22.dp else 6.dp,
                        animationSpec = tween(durationMillis = 240, easing = CineEase),
                        label = "heroDotWidth"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isSelected) colors.accentMid else colors.textMuted.copy(alpha = 0.45f),
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
    attachTestTags: Boolean = true
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    val cardInteraction = remember { MutableInteractionSource() }
    val watchlistInteraction = remember { MutableInteractionSource() }
    val trailerInteraction = remember { MutableInteractionSource() }
    val darkPlaceholder = rememberDarkImagePainter(colors.surfaceElevated)
    val fadeOverlayBrush = rememberVerticalGradientBrush(
        Color.Black.copy(alpha = 0.45f),
        colors.atmosphericTop.copy(alpha = 0.30f),
        colors.background.copy(alpha = 0.90f),
        colors.background
    )
    val ratingText = remember(media.voteAverage) {
        if (media.voteAverage > 0.0) String.format(Locale.US, "%.1f", media.voteAverage) else ""
    }
    val genresLine = remember(media.genres) {
        media.genres.take(2).joinToString(" • ")
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(380.dp)
            .background(colors.background)
            .clickable(
                interactionSource = cardInteraction,
                indication = ripple(),
                onClick = onCardClick
            )
            .then(if (attachTestTags) Modifier.testTag("hero_banner_card") else Modifier)
    ) {
        // Backdrop Image
        if (media.fullBackdropUrl.isNotBlank()) {
            val backdropRequest = rememberCineImageRequest(
                url = media.fullBackdropUrl,
                widthDp = maxWidth,
                heightDp = 380.dp,
                crossfadeMs = 250
            )
            AsyncImage(
                model = backdropRequest,
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

        // Gradient Fade Overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(fadeOverlayBrush)
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Badges row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(HeroBadgeShape)
                        .background(gradients.softBadge)
                        .border(0.75.dp, colors.softRose.copy(alpha = 0.45f), HeroBadgeShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (media.mediaType == "tv") "SERIES" else "FEATURE FILM",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (ratingText.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), HeroSmallBadgeShape)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = colors.gold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = ratingText,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (media.certification.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), HeroSmallBadgeShape)
                            .border(0.75.dp, Color.White.copy(alpha = 0.45f), HeroSmallBadgeShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = media.certification,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (genresLine.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = genresLine,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = media.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Overview snippet
            if (media.overview.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = media.overview,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CineGradientButton(
                    onClick = onWatchClick,
                    shape = HeroButtonShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = if (attachTestTags) Modifier.testTag("hero_watch_button") else Modifier
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Watch Now",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Watch Now",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedButton(
                    onClick = onWatchlistClick,
                    interactionSource = watchlistInteraction,
                    shape = HeroButtonShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .cinePressScale(watchlistInteraction)
                        .then(if (attachTestTags) Modifier.testTag("hero_watchlist_button") else Modifier)
                ) {
                    Icon(
                        imageVector = if (isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Watchlist",
                        tint = if (isInWatchlist) colors.gold else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isInWatchlist) "Saved" else "+ List",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedButton(
                    onClick = onTrailerClick,
                    interactionSource = trailerInteraction,
                    shape = HeroButtonShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .cinePressScale(trailerInteraction)
                        .then(if (attachTestTags) Modifier.testTag("hero_trailer_button") else Modifier)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Trailer",
                        tint = colors.gold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Trailer",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
