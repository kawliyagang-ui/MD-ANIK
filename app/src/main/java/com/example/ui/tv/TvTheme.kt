package com.example.ui.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.example.ui.theme.CinePaletteColors
import com.example.ui.theme.CineTheme

fun buildTvColorScheme(paletteColors: CinePaletteColors) = darkColorScheme(
    primary = paletteColors.primary,
    onPrimary = Color.White,
    primaryContainer = paletteColors.primaryContainer,
    onPrimaryContainer = paletteColors.onPrimaryContainer,
    secondary = paletteColors.gold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3E3000),
    onSecondaryContainer = paletteColors.goldLight,
    tertiary = paletteColors.info,
    background = paletteColors.background,
    onBackground = paletteColors.textPrimary,
    surface = paletteColors.surface,
    onSurface = paletteColors.textPrimary,
    surfaceVariant = paletteColors.surfaceElevated,
    onSurfaceVariant = paletteColors.textSecondary,
    border = paletteColors.border
)

@Composable
fun CinetraTvTheme(content: @Composable () -> Unit) {
    val cineColors = CineTheme.colors
    val tvColors = remember(cineColors) { buildTvColorScheme(cineColors) }
    MaterialTheme(
        colorScheme = tvColors,
        content = content
    )
}
