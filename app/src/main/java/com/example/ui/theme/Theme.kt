package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val CinetraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),      // primary & play buttons
    medium = RoundedCornerShape(14.dp),     // poster & landscape cards
    large = RoundedCornerShape(20.dp),      // hero poster card
    extraLarge = RoundedCornerShape(24.dp)  // bottom sheets & dialogs
)

val LocalCineColors = staticCompositionLocalOf { VelvetCinemaColors }
val LocalCineGradients = staticCompositionLocalOf { buildPaletteGradients(VelvetCinemaColors) }
val LocalBottomBarClearance = staticCompositionLocalOf { 0.dp }

object CinetraTheme {
    val colors: CinePaletteColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCineColors.current

    val gradients: CinePaletteGradients
        @Composable
        @ReadOnlyComposable
        get() = LocalCineGradients.current

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes

    val bottomBarClearance: Dp
        @Composable
        @ReadOnlyComposable
        get() = LocalBottomBarClearance.current
}

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
    return if (paletteColors.isLight) {
        lightColorScheme(
            primary = paletteColors.primary,
            onPrimary = paletteColors.onPrimary,
            primaryContainer = paletteColors.primaryContainer,
            onPrimaryContainer = paletteColors.onPrimaryContainer,
            secondary = paletteColors.softRose,
            onSecondary = paletteColors.onPrimary,
            secondaryContainer = paletteColors.primaryContainer,
            onSecondaryContainer = paletteColors.onPrimaryContainer,
            tertiary = paletteColors.info,
            background = paletteColors.background,
            onBackground = paletteColors.textPrimary,
            surface = paletteColors.surface,
            onSurface = paletteColors.textPrimary,
            surfaceVariant = paletteColors.surfaceElevated,
            onSurfaceVariant = paletteColors.textSecondary,
            outline = paletteColors.border
        )
    } else {
        darkColorScheme(
            primary = paletteColors.primary,
            onPrimary = paletteColors.onPrimary,
            primaryContainer = paletteColors.primaryContainer,
            onPrimaryContainer = paletteColors.onPrimaryContainer,
            secondary = paletteColors.softRose,
            onSecondary = paletteColors.onPrimary,
            secondaryContainer = paletteColors.primaryContainer,
            onSecondaryContainer = paletteColors.onPrimaryContainer,
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
}

@Composable
fun CinetraTheme(
    palette: DarkThemePalette = DarkThemePalette.VELVET_CINEMA,
    themeMode: ThemeMode = ThemeMode.DARK,
    accentColor: AccentColorOption? = null,
    content: @Composable () -> Unit
) {
    val systemIsDark = isSystemInDarkTheme()
    val useLightTheme = when (themeMode) {
        ThemeMode.LIGHT -> true
        ThemeMode.DARK -> false
        ThemeMode.SYSTEM -> !systemIsDark
    }
    val paletteColors = remember(palette, useLightTheme, accentColor) {
        paletteColorsFor(
            palette = palette,
            isLight = useLightTheme,
            accentOption = accentColor
        )
    }
    val paletteGradients = remember(paletteColors) { buildPaletteGradients(paletteColors) }
    val colorScheme = remember(paletteColors) { buildMaterialColorScheme(paletteColors) }

    CompositionLocalProvider(
        LocalCineColors provides paletteColors,
        LocalCineGradients provides paletteGradients
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = CinetraShapes,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    palette: DarkThemePalette = DarkThemePalette.VELVET_CINEMA,
    themeMode: ThemeMode = ThemeMode.DARK,
    accentColor: AccentColorOption? = null,
    content: @Composable () -> Unit
) {
    CinetraTheme(
        palette = palette,
        themeMode = themeMode,
        accentColor = accentColor,
        content = content
    )
}
