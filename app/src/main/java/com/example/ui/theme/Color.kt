package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class DarkThemePalette(
    val storageKey: String,
    val displayName: String,
    val subtitle: String,
    val badge: String,
    val isHighReadability: Boolean
) {
    VELVET_CINEMA(
        storageKey = "velvet_cinema",
        displayName = "Velvet Cinema",
        subtitle = "Deep obsidian-plum backdrop with soft rose-plum gradients.",
        badge = "Default",
        isHighReadability = false
    ),
    CRISP_SLATE(
        storageKey = "crisp_slate",
        displayName = "Crisp Slate",
        subtitle = "High-contrast navy slate with bright off-white text and sharp borders for maximum legibility.",
        badge = "High Contrast",
        isHighReadability = true
    ),
    WARM_AMBER_NIGHT(
        storageKey = "warm_amber_night",
        displayName = "Warm Amber Night",
        subtitle = "Low-glare warm espresso surfaces with cream typography to reduce night eye strain.",
        badge = "Low Glare",
        isHighReadability = true
    );

    companion object {
        fun fromKey(key: String?): DarkThemePalette {
            return entries.firstOrNull { it.storageKey == key } ?: VELVET_CINEMA
        }
    }
}

object ThemeTogglePolicy {
    fun toggleHighReadability(
        current: DarkThemePalette,
        preferredHighReadability: DarkThemePalette = DarkThemePalette.CRISP_SLATE
    ): DarkThemePalette {
        val safeReadable = if (preferredHighReadability.isHighReadability) {
            preferredHighReadability
        } else {
            DarkThemePalette.CRISP_SLATE
        }
        return if (current.isHighReadability) {
            DarkThemePalette.VELVET_CINEMA
        } else {
            safeReadable
        }
    }
}

@Immutable
data class CinePaletteColors(
    val palette: DarkThemePalette,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHover: Color,
    val primary: Color,
    val primaryHover: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val softRose: Color,
    val accentStart: Color,
    val accentMid: Color,
    val accentEnd: Color,
    val navIndicator: Color,
    val atmosphericTop: Color,
    val atmosphericMid: Color,
    val topBarStart: Color,
    val topBarMid: Color,
    val topBarEnd: Color,
    val bottomBarTop: Color,
    val bottomBarBottom: Color,
    val gold: Color,
    val goldLight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val success: Color,
    val warning: Color,
    val info: Color
)

@Immutable
data class CinePaletteGradients(
    val appBackdrop: Brush,
    val topBarBackdrop: Brush,
    val bottomBarBackdrop: Brush,
    val primaryCta: Brush,
    val softBadge: Brush,
    val wordmark: Brush,
    val adGateGlow: Brush,
    val adBannerSurface: Brush
)

val VelvetCinemaColors = CinePaletteColors(
    palette = DarkThemePalette.VELVET_CINEMA,
    background = Color(0xFF09090D),
    surface = Color(0xFF13131B),
    surfaceElevated = Color(0xFF1D1D28),
    surfaceHover = Color(0xFF272736),
    primary = Color(0xFFC4455B),
    primaryHover = Color(0xFFDA5D72),
    primaryContainer = Color(0xFF2B1824),
    onPrimaryContainer = Color(0xFFF6D9E2),
    softRose = Color(0xFFE37A8C),
    accentStart = Color(0xFFC84B5F),
    accentMid = Color(0xFF9F365D),
    accentEnd = Color(0xFF6B2D5C),
    navIndicator = Color(0xFF362032),
    atmosphericTop = Color(0xFF171221),
    atmosphericMid = Color(0xFF0D0C12),
    topBarStart = Color(0xFF15111D),
    topBarMid = Color(0xFF1B1422),
    topBarEnd = Color(0xFF12121A),
    bottomBarTop = Color(0xFF171421),
    bottomBarBottom = Color(0xFF0F0E15),
    gold = Color(0xFFFFC107),
    goldLight = Color(0xFFFFD54F),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFFB3B3C0),
    textMuted = Color(0xFF838391),
    border = Color(0xFF2E2E3E),
    success = Color(0xFF10B981),
    warning = Color(0xFFF59E0B),
    info = Color(0xFF3B82F6)
)

val CrispSlateColors = CinePaletteColors(
    palette = DarkThemePalette.CRISP_SLATE,
    background = Color(0xFF0A1019),
    surface = Color(0xFF121B29),
    surfaceElevated = Color(0xFF1B273A),
    surfaceHover = Color(0xFF24344D),
    primary = Color(0xFF0EA5E9),
    primaryHover = Color(0xFF38BDF8),
    primaryContainer = Color(0xFF132D46),
    onPrimaryContainer = Color(0xFFE0F2FE),
    softRose = Color(0xFF7DD3FC),
    accentStart = Color(0xFF0284C7),
    accentMid = Color(0xFF0EA5E9),
    accentEnd = Color(0xFF2563EB),
    navIndicator = Color(0xFF183654),
    atmosphericTop = Color(0xFF111D2E),
    atmosphericMid = Color(0xFF0D1521),
    topBarStart = Color(0xFF101927),
    topBarMid = Color(0xFF162235),
    topBarEnd = Color(0xFF101927),
    bottomBarTop = Color(0xFF141F30),
    bottomBarBottom = Color(0xFF0C131E),
    gold = Color(0xFFFBBF24),
    goldLight = Color(0xFFFDE68A),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFD5DFEC),
    textMuted = Color(0xFFA0B1C7),
    border = Color(0xFF3A4E6C),
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    info = Color(0xFF60A5FA)
)

