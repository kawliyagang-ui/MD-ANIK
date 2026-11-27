package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object CinetraTokens {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF17171B)
    val SurfaceVariant = Color(0xFF1E1E24)
    val ProgressTrack = Color(0xFF2A2A31)
    val SurfaceHover = ProgressTrack
    val Border = Color(0xFF232329)

    val PrimaryRed = Color(0xFFE50914)
    val RedGradientStart = Color(0xFFFF2E4D)
    val RedGradientEnd = Color(0xFF8F0E1F)
    val ProgressGlowDot = Color(0xFFFF4D5E)
    val OnRed = Color(0xFFFFFFFF)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB9B9C0)
    val TextMuted = Color(0xFF71717A)

    val GlassSurface = Color(0xFF101014)
    val GlassBorder = Color(0xFF232329)
    val NavInactiveIcon = Color(0xFF8A8A93)

    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Info = Color(0xFF3B82F6)
}

enum class ThemeMode(
    val storageKey: String,
    val displayName: String,
    val subtitle: String
) {
    DARK("dark", "Dark Mode", "Pure-black cinema theme with Cinetra red accents"),
    LIGHT("light", "Light Mode", "Bright high-clarity daytime UI"),
    SYSTEM("system", "System Default", "Follow device appearance setting");

    companion object {
        fun fromKey(key: String?): ThemeMode {
            return entries.firstOrNull { it.storageKey == key } ?: DARK
        }
    }
}

enum class AccentColorOption(
    val storageKey: String,
    val displayName: String,
    val primaryColor: Color,
    val hoverColor: Color,
    val softTint: Color,
    val darkContainer: Color,
    val lightContainer: Color,
    val onDarkContainer: Color,
    val onLightContainer: Color,
    val gradientStart: Color,
    val gradientMid: Color,
    val gradientEnd: Color
) {
    VELVET_ROSE(
        storageKey = "velvet_rose",
        displayName = "Cinetra Red",
        primaryColor = CinetraTokens.PrimaryRed,
        hoverColor = CinetraTokens.RedGradientStart,
        softTint = CinetraTokens.RedGradientStart,
        darkContainer = Color(0xFF2A0D12),
        lightContainer = Color(0xFFFFE5E7),
        onDarkContainer = CinetraTokens.OnRed,
        onLightContainer = Color(0xFF8F0E1F),
        gradientStart = CinetraTokens.RedGradientStart,
        gradientMid = CinetraTokens.PrimaryRed,
        gradientEnd = CinetraTokens.RedGradientEnd
    ),
    CRIMSON_RED(
        storageKey = "crimson_red",
        displayName = "Cinema Red",
        primaryColor = CinetraTokens.PrimaryRed,
        hoverColor = CinetraTokens.RedGradientStart,
        softTint = CinetraTokens.RedGradientStart,
        darkContainer = Color(0xFF2A0D12),
        lightContainer = Color(0xFFFFE5E7),
        onDarkContainer = CinetraTokens.OnRed,
        onLightContainer = Color(0xFF8E000A),
        gradientStart = CinetraTokens.RedGradientStart,
        gradientMid = CinetraTokens.PrimaryRed,
        gradientEnd = CinetraTokens.RedGradientEnd
    ),
    ELECTRIC_BLUE(
        storageKey = "electric_blue",
        displayName = "Cobalt Blue",
        primaryColor = Color(0xFF0EA5E9),
        hoverColor = Color(0xFF38BDF8),
        softTint = Color(0xFF7DD3FC),
        darkContainer = Color(0xFF132D46),
        lightContainer = Color(0xFFE0F2FE),
        onDarkContainer = Color(0xFFE0F2FE),
        onLightContainer = Color(0xFF075985),
        gradientStart = Color(0xFF0284C7),
        gradientMid = Color(0xFF0EA5E9),
        gradientEnd = Color(0xFF2563EB)
    ),
    ROYAL_VIOLET(
        storageKey = "royal_violet",
        displayName = "Royal Violet",
        primaryColor = Color(0xFF8B5CF6),
        hoverColor = Color(0xFFA78BFA),
        softTint = Color(0xFFC4B5FD),
        darkContainer = Color(0xFF281B49),
        lightContainer = Color(0xFFEDE9FE),
        onDarkContainer = Color(0xFFEDE9FE),
        onLightContainer = Color(0xFF5B21B6),
        gradientStart = Color(0xFF7C3AED),
        gradientMid = Color(0xFF8B5CF6),
        gradientEnd = Color(0xFF4F46E5)
    ),
    EMERALD_MINT(
        storageKey = "emerald_mint",
        displayName = "Emerald",
        primaryColor = Color(0xFF10B981),
        hoverColor = Color(0xFF34D399),
        softTint = Color(0xFF6EE7B7),
        darkContainer = Color(0xFF0E3529),
        lightContainer = Color(0xFFD1FAE5),
        onDarkContainer = Color(0xFFD1FAE5),
        onLightContainer = Color(0xFF065F46),
        gradientStart = Color(0xFF059669),
        gradientMid = Color(0xFF10B981),
        gradientEnd = Color(0xFF0D9488)
    ),
    AMBER_GOLD(
        storageKey = "amber_gold",
        displayName = "Sunset Gold",
        primaryColor = Color(0xFFF59E0B),
        hoverColor = Color(0xFFFBBF24),
        softTint = Color(0xFFFCD34D),
        darkContainer = Color(0xFF3A270B),
        lightContainer = Color(0xFFFEF3C7),
        onDarkContainer = Color(0xFFFEF3C7),
        onLightContainer = Color(0xFF92400E),
        gradientStart = Color(0xFFD97706),
        gradientMid = Color(0xFFF59E0B),
        gradientEnd = Color(0xFFB45309)
    );

    companion object {
        fun fromKey(key: String?): AccentColorOption {
            return entries.firstOrNull { it.storageKey == key } ?: VELVET_ROSE
        }
    }
}

