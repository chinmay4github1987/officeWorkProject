package com.chinmay.catalogue.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/*
 * "Warm Coast" brand palette, after the reference shopping app: deep teal for words and the main action, soft mint
 * for calm surfaces, and coral as the one warm accent (cart, highlights). Backgrounds are a warm off-white rather
 * than pure white, which is gentler on the eyes; dark mode is a deep teal-navy, never pure black.
 *   1. Teal (primary) carries text-weight things: titles, prices, the main button, selected chips.
 *   2. Coral (secondary) marks "add" actions and small highlights; it only ever carries icons, never body text.
 *   3. Pastel tiles (categories) and soft greys separate content instead of borders.
 *
 * Contrast (WCAG 2.2 AA: 4.5:1 text, 3:1 icons/borders), for the pairs screens use:
 *   light: ink #1B2A30 on #FAF8F5 ≈ 14:1 · muted #56676C on #FAF8F5 ≈ 5.6:1 · white on teal #1F4A5C ≈ 9.6:1
 *          · teal on mint #D3F0E6 ≈ 8.0:1 · white icon on coral #E2614D ≈ 3.6:1
 *   dark:  #E8F1EE on #0F1E24 ≈ 14:1 · muted #A6B8BC on card #172A31 ≈ 7.1:1 · #0B2A30 on mint #8FD9C4 ≈ 9.8:1
 *          · dark icon #3B0E06 on coral #FF8A78 ≈ 7.5:1
 */
private val Teal = Color(0xFF1F4A5C)
private val Mint = Color(0xFF8FD9C4)
private val MintSoft = Color(0xFFD3F0E6)
private val Coral = Color(0xFFE2614D)
private val CoralDark = Color(0xFFFF8A78)
private val WarmWhite = Color(0xFFFAF8F5)
private val Ink = Color(0xFF1B2A30)
private val Night = Color(0xFF0F1E24)

internal val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = MintSoft,
    onPrimaryContainer = Color(0xFF0B3340),
    inversePrimary = Mint,
    secondary = Coral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE3DD),
    onSecondaryContainer = Color(0xFF5C1D12),
    tertiary = Color(0xFF2E7D6B),
    onTertiary = Color.White,
    tertiaryContainer = MintSoft,
    onTertiaryContainer = Color(0xFF0B3340),
    background = WarmWhite,
    onBackground = Ink,
    surface = WarmWhite,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1EEE9),
    onSurfaceVariant = Color(0xFF56676C),
    surfaceTint = Color.Transparent, // white cards stay white
    inverseSurface = Ink,
    inverseOnSurface = WarmWhite,
    surfaceContainerLowest = Color.White, // cards
    surfaceContainerLow = Color(0xFFF6F3EF),
    surfaceContainer = Color(0xFFF1EEE9), // fields, chips
    surfaceContainerHigh = Color(0xFFE9E5DF),
    surfaceContainerHighest = Color(0xFFE1DCD5),
    outline = Color(0xFFB9B3AA),
    outlineVariant = Color(0xFFE6E1DA),
    scrim = Color(0xFF000000),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFDE4E2),
    onErrorContainer = Color(0xFF7A1F1A),
)

internal val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF0B2A30),
    primaryContainer = Teal,
    onPrimaryContainer = MintSoft,
    inversePrimary = Teal,
    secondary = CoralDark,
    onSecondary = Color(0xFF3B0E06),
    secondaryContainer = Color(0xFF5A2A22),
    onSecondaryContainer = Color(0xFFFFDAD2),
    tertiary = Mint,
    onTertiary = Color(0xFF0B2A30),
    tertiaryContainer = Color(0xFF1E3F3A),
    onTertiaryContainer = MintSoft,
    background = Night,
    onBackground = Color(0xFFE8F1EE),
    surface = Night,
    onSurface = Color(0xFFE8F1EE),
    surfaceVariant = Color(0xFF1C3139),
    onSurfaceVariant = Color(0xFFA6B8BC),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFE8F1EE),
    inverseOnSurface = Night,
    surfaceContainerLowest = Color(0xFF172A31), // cards
    surfaceContainerLow = Color(0xFF14262C),
    surfaceContainer = Color(0xFF1C3139), // fields, chips
    surfaceContainerHigh = Color(0xFF223A43),
    surfaceContainerHighest = Color(0xFF2A444E),
    outline = Color(0xFF5E7880),
    outlineVariant = Color(0xFF2A3F47),
    scrim = Color(0xFF000000),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0907),
    errorContainer = Color(0xFF4A1512),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** Brand colours Material's scheme has no slot for. */
@Immutable
data class CatalogueExtendedColors(
    val ratingStar: Color,
    /**
     * The tile behind every product photo. Fake Store photos have white backgrounds, so ProductImage multiplies them
     * with this colour: the white becomes the tile and the product looks cut out. It stays light in dark mode too,
     * because darkening the photo would muddy the product.
     */
    val productImageBackground: Color,
    /** The home hero banner and category banners: a soft mint wash. */
    val heroGradient: Brush,
    /** Old price, struck through, next to a sale price. */
    val strikePrice: Color,
    /** Pastel tiles for categories: (container, content) pairs, cycled by index. */
    val categoryAccents: List<Pair<Color, Color>>,
    val dividerSubtle: Color,
    val overlayScrim: Color,
)

internal val LightExtendedColors = CatalogueExtendedColors(
    ratingStar = Color(0xFFF5A524),
    productImageBackground = Color(0xFFF3F0EB),
    heroGradient = Brush.linearGradient(listOf(Color(0xFFDDF4EC), Color(0xFFBEE9DD))),
    strikePrice = Color(0xFFC62828),
    categoryAccents = listOf(
        MintSoft to Color(0xFF0B3340),
        Color(0xFFFBDCD3) to Color(0xFF5C2418),
        Color(0xFFF6ECD6) to Color(0xFF4D3A12),
        Color(0xFFDCE9F5) to Color(0xFF173A5C),
        Color(0xFFE9E1F4) to Color(0xFF3A2457),
    ),
    dividerSubtle = Color(0xFFEFEBE6),
    overlayScrim = Color(0x00FAF8F5),
)

internal val DarkExtendedColors = CatalogueExtendedColors(
    ratingStar = Color(0xFFFFC145),
    productImageBackground = Color(0xFFE9E6E1),
    heroGradient = Brush.linearGradient(listOf(Color(0xFF1C3B44), Color(0xFF143038))),
    strikePrice = Color(0xFFFF8A80),
    categoryAccents = listOf(
        Color(0xFF1E3F3A) to Color(0xFFBFEBDD),
        Color(0xFF4A2A24) to Color(0xFFFBD3C9),
        Color(0xFF43391F) to Color(0xFFF3E2BC),
        Color(0xFF1E3347) to Color(0xFFCFE1F5),
        Color(0xFF33284A) to Color(0xFFE2D6F5),
    ),
    dividerSubtle = Color(0xFF1E3139),
    overlayScrim = Color(0x000F1E24),
)
