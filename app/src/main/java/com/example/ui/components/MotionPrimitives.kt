package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import coil.size.Precision
import com.example.ui.theme.CineTheme

val CineEase = FastOutSlowInEasing

/**
 * Applies a subtle GPU-accelerated press scale (default 0.97f) using graphicsLayer
 * without triggering relayout or bouncy springs.
 */
fun Modifier.cinePressScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) pressedScale else 1f,
        animationSpec = tween(durationMillis = 110, easing = CineEase),
        label = "cinePressScale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Convenience clickable modifier pairing subtle Material ripple + 0.97f press scale.
 */
fun Modifier.cineSubtleClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    indication: Indication? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val resolvedIndication = indication ?: ripple(bounded = true)
    this
        .cinePressScale(
            interactionSource = interactionSource,
            enabled = enabled,
            pressedScale = pressedScale
        )
        .clickable(
            interactionSource = interactionSource,
            indication = resolvedIndication,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Builds a remembered Coil ImageRequest sized precisely in pixels for the target slot,
 * using a short 220ms crossfade and shared memory/disk keys for instant cache reuse.
 */
@Composable
fun rememberCineImageRequest(
    url: String,
    widthDp: Dp,
    heightDp: Dp,
    crossfadeMs: Int = 220
): ImageRequest {
    val context = LocalContext.current
    val density = LocalDensity.current
    val widthPx = with(density) { widthDp.roundToPx().coerceAtLeast(1) }
    val heightPx = with(density) { heightDp.roundToPx().coerceAtLeast(1) }
    return remember(url, widthPx, heightPx, crossfadeMs, context) {
        ImageRequest.Builder(context)
            .data(url)
            .size(widthPx, heightPx)
            .precision(Precision.INEXACT)
            .memoryCacheKey(url)
            .diskCacheKey(url)
            .crossfade(crossfadeMs)
            .build()
    }
}

/**
 * Dark ColorPainter used as AsyncImage placeholder & error drawable so posters never flash white.
 */
@Composable
fun rememberDarkImagePainter(
    color: Color = CineTheme.colors.surfaceElevated
): ColorPainter = remember(color) { ColorPainter(color) }

/**
 * Caches vertical gradient brushes in composition to avoid repeated array allocations in Lazy items.
 */
@Composable
fun rememberVerticalGradientBrush(c1: Color, c2: Color): Brush =
    remember(c1, c2) { Brush.verticalGradient(listOf(c1, c2)) }

@Composable
fun rememberVerticalGradientBrush(c1: Color, c2: Color, c3: Color): Brush =
    remember(c1, c2, c3) { Brush.verticalGradient(listOf(c1, c2, c3)) }

@Composable
fun rememberVerticalGradientBrush(c1: Color, c2: Color, c3: Color, c4: Color): Brush =
    remember(c1, c2, c3, c4) { Brush.verticalGradient(listOf(c1, c2, c3, c4)) }

/**
 * Caches horizontal gradient brushes in composition to avoid per-recomposition allocations.
 */
@Composable
fun rememberHorizontalGradientBrush(c1: Color, c2: Color): Brush =
    remember(c1, c2) { Brush.horizontalGradient(listOf(c1, c2)) }

@Composable
fun rememberHorizontalGradientBrush(c1: Color, c2: Color, c3: Color): Brush =
    remember(c1, c2, c3) { Brush.horizontalGradient(listOf(c1, c2, c3)) }

/**
 * Soft non-bouncy fade + subtle scale (0.96f -> 1f) entrance wrapper for Dialog cards.
 */
@Composable
fun CineDialogEntryBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 200, easing = CineEase)
        )
    }
    Box(
        modifier = modifier.graphicsLayer {
            val p = progress.value
            alpha = p
            val s = 0.96f + (0.04f * p)
            scaleX = s
            scaleY = s
        },
        content = content
    )
}

/**
 * Soft fade + subtle upward slide entrance for ModalBottomSheet body content.
 */
@Composable
fun CineSheetEntryBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val progress = remember { Animatable(0f) }
    val offsetPx = with(LocalDensity.current) { 14.dp.toPx() }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 220, easing = CineEase)
        )
    }
    Box(
        modifier = modifier.graphicsLayer {
            val p = progress.value
            alpha = p
            translationY = (1f - p) * offsetPx
        },
        content = content
    )
}
