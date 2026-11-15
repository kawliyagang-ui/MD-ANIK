package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalCineColors = staticCompositionLocalOf { VelvetCinemaColors }
val LocalCineGradients = staticCompositionLocalOf { buildPaletteGradients(VelvetCinemaColors) }

object CineTheme {
    val colors: CinePaletteColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCineColors.current

    val gradients: CinePaletteGradients
        @Composable
        @ReadOnlyComposable
        get() = LocalCineGradients.current
}

fun buildMaterialColorScheme(paletteColors: CinePaletteColors): ColorScheme {
    return darkColorScheme(
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
        outline = paletteColors.border
    )
}

@Composable
fun MyApplicationTheme(
    palette: DarkThemePalette = DarkThemePalette.VELVET_CINEMA,
    content: @Composable () -> Unit
) {
    val paletteColors = remember(palette) { paletteColorsFor(palette) }
    val paletteGradients = remember(paletteColors) { buildPaletteGradients(paletteColors) }
    val colorScheme = remember(paletteColors) { buildMaterialColorScheme(paletteColors) }

    CompositionLocalProvider(
        LocalCineColors provides paletteColors,
        LocalCineGradients provides paletteGradients
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
