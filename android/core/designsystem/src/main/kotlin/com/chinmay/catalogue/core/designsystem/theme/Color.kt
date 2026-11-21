package com.chinmay.catalogue.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/*
 * "Dark luxe" brand palette. Three rules keep it looking expensive rather than loud:
 *   1. One accent only: champagne gold, used for actions, prices and focus. Never for large areas.
 *   2. Warm neutrals: charcoal with a hint of brown and warm off-white text, never pure #000 / #FFF.
 *   3. Depth from tone, not shadow: each raised layer (background → card → field → pressed) is a step lighter.
 * The app is dark by default (see CatalogueTheme); the light scheme is the same brand on ivory.
 *
 * Contrast (WCAG 2.2 AA needs 4.5:1 for text, 3:1 for icons and borders), for the pairs screens use:
 *   text #F4F1EA on #0E0E10 ≈ 17:1 · secondary text #A8A49C on card #18181B ≈ 7.0:1
 *   champagne #D6B57A on #0E0E10 ≈ 9.9:1 · button label #1C160A on champagne ≈ 9.3:1
 *   light scheme: #8A6512 on ivory #F7F4EE ≈ 5.1:1
 */
private val Gold = Color(0xFFD6B57A) // champagne: muted, so it reads as premium rather than yellow
private val Charcoal = Color(0xFF0E0E10)

internal val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1C160A),
    primaryContainer = Color(0xFF2E281D),
    onPrimaryContainer = Color(0xFFF2E3C4),
    inversePrimary = Color(0xFF8A6512),
    secondary = Color(0xFFCFC6B4),
    onSecondary = Color(0xFF1F1B12),
    secondaryContainer = Color(0xFF2A2720),
    onSecondaryContainer = Color(0xFFEDE4D0),
    tertiary = Color(0xFF9FD3C7),
    onTertiary = Color(0xFF0B2A24),
    tertiaryContainer = Color(0xFF1C3530),
    onTertiaryContainer = Color(0xFFCDEDE5),
    background = Charcoal,
    onBackground = Color(0xFFF4F1EA),
    surface = Charcoal,
    onSurface = Color(0xFFF4F1EA),
    surfaceVariant = Color(0xFF232326),
    onSurfaceVariant = Color(0xFFA8A49C),
    surfaceTint = Gold,
    inverseSurface = Color(0xFFF4F1EA),
    inverseOnSurface = Color(0xFF1B1B20),
    // Cards and bars use the "lowest" slot, so here it is a raised charcoal that lifts content off the background.
    surfaceContainerLowest = Color(0xFF18181B),
    surfaceContainerLow = Color(0xFF141416),
    surfaceContainer = Color(0xFF1C1C1F),
    surfaceContainerHigh = Color(0xFF232326),
    surfaceContainerHighest = Color(0xFF2C2C30),
    outline = Color(0xFF3F3D39),
    outlineVariant = Color(0xFF2A2927),
    scrim = Color(0xFF000000),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0907),
    errorContainer = Color(0xFF4A1512),
    onErrorContainer = Color(0xFFFFDAD6),
)

internal val LightColors = lightColorScheme(
    primary = Color(0xFF8A6512),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF6DFA8),
    onPrimaryContainer = Color(0xFF2B1F02),
    secondary = Color(0xFF5E584B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE4D0),
    onSecondaryContainer = Color(0xFF1F1B12),
    tertiary = Color(0xFF2E6B5F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCDEDE5),
    onTertiaryContainer = Color(0xFF0B2A24),
    background = Color(0xFFF7F4EE),
    onBackground = Color(0xFF16161A),
    surface = Color(0xFFF7F4EE),
    onSurface = Color(0xFF16161A),
    surfaceVariant = Color(0xFFEAE5DA),
    onSurfaceVariant = Color(0xFF5C5A55),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF9F5),
    surfaceContainer = Color(0xFFF1EDE5),
    surfaceContainerHigh = Color(0xFFEAE5DA),
    surfaceContainerHighest = Color(0xFFE3DDD0),
    outline = Color(0xFFCBC4B6),
    outlineVariant = Color(0xFFE4DED2),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

/** Brand colours Material's scheme has no slot for. */
@Immutable
data class CatalogueExtendedColors(
    val ratingStar: Color,
    /** Fake Store photos have white backgrounds, so images always sit on a light tile, even in dark mode. */
    val productImageBackground: Color,
    /** Soft tints for category cards: (container, content) pairs, cycled by index. */
    val categoryAccents: List<Pair<Color, Color>>,
)

internal val DarkExtendedColors = CatalogueExtendedColors(
    ratingStar = Gold,
    productImageBackground = Color(0xFFFFFFFF),
    // Deep jewel tones with light text: they read as "luxe" on charcoal without shouting.
    categoryAccents = listOf(
        Color(0xFF2E2614) to Color(0xFFF3D9A0),
        Color(0xFF16302B) to Color(0xFFA8E0D5),
        Color(0xFF301A26) to Color(0xFFF0B8D0),
        Color(0xFF1A2236) to Color(0xFFB8C8F0),
        Color(0xFF30211A) to Color(0xFFF0C4A8),
    ),
)

internal val LightExtendedColors = CatalogueExtendedColors(
    ratingStar = Color(0xFFC8901E),
    productImageBackground = Color(0xFFFFFFFF),
    categoryAccents = listOf(
        Color(0xFFF6E7C4) to Color(0xFF4A3608),
        Color(0xFFD9F0EA) to Color(0xFF14453B),
        Color(0xFFF6DDE8) to Color(0xFF5C1F3A),
        Color(0xFFDDE5F6) to Color(0xFF1E2E55),
        Color(0xFFF6E1D6) to Color(0xFF5C2F14),
    ),
)