enum class DarkThemePalette(
    val storageKey: String,
    val displayName: String,
    val subtitle: String,
    val badge: String,
    val isHighReadability: Boolean
) {
    VELVET_CINEMA(
        storageKey = "velvet_cinema",
        displayName = "Cinematic Dark Red",
        subtitle = "Clean near-black obsidian surfaces with crisp #E50914 red accents.",
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
    val isLight: Boolean = false,
    val accentOption: AccentColorOption = AccentColorOption.VELVET_ROSE,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHover: Color,
    val primary: Color,
    val primaryHover: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val onPrimary: Color = Color.White,
    val softRose: Color,
    val accentStart: Color,
    val accentMid: Color,
    val accentEnd: Color,
    val navIndicator: Color,
    val glassSurface: Color = CinetraTokens.GlassSurface,
    val glassBorder: Color = CinetraTokens.GlassBorder,
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
    val redButton: Brush,
    val softBadge: Brush,
    val wordmark: Brush,
    val adGateGlow: Brush,
    val adBannerSurface: Brush
)

val VelvetCinemaColors = CinePaletteColors(
    palette = DarkThemePalette.VELVET_CINEMA,
    isLight = false,
    accentOption = AccentColorOption.VELVET_ROSE,
    background = CinetraTokens.Background,
    surface = CinetraTokens.Surface,
    surfaceElevated = CinetraTokens.SurfaceVariant,
    surfaceHover = CinetraTokens.SurfaceHover,
    primary = CinetraTokens.PrimaryRed,
    primaryHover = CinetraTokens.RedGradientStart,
    primaryContainer = Color(0xFF2A0D12),
    onPrimaryContainer = CinetraTokens.OnRed,
    onPrimary = CinetraTokens.OnRed,
    softRose = CinetraTokens.RedGradientStart,
    accentStart = CinetraTokens.RedGradientStart,
    accentMid = CinetraTokens.PrimaryRed,
    accentEnd = CinetraTokens.RedGradientEnd,
    navIndicator = Color(0xFF2A0D12),
    glassSurface = CinetraTokens.GlassSurface,
    glassBorder = CinetraTokens.GlassBorder,
    atmosphericTop = CinetraTokens.Background,
    atmosphericMid = CinetraTokens.Background,
    topBarStart = CinetraTokens.Background,
    topBarMid = CinetraTokens.Background,
    topBarEnd = CinetraTokens.Background,
    bottomBarTop = CinetraTokens.GlassSurface,
    bottomBarBottom = CinetraTokens.GlassSurface,
    gold = CinetraTokens.PrimaryRed,
    goldLight = CinetraTokens.RedGradientStart,
    textPrimary = CinetraTokens.TextPrimary,
    textSecondary = CinetraTokens.TextSecondary,
    textMuted = CinetraTokens.TextMuted,
    border = CinetraTokens.Border,
    success = CinetraTokens.Success,
    warning = CinetraTokens.Warning,
    info = CinetraTokens.Info
)

val CrispSlateColors = CinePaletteColors(
    palette = DarkThemePalette.CRISP_SLATE,
    isLight = false,
    accentOption = AccentColorOption.ELECTRIC_BLUE,
    background = Color(0xFF0A1019),
    surface = Color(0xFF121B29),
    surfaceElevated = Color(0xFF1B273A),
    surfaceHover = Color(0xFF24344D),
    primary = Color(0xFF0EA5E9),
    primaryHover = Color(0xFF38BDF8),
    primaryContainer = Color(0xFF132D46),
    onPrimaryContainer = Color(0xFFE0F2FE),
    onPrimary = Color.White,
    softRose = Color(0xFF7DD3FC),
    accentStart = Color(0xFF0284C7),
    accentMid = Color(0xFF0EA5E9),
    accentEnd = Color(0xFF2563EB),
    navIndicator = Color(0xFF183654),
    glassSurface = Color(0xCC121B29),
    glassBorder = Color(0xFF3A4E6C),
    atmosphericTop = Color(0xFF111D2E),
    atmosphericMid = Color(0xFF0D1521),
    topBarStart = Color(0xFF101927),
    topBarMid = Color(0xFF162235),
    topBarEnd = Color(0xFF101927),
    bottomBarTop = Color(0xCC121B29),
    bottomBarBottom = Color(0xCC121B29),
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
    isLight = false,
    accentOption = AccentColorOption.AMBER_GOLD,
    background = Color(0xFF110E0B),
    surface = Color(0xFF1B1612),
    surfaceElevated = Color(0xFF261F1A),
    surfaceHover = Color(0xFF332922),
    primary = Color(0xFFD97757),
    primaryHover = Color(0xFFE89174),
    primaryContainer = Color(0xFF3A241B),
    onPrimaryContainer = Color(0xFFFDE8DC),
    onPrimary = Color.White,
    softRose = Color(0xFFF4A261),
    accentStart = Color(0xFFE07A5F),
    accentMid = Color(0xFFC8644B),
    accentEnd = Color(0xFF8C4338),
    navIndicator = Color(0xFF3C2820),
    glassSurface = Color(0xCC1B1612),
    glassBorder = Color(0xFF45382E),
    atmosphericTop = Color(0xFF1E1712),
    atmosphericMid = Color(0xFF15110D),
    topBarStart = Color(0xFF1A1410),
    topBarMid = Color(0xFF231B15),
    topBarEnd = Color(0xFF17120E),
    bottomBarTop = Color(0xCC1B1612),
    bottomBarBottom = Color(0xCC1B1612),
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

val LightCinemaColors = CinePaletteColors(
    palette = DarkThemePalette.VELVET_CINEMA,
    isLight = true,
    accentOption = AccentColorOption.VELVET_ROSE,
    background = Color(0xFFF6F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFEEF1F7),
    surfaceHover = Color(0xFFE3E8F2),
    primary = CinetraTokens.PrimaryRed,
    primaryHover = CinetraTokens.RedGradientEnd,
    primaryContainer = Color(0xFFFFE5E7),
    onPrimaryContainer = Color(0xFF8F0E1F),
    onPrimary = Color.White,
    softRose = CinetraTokens.PrimaryRed,
    accentStart = CinetraTokens.RedGradientStart,
    accentMid = CinetraTokens.PrimaryRed,
    accentEnd = CinetraTokens.RedGradientEnd,
    navIndicator = Color(0xFFFFE5E7),
    glassSurface = Color(0xCCFFFFFF),
    glassBorder = Color(0xFFD8DEE9),
    atmosphericTop = Color(0xFFECEFF7),
    atmosphericMid = Color(0xFFF3F5FA),
    topBarStart = Color(0xFFFFFFFF),
    topBarMid = Color(0xFFF9FAFD),
    topBarEnd = Color(0xFFFFFFFF),
    bottomBarTop = Color(0xCCFFFFFF),
    bottomBarBottom = Color(0xCCF4F6FA),
    gold = CinetraTokens.PrimaryRed,
    goldLight = CinetraTokens.RedGradientEnd,
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF4B5563),
    textMuted = Color(0xFF6B7280),
    border = Color(0xFFD8DEE9),
    success = Color(0xFF059669),
    warning = Color(0xFFD97706),
    info = Color(0xFF2563EB)
)

fun paletteColorsFor(
    palette: DarkThemePalette,
    isLight: Boolean = false,
    accentOption: AccentColorOption? = null
): CinePaletteColors {
    val base = if (isLight) {
        LightCinemaColors.copy(palette = palette)
    } else {
        when (palette) {
            DarkThemePalette.VELVET_CINEMA -> VelvetCinemaColors
            DarkThemePalette.CRISP_SLATE -> CrispSlateColors
            DarkThemePalette.WARM_AMBER_NIGHT -> WarmAmberNightColors
        }
    }
    if (accentOption == null) return base

    return base.copy(
        accentOption = accentOption,
        primary = accentOption.primaryColor,
        primaryHover = accentOption.hoverColor,
        primaryContainer = if (isLight) accentOption.lightContainer else accentOption.darkContainer,
        onPrimaryContainer = if (isLight) accentOption.onLightContainer else accentOption.onDarkContainer,
        softRose = if (isLight) accentOption.primaryColor else accentOption.softTint,
        accentStart = accentOption.gradientStart,
        accentMid = accentOption.gradientMid,
        accentEnd = accentOption.gradientEnd,
        navIndicator = if (isLight) accentOption.lightContainer else accentOption.darkContainer
    )
}

fun buildPaletteGradients(colors: CinePaletteColors): CinePaletteGradients {
    return CinePaletteGradients(
        appBackdrop = Brush.verticalGradient(
            colors = listOf(
                colors.background,
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
                colors.glassSurface,
                colors.glassSurface
            )
        ),
        primaryCta = Brush.horizontalGradient(
            colors = listOf(
                colors.accentStart,
                colors.accentEnd
            )
        ),
        redButton = Brush.horizontalGradient(
            colors = listOf(
                colors.accentStart,
                colors.accentEnd
            )
        ),
        softBadge = Brush.horizontalGradient(
            colors = listOf(
                colors.accentStart,
                colors.accentEnd
            )
        ),
        wordmark = Brush.horizontalGradient(
            colors = listOf(
                colors.accentStart,
                colors.accentEnd
            )
        ),
        adGateGlow = Brush.radialGradient(
            colors = listOf(
                colors.accentEnd.copy(alpha = 0.20f),
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

// Legacy top-level defaults (point to Cinetra Dark Red baseline for static contexts)
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