val WarmAmberNightColors = CinePaletteColors(
    palette = DarkThemePalette.WARM_AMBER_NIGHT,
    background = Color(0xFF110E0B),
    surface = Color(0xFF1B1612),
    surfaceElevated = Color(0xFF261F1A),
    surfaceHover = Color(0xFF332922),
    primary = Color(0xFFD97757),
    primaryHover = Color(0xFFE89174),
    primaryContainer = Color(0xFF3A241B),
    onPrimaryContainer = Color(0xFFFDE8DC),
    softRose = Color(0xFFF4A261),
    accentStart = Color(0xFFE07A5F),
    accentMid = Color(0xFFC8644B),
    accentEnd = Color(0xFF8C4338),
    navIndicator = Color(0xFF3C2820),
    atmosphericTop = Color(0xFF1E1712),
    atmosphericMid = Color(0xFF15110D),
    topBarStart = Color(0xFF1A1410),
    topBarMid = Color(0xFF231B15),
    topBarEnd = Color(0xFF17120E),
    bottomBarTop = Color(0xFF1D1713),
    bottomBarBottom = Color(0xFF130F0C),
    gold = Color(0xFFF4B942),
    goldLight = Color(0xFFFAD37D),
    textPrimary = Color(0xFFFAF3E8),
    textSecondary = Color(0xFFDAD0C2),
    textMuted = Color(0xFFA89A8A),
    border = Color(0xFF45382E),
    success = Color(0xFF34D399),
    warning = Color(0xFFF59E0B),
    info = Color(0xFF60A5FA)
)

fun paletteColorsFor(palette: DarkThemePalette): CinePaletteColors {
    return when (palette) {
        DarkThemePalette.VELVET_CINEMA -> VelvetCinemaColors
        DarkThemePalette.CRISP_SLATE -> CrispSlateColors
        DarkThemePalette.WARM_AMBER_NIGHT -> WarmAmberNightColors
    }
}

fun buildPaletteGradients(colors: CinePaletteColors): CinePaletteGradients {
    return CinePaletteGradients(
        appBackdrop = Brush.verticalGradient(
            colors = listOf(
                colors.atmosphericTop,
                colors.atmosphericMid,
                colors.background
            )
        ),
        topBarBackdrop = Brush.horizontalGradient(
            colors = listOf(
                colors.topBarStart,
                colors.topBarMid,
                colors.topBarEnd
            )
        ),
        bottomBarBackdrop = Brush.verticalGradient(
            colors = listOf(
                colors.bottomBarTop,
                colors.bottomBarBottom
            )
        ),
        primaryCta = Brush.horizontalGradient(
            colors = listOf(
                colors.accentStart,
                colors.accentMid,
                colors.accentEnd
            )
        ),
        softBadge = Brush.horizontalGradient(
            colors = listOf(
                colors.accentMid,
                colors.accentEnd
            )
        ),
        wordmark = Brush.horizontalGradient(
            colors = listOf(
                colors.textPrimary,
                colors.softRose,
                colors.goldLight
            )
        ),
        adGateGlow = Brush.radialGradient(
            colors = listOf(
                colors.accentEnd.copy(alpha = 0.24f),
                colors.surface,
                colors.background
            )
        ),
        adBannerSurface = Brush.linearGradient(
            colors = listOf(
                colors.surface,
                colors.primaryContainer.copy(alpha = 0.78f)
            )
        )
    )
}

// Legacy top-level defaults (point to Velvet Cinema baseline for static contexts)
val CineDarkBackground = VelvetCinemaColors.background
val CineDarkSurface = VelvetCinemaColors.surface
val CineDarkSurfaceElevated = VelvetCinemaColors.surfaceElevated
val CineDarkSurfaceHover = VelvetCinemaColors.surfaceHover

val CineRedPrimary = VelvetCinemaColors.primary
val CineRedHover = VelvetCinemaColors.primaryHover
val CineRedContainer = VelvetCinemaColors.primaryContainer
val CineSoftRose = VelvetCinemaColors.softRose

val CineAccentStart = VelvetCinemaColors.accentStart
val CineAccentMid = VelvetCinemaColors.accentMid
val CineAccentEnd = VelvetCinemaColors.accentEnd
val CineNavIndicator = VelvetCinemaColors.navIndicator

val CineAtmosphericTop = VelvetCinemaColors.atmosphericTop
val CineAtmosphericMid = VelvetCinemaColors.atmosphericMid

val CineGold = VelvetCinemaColors.gold
val CineGoldLight = VelvetCinemaColors.goldLight

val CineTextPrimary = VelvetCinemaColors.textPrimary
val CineTextSecondary = VelvetCinemaColors.textSecondary
val CineTextMuted = VelvetCinemaColors.textMuted

val CineBorder = VelvetCinemaColors.border
val CineSuccess = VelvetCinemaColors.success
val CineWarning = VelvetCinemaColors.warning
val CineInfo = VelvetCinemaColors.info

object CineGradients {
    private val defaultGradients = buildPaletteGradients(VelvetCinemaColors)
    val appBackdrop: Brush = defaultGradients.appBackdrop
    val topBarBackdrop: Brush = defaultGradients.topBarBackdrop
    val bottomBarBackdrop: Brush = defaultGradients.bottomBarBackdrop
    val primaryCta: Brush = defaultGradients.primaryCta
    val softBadge: Brush = defaultGradients.softBadge
    val wordmark: Brush = defaultGradients.wordmark
    val adGateGlow: Brush = defaultGradients.adGateGlow
    val adBannerSurface: Brush = defaultGradients.adBannerSurface
}
