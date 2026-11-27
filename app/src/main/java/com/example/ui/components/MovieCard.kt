package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.MediaItem

val LocalDownloadedMediaIds = staticCompositionLocalOf<Set<String>> { emptySet() }

@Composable
fun MovieCard(
    media: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 136.dp,
    cardHeight: Dp = 204.dp,
    progressPercent: Int? = null,
    episodeLabel: String? = null,
    showTypePill: Boolean = false
) {
    PosterCard(
        media = media,
        onClick = onClick,
        modifier = modifier,
        cardWidth = cardWidth,
        cardHeight = cardHeight,
        progressPercent = progressPercent,
        showTypePill = showTypePill
    )
}
