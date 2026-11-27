package com.example.ui.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.MediaItem
import com.example.ui.components.LocalDownloadedMediaIds
import com.example.ui.components.rememberCineImageRequest
import com.example.ui.components.rememberDarkImagePainter
import com.example.ui.theme.CineTheme
import java.util.Locale

val TvOverscanHorizontal = 48.dp
val TvOverscanVertical = 27.dp

enum class TvTopDestination(
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Default.Home, "tv_nav_home"),
    SEARCH("Search", Icons.Default.Search, "tv_nav_search"),
    MY_LIST("My List", Icons.Default.Bookmark, "tv_nav_mylist"),
    DOWNLOADS("Downloads", Icons.Default.Download, "tv_nav_downloads")
}

@Composable
fun TvTopNavigationBar(
    selectedDestination: TvTopDestination,
    watchlistCount: Int,
    runningDownloadsCount: Int,
    onSelectDestination: (TvTopDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    val gradients = CineTheme.gradients
    val selectedIndex = TvTopDestination.entries.indexOf(selectedDestination).coerceAtLeast(0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(gradients.topBarBackdrop)
            .padding(horizontal = TvOverscanHorizontal, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_cinetra_logo),
                contentDescription = "Cinetra Logo",
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "CINETRA",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TV",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.softRose,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.primaryContainer)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }

        TabRow(
            selectedTabIndex = selectedIndex,
            separator = { Spacer(modifier = Modifier.width(10.dp)) },
            indicator = { tabPositions, doesTabRowHaveFocus ->
                TabRowDefaults.PillIndicator(
                    currentTabPosition = tabPositions[selectedIndex],
                    doesTabRowHaveFocus = doesTabRowHaveFocus,
                    activeColor = colors.primary,
                    inactiveColor = colors.accentMid.copy(alpha = 0.72f)
                )
            }
        ) {
            TvTopDestination.entries.forEachIndexed { index, dest ->
                val isSelected = selectedIndex == index
                val suffix = when (dest) {
                    TvTopDestination.MY_LIST -> if (watchlistCount > 0) " ($watchlistCount)" else ""
                    TvTopDestination.DOWNLOADS -> if (runningDownloadsCount > 0) " ($runningDownloadsCount)" else ""
                    else -> ""
                }
                Tab(
                    selected = isSelected,
                    onFocus = {},
                    onClick = { onSelectDestination(dest) },
                    modifier = Modifier.testTag(dest.testTag)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${dest.label}$suffix",
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else colors.textSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvActionButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = CineTheme.colors.primary,
    contentColor: Color = Color.White,
    enabled: Boolean = true
) {
    val colors = CineTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(10.dp)),
        scale = ButtonDefaults.scale(focusedScale = 1.06f, pressedScale = 0.98f),
        border = ButtonDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), shape = RoundedCornerShape(10.dp)),
            focusedBorder = Border(BorderStroke(2.5.dp, Color.White), shape = RoundedCornerShape(10.dp))
        ),
        colors = ButtonDefaults.colors(
            containerColor = containerColor,
            contentColor = contentColor,
            focusedContainerColor = colors.primaryHover,
            focusedContentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TvOutlinedActionButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    enabled: Boolean = true
) {
    val colors = CineTheme.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(10.dp)),
        scale = ButtonDefaults.scale(focusedScale = 1.06f, pressedScale = 0.98f),
        border = ButtonDefaults.border(
            border = Border(
                BorderStroke(1.dp, if (highlighted) colors.softRose.copy(alpha = 0.7f) else colors.border),
                shape = RoundedCornerShape(10.dp)
            ),
            focusedBorder = Border(
                BorderStroke(2.5.dp, colors.softRose),
                shape = RoundedCornerShape(10.dp)
            )
        ),
        colors = ButtonDefaults.colors(
            containerColor = if (highlighted) colors.primaryContainer else colors.surfaceElevated.copy(alpha = 0.85f),
            contentColor = if (highlighted) colors.softRose else colors.textPrimary,
            focusedContainerColor = colors.surfaceHover,
            focusedContentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TvFilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CineTheme.colors
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(999.dp)),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) colors.accentMid else colors.surfaceElevated,
            contentColor = if (selected) Color.White else colors.textSecondary,
            focusedContainerColor = if (selected) colors.primary else colors.surfaceHover,
            focusedContentColor = Color.White
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                BorderStroke(1.dp, if (selected) colors.softRose.copy(alpha = 0.7f) else colors.border),
                shape = RoundedCornerShape(999.dp)
            ),
            focusedBorder = Border(
                BorderStroke(2.5.dp, Color.White),
                shape = RoundedCornerShape(999.dp)
            )
        ),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            maxLines = 1
        )
    }
}

