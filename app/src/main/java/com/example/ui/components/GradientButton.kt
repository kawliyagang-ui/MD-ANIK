package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CinetraTheme
import com.example.ui.theme.CinetraTokens

@Composable
fun CineGradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    brush: Brush = CinetraTheme.gradients.redButton,
    shape: Shape = RoundedCornerShape(14.dp),
    fillWidth: Boolean = false,
    minHeight: Dp = 48.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val colors = CinetraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = colors.onPrimary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = colors.onPrimary.copy(alpha = 0.6f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(0.dp),
        shape = shape,
        modifier = modifier.cinePressScale(
            interactionSource = interactionSource,
            enabled = enabled
        )
    ) {
        Box(
            modifier = Modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .defaultMinSize(minHeight = minHeight)
                .clip(shape)
                .background(brush, alpha = if (enabled) 1f else 0.55f)
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}

@Composable
fun StylishProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
    height: Dp = 5.dp,
    trackColor: Color = CinetraTheme.colors.surfaceHover,
    fillStartColor: Color = CinetraTheme.colors.accentEnd,   // #8F0E1F
    fillEndColor: Color = CinetraTheme.colors.accentStart,   // #FF2E4D
    dotColor: Color = CinetraTokens.ProgressGlowDot,         // #FF4D5E
    labelColor: Color = CinetraTheme.colors.accentStart,     // #FF2E4D
    showGlowDot: Boolean = true,
    cornerRadius: Dp = 50.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 220, easing = CineEase),
        label = "stylishProgress"
    )
    val canvasHeight = if (showGlowDot) maxOf(height, 14.dp) else height

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(canvasHeight)
        ) {
            val barHeightPx = height.toPx().coerceAtMost(size.height)
            val barTop = (size.height - barHeightPx) / 2f
            val radiusPx = minOf(cornerRadius.toPx(), barHeightPx / 2f)

            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, barTop),
                size = Size(size.width, barHeightPx),
                cornerRadius = CornerRadius(radiusPx, radiusPx)
            )

            if (animatedProgress > 0.005f) {
                val fillWidth = (size.width * animatedProgress).coerceAtLeast(barHeightPx)
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(fillStartColor, fillEndColor),
                        startX = 0f,
                        endX = fillWidth
                    ),
                    topLeft = Offset(0f, barTop),
                    size = Size(fillWidth, barHeightPx),
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )

                if (showGlowDot) {
                    val dotRadiusPx = 4.dp.toPx()
                    val haloRadiusPx = 7.dp.toPx()
                    val centerY = size.height / 2f
                    val tipX = fillWidth.coerceIn(
                        dotRadiusPx,
                        (size.width - dotRadiusPx).coerceAtLeast(dotRadiusPx)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(dotColor.copy(alpha = 0.50f), Color.Transparent),
                            center = Offset(tipX, centerY),
                            radius = haloRadiusPx
                        ),
                        radius = haloRadiusPx,
                        center = Offset(tipX, centerY)
                    )
                    drawCircle(
                        color = dotColor,
                        radius = dotRadiusPx,
                        center = Offset(tipX, centerY)
                    )
                }
            }
        }

        if (!label.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = labelColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun CineGradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 5.dp,
    brush: Brush = CinetraTheme.gradients.primaryCta,
    trackColor: Color = CinetraTheme.colors.surfaceHover,
    cornerRadius: Dp = 50.dp,
    showGlowDot: Boolean = true
) {
    StylishProgressBar(
        progress = progress,
        modifier = modifier,
        height = height,
        trackColor = trackColor,
        showGlowDot = showGlowDot,
        cornerRadius = cornerRadius
    )
}
