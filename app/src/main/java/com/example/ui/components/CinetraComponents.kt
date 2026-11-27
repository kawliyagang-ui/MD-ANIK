package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaItem
import com.example.ui.theme.CinetraTheme
import java.util.Locale

private val PillCornerShape = RoundedCornerShape(50)
private val ActionCardShape = RoundedCornerShape(16.dp)
private val PlayButtonShape = RoundedCornerShape(14.dp)

@Composable
fun RedPlayButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Default.PlayArrow,
    minHeight: Dp = 50.dp
) {
    val colors = CinetraTheme.colors
    RedButton(
        onClick = onClick,
        enabled = enabled,
        useGradient = false,
        shape = PlayButtonShape,
        fillWidth = true,
        minHeight = minHeight,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.onPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = colors.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    useGradient: Boolean = false,
    shape: Shape = PlayButtonShape,
    fillWidth: Boolean = false,
    minHeight: Dp = 48.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val colors = CinetraTheme.colors
    val gradients = CinetraTheme.gradients
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
                .then(
                    if (useGradient) {
                        Modifier.background(
                            brush = gradients.redButton,
                            alpha = if (enabled) 1f else 0.5f
                        )
                    } else {
                        Modifier.background(
                            color = if (enabled) colors.primary else colors.primary.copy(alpha = 0.5f)
                        )
                    }
                )
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
fun ActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    enabled: Boolean = true,
    iconTint: Color = if (isActive) CinetraTheme.colors.primary else CinetraTheme.colors.textPrimary,
    contentDescription: String? = label
) {
    val colors = CinetraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val bgColor by animateColorAsState(
        targetValue = if (isActive) colors.primary.copy(alpha = 0.14f) else colors.surfaceElevated,
        animationSpec = tween(durationMillis = 180, easing = CineEase),
        label = "actionCardBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isActive) colors.primary.copy(alpha = 0.65f) else colors.border,
        animationSpec = tween(durationMillis = 180, easing = CineEase),
        label = "actionCardBorder"
    )

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 72.dp)
            .minimumInteractiveComponentSize()
            .cinePressScale(interactionSource = interactionSource, enabled = enabled)
            .clip(ActionCardShape)
            .background(bgColor)
            .border(1.dp, borderColor, ActionCardShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) iconTint else colors.textMuted,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = if (isActive) colors.primary else if (enabled) colors.textPrimary else colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CategoryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CinetraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 84.dp)
            .minimumInteractiveComponentSize()
            .cinePressScale(interactionSource = interactionSource)
            .clip(ActionCardShape)
            .background(colors.surface)
            .border(1.dp, colors.border, ActionCardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = CinetraTheme.colors.textPrimary,
    size: Dp = 48.dp,
    iconSize: Dp = 20.dp,
    isActive: Boolean = false
) {
    val colors = CinetraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val bgColor = if (isActive) colors.primary.copy(alpha = 0.22f) else colors.glassSurface.copy(alpha = 0.85f)
    val borderColor = if (isActive) colors.primary.copy(alpha = 0.7f) else colors.glassBorder

    Box(
        modifier = modifier
            .size(size)
            .minimumInteractiveComponentSize()
            .cinePressScale(interactionSource = interactionSource)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) colors.primary else tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun CategoryPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    accessibilityLabel: String? = null
) {
    val colors = CinetraTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.surfaceElevated,
        animationSpec = tween(durationMillis = 180, easing = CineEase),
        label = "categoryPillBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.onPrimary else colors.textSecondary,
        animationSpec = tween(durationMillis = 180, easing = CineEase),
        label = "categoryPillContent"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.border,
        animationSpec = tween(durationMillis = 180, easing = CineEase),
        label = "categoryPillBorder"
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .cinePressScale(interactionSource = interactionSource)
            .clip(PillCornerShape)
            .background(containerColor)
            .border(1.dp, borderColor, PillCornerShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Tab,
                onClick = onClick
            )
            .semantics {
                this.selected = selected
                if (accessibilityLabel != null) {
                    this.contentDescription = accessibilityLabel
                }
            }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun RatingInlineLabel(
    rating: Double,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.sp,
    textColor: Color = CinetraTheme.colors.textPrimary,
    iconSize: Dp = 11.dp
) {
    if (rating <= 0.0) return
    val colors = CinetraTheme.colors
    val ratingText = remember(rating) { String.format(Locale.US, "%.1f", rating) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = ratingText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold
            ),
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
fun PosterCard(
    media: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 136.dp,
    cardHeight: Dp = 204.dp, // 2:3 ratio
    progressPercent: Int? = null,
    showTypePill: Boolean = false
) {
    val colors = CinetraTheme.colors
    val cardShape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val darkPlaceholder = rememberDarkImagePainter(colors.surface)

    val yearText = remember(media.releaseDate) { media.releaseDate.take(4) }
    val hasRating = media.voteAverage > 0.0
    val typePillLabel = remember(media.mediaType) {
        if (media.mediaType.equals("tv", ignoreCase = true)) "TV" else "MOVIE"
    }

    Column(
        modifier = modifier
            .width(cardWidth)
            .cinePressScale(interactionSource = interactionSource)
            .clip(cardShape)
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
                .clip(cardShape)
                .background(colors.surface)
                .border(1.dp, colors.border, cardShape)
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
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            if (showTypePill) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(PillCornerShape)
                        .background(colors.primary)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = typePillLabel,
                        color = colors.onPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                }
            }

            if (progressPercent != null && progressPercent > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(colors.background.copy(alpha = 0.65f))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    StylishProgressBar(
                        progress = (progressPercent / 100f).coerceIn(0f, 1f),
                        height = 4.dp,
                        showGlowDot = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = media.title,
            style = MaterialTheme.typography.bodyMedium, // Medium 13sp
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (yearText.isNotBlank() || hasRating) {
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (yearText.isNotBlank()) {
                    Text(
                        text = yearText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                        maxLines = 1
                    )
                }
                if (yearText.isNotBlank() && hasRating) {
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
                if (hasRating) {
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
fun LandscapeCard(
    title: String,
    imageUrl: String,
    metaText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 224.dp,
    progressFraction: Float? = null,
    progressLabel: String? = null,
    testTag: String? = null
) {
    val colors = CinetraTheme.colors
    val cardShape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val darkPlaceholder = rememberDarkImagePainter(colors.surface)
    val cardHeight = cardWidth * 9f / 16f

    Column(
        modifier = modifier
            .width(cardWidth)
            .cinePressScale(interactionSource = interactionSource)
            .clip(cardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(cardShape)
                .background(colors.surface)
                .border(1.dp, colors.border, cardShape)
        ) {
            if (imageUrl.isNotBlank()) {
                val req = rememberCineImageRequest(
                    url = imageUrl,
                    widthDp = cardWidth,
                    heightDp = cardHeight
                )
                AsyncImage(
                    model = req,
                    contentDescription = title,
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
        }

        if (progressFraction != null && progressFraction > 0f) {
            Spacer(modifier = Modifier.height(6.dp))
            StylishProgressBar(
                progress = progressFraction,
                label = progressLabel,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (metaText.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = metaText,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSeeAllClick: (() -> Unit)? = null,
    seeAllText: String = "See all",
    seeAllTestTag: String? = null
) {
    val colors = CinetraTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            ),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (onSeeAllClick != null) {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSeeAllClick)
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .then(if (seeAllTestTag != null) Modifier.testTag(seeAllTestTag) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = seeAllText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.primary
                )
            }
        }
    }
}

@Composable
fun FloatingGlassBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val colors = CinetraTheme.colors
    val pillShape = PillCornerShape

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp)
            .padding(bottom = 14.dp)
    ) {
        Surface(
            color = colors.glassSurface,
            shape = pillShape,
            border = BorderStroke(1.dp, colors.glassBorder),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 16.dp,
                    shape = pillShape,
                    ambientColor = Color.Black.copy(alpha = 0.75f),
                    spotColor = Color.Black.copy(alpha = 0.75f)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                content = content
            )
        }
    }
}

@Composable
fun CinetraEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    ctaLabel: String? = null,
    ctaTestTag: String? = null,
    onCtaClick: (() -> Unit)? = null
) {
    val colors = CinetraTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )
        if (!ctaLabel.isNullOrBlank() && onCtaClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            RedButton(
                onClick = onCtaClick,
                modifier = if (ctaTestTag != null) Modifier.testTag(ctaTestTag) else Modifier
            ) {
                Text(
                    text = ctaLabel,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary
                )
            }
        }
    }
}