@Composable
fun TvPosterCard(
    media: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocused: () -> Unit = {},
    cardWidth: Dp = 148.dp,
    cardHeight: Dp = 222.dp,
    rank: Int? = null,
    progressPercent: Int? = null,
    episodeLabel: String? = null
) {
    val colors = CineTheme.colors
    val downloadedMediaIds = LocalDownloadedMediaIds.current
    val isOfflineAvailable = media.id in downloadedMediaIds
    val darkPainter = rememberDarkImagePainter()
    var isFocused by remember { mutableStateOf(false) }

    val ratingLabel = remember(media.voteAverage) {
        if (media.voteAverage > 0.0) String.format(Locale.US, "%.1f", media.voteAverage) else ""
    }
    val yearLabel = remember(media.releaseDate) { media.releaseDate.take(4) }
    val cardImageRequest = rememberCineImageRequest(
        url = media.fullPosterUrl,
        widthDp = cardWidth,
        heightDp = cardHeight
    )

    Column(
        modifier = modifier.width(cardWidth),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            onClick = onClick,
            shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
            scale = CardDefaults.scale(focusedScale = 1.07f, pressedScale = 0.99f),
            border = CardDefaults.border(
                border = Border(
                    BorderStroke(1.dp, colors.border.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(12.dp)
                ),
                focusedBorder = Border(
                    BorderStroke(2.5.dp, colors.softRose),
                    shape = RoundedCornerShape(12.dp)
                )
            ),
            glow = CardDefaults.glow(
                focusedGlow = Glow(
                    elevationColor = colors.primary.copy(alpha = 0.48f),
                    elevation = 14.dp
                )
            ),
            colors = CardDefaults.colors(containerColor = colors.surfaceElevated),
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { state ->
                    isFocused = state.isFocused
                    if (state.isFocused) onFocused()
                }
                .testTag("tv_poster_card_${media.id}")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = cardImageRequest,
                    contentDescription = media.title,
                    placeholder = darkPainter,
                    error = darkPainter,
                    fallback = darkPainter,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle bottom readability gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )

                // Rating badge
                if (ratingLabel.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = colors.gold,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = ratingLabel,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Rank or Offline indicator
                when {
                    rank != null -> {
                        Text(
                            text = "#$rank",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.primary.copy(alpha = 0.92f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    isOfflineAvailable -> {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.success.copy(alpha = 0.9f))
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = "Downloaded",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Episode / Continue Watching progress overlay
                if (!episodeLabel.isNullOrBlank() || (progressPercent != null && progressPercent > 0)) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!episodeLabel.isNullOrBlank()) {
                            Text(
                                text = episodeLabel,
                                color = colors.gold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (progressPercent != null && progressPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = 0.28f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((progressPercent / 100f).coerceIn(0.04f, 1f))
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(colors.primary)
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 2.dp)) {
            Text(
                text = media.title,
                color = if (isFocused) Color.White else colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val metaLine = buildList {
                if (yearLabel.isNotBlank()) add(yearLabel)
                add(if (media.mediaType.equals("tv", ignoreCase = true)) "Series" else "Movie")
            }.joinToString(" • ")
            Text(
                text = metaLine,
                color = if (isFocused) colors.softRose else colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}
