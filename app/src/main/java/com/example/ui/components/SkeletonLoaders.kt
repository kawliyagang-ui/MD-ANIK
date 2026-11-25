package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CineTheme

@Composable
fun rememberShimmerBrush(): Brush {
    val colors = CineTheme.colors
    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val shimmerColors = listOf(
        colors.surfaceElevated,
        colors.surfaceHover,
        colors.softRose.copy(alpha = 0.14f),
        colors.surfaceHover,
        colors.surfaceElevated
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim - 320f, y = translateAnim - 320f),
        end = Offset(x = translateAnim, y = translateAnim)
    )
}

@Composable
fun MovieCardSkeleton(
    modifier: Modifier = Modifier,
    cardWidth: Dp = 140.dp,
    cardHeight: Dp = 210.dp,
    shimmerBrush: Brush = rememberShimmerBrush()
) {
    val colors = CineTheme.colors
    Column(
        modifier = modifier.width(cardWidth)
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(shimmerBrush)
                .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.background.copy(alpha = 0.35f))
                )
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.background.copy(alpha = 0.35f))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .height(13.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )

        Spacer(modifier = Modifier.height(5.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.52f)
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
    }
}

@Composable
fun HeroBannerSkeleton(
    modifier: Modifier = Modifier,
    shimmerBrush: Brush = rememberShimmerBrush()
) {
    val colors = CineTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(350.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .border(1.dp, colors.border.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(shimmerBrush)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .width(74.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.background.copy(alpha = 0.45f))
                )
                Box(
                    modifier = Modifier
                        .width(58.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.background.copy(alpha = 0.45f))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.background.copy(alpha = 0.5f))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(13.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.background.copy(alpha = 0.4f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.background.copy(alpha = 0.55f))
                )
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.background.copy(alpha = 0.45f))
                )
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.background.copy(alpha = 0.45f))
                )
            }
        }
    }
}

@Composable
fun HorizontalMovieListSkeleton(
    modifier: Modifier = Modifier,
    titleWidth: Dp = 150.dp,
    itemCount: Int = 5,
    cardWidth: Dp = 140.dp,
    cardHeight: Dp = 210.dp,
    showFilterChips: Boolean = false,
    shimmerBrush: Brush = rememberShimmerBrush()
) {
    val colors = CineTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(titleWidth)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerBrush)
                )
            }

            Box(
                modifier = Modifier
                    .width(54.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
        }

        if (showFilterChips) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(52.dp, 68.dp, 74.dp, 62.dp, 78.dp).forEach { chipWidth ->
                    Box(
                        modifier = Modifier
                            .width(chipWidth)
                            .height(32.dp)
                            .clip(RoundedCornerShape(50))
                            .background(shimmerBrush)
                            .border(1.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(50))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(
                count = itemCount,
                key = { it },
                contentType = { "skeleton_card" }
            ) {
                MovieCardSkeleton(
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    shimmerBrush = shimmerBrush
                )
            }
        }
    }
}

@Composable
fun HomeCatalogSkeleton(
    modifier: Modifier = Modifier
) {
    val gradients = CineTheme.gradients
    val shimmerBrush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradients.appBackdrop)
            .verticalScroll(rememberScrollState())
            .testTag("home_skeleton_loader")
    ) {
        HeroBannerSkeleton(shimmerBrush = shimmerBrush)

        HorizontalMovieListSkeleton(
            titleWidth = 148.dp,
            showFilterChips = true,
            shimmerBrush = shimmerBrush
        )

        HorizontalMovieListSkeleton(
            titleWidth = 136.dp,
            shimmerBrush = shimmerBrush
        )

        HorizontalMovieListSkeleton(
            titleWidth = 162.dp,
            shimmerBrush = shimmerBrush
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SearchResultsSkeletonGrid(
    modifier: Modifier = Modifier,
    itemCount: Int = 6
) {
    val shimmerBrush = rememberShimmerBrush()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 135.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        userScrollEnabled = false,
        modifier = modifier
            .fillMaxSize()
            .testTag("search_results_skeleton_grid")
    ) {
        items(
            count = itemCount,
            key = { it },
            contentType = { "skeleton_grid_card" }
        ) {
            MovieCardSkeleton(
                cardWidth = 140.dp,
                cardHeight = 205.dp,
                shimmerBrush = shimmerBrush
            )
        }
    }
}

@Composable
fun TmdbSearchRowSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 3
) {
    val colors = CineTheme.colors
    val shimmerBrush = rememberShimmerBrush()
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false,
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_tmdb_skeleton_row")
    ) {
        items(
            count = itemCount,
            key = { it },
            contentType = { "skeleton_tmdb_card" }
        ) {
            Column(
                modifier = Modifier
                    .width(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .padding(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(9.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
        }
    }
}

@Composable
fun DetailsEpisodesSkeleton(
    modifier: Modifier = Modifier,
    rowCount: Int = 4
) {
    val colors = CineTheme.colors
    val shimmerBrush = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("details_episodes_skeleton"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(rowCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}
