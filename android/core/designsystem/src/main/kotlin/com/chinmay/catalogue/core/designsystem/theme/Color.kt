package com.chinmay.catalogue.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/*
 * Brand palette (placeholder until the creative agency delivers): deep indigo for action,
 * warm paper neutrals for browsing, amber for ratings. Change these values to re-skin the whole app.
 */
internal val LightColors = lightColorScheme(
    primary = Color(0xFF4F3CE8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE7E3FF),
    onPrimaryContainer = Color(0xFF1E1470),
    secondary = Color(0xFF5E5A6E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9E6F2),
    onSecondaryContainer = Color(0xFF1D1B26),
    tertiary = Color(0xFFB45309),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFEDD5),
    onTertiaryContainer = Color(0xFF4A2504),
    background = Color(0xFFF6F5F2),
    onBackground = Color(0xFF17171C),
    surface = Color(0xFFF6F5F2),
    onSurface = Color(0xFF17171C),
    surfaceVariant = Color(0xFFEAE7E2),
    onSurfaceVariant = Color(0xFF5D5A66),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBFAF8),
    surfaceContainer = Color(0xFFF0EEEA),
    surfaceContainerHigh = Color(0xFFEAE7E2),
    surfaceContainerHighest = Color(0xFFE3E0DA),
    outline = Color(0xFFC9C5CF),
    outlineVariant = Color(0xFFE2DFE6),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFB3A8FF),
    onPrimary = Color(0xFF221776),
    primaryContainer = Color(0xFF3A2DB0),
    onPrimaryContainer = Color(0xFFE7E3FF),
    secondary = Color(0xFFC9C4D9),
    onSecondary = Color(0xFF2F2C3B),
    secondaryContainer = Color(0xFF45424F),
    onSecondaryContainer = Color(0xFFE6E1F5),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF3F2A00),
    tertiaryContainer = Color(0xFF4A3410),
    onTertiaryContainer = Color(0xFFFFEDD5),
    background = Color(0xFF121116),
    onBackground = Color(0xFFE7E5EC),
    surface = Color(0xFF121116),
    onSurface = Color(0xFFE7E5EC),
    surfaceVariant = Color(0xFF26252C),
    onSurfaceVariant = Color(0xFFB0ADBA),
    surfaceContainerLowest = Color(0xFF0D0C10),
    surfaceContainerLow = Color(0xFF17161B),
    surfaceContainer = Color(0xFF1C1B21),
    surfaceContainerHigh = Color(0xFF26252C),
    surfaceContainerHighest = Color(0xFF312F37),
    outline = Color(0xFF4A4753),
    outlineVariant = Color(0xFF33313B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** Brand colours Material's scheme has no slot for. */
@Immutable
data class CatalogueExtendedColors(
    val ratingStar: Color,
    /** Fake Store photos have white backgrounds, so images always sit on white, even in dark mode. */
    val productImageBackground: Color,
    /** Soft tints for category cards: (container, content) pairs, cycled by index. */
    val categoryAccents: List<Pair<Color, Color>>,
)

internal val LightExtendedColors = CatalogueExtendedColors(
    ratingStar = Color(0xFFF59E0B),
    productImageBackground = Color(0xFFFFFFFF),
    categoryAccents = listOf(
        Color(0xFFE7E3FF) to Color(0xFF2A1F8F),
        Color(0xFFFFE8D6) to Color(0xFF7A3410),
        Color(0xFFDDF3E8) to Color(0xFF14532D),
        Color(0xFFFCE1EC) to Color(0xFF831843),
        Color(0xFFDCEBFB) to Color(0xFF1E3A8A),
    ),
)

internal val DarkExtendedColors = CatalogueExtendedColors(
    ratingStar = Color(0xFFFBBF24),
    productImageBackground = Color(0xFFFFFFFF),
    categoryAccents = listOf(
        Color(0xFF2B2370) to Color(0xFFE7E3FF),
        Color(0xFF4A2A14) to Color(0xFFFFE8D6),
        Color(0xFF173B2A) to Color(0xFFDDF3E8),
        Color(0xFF4A1A30) to Color(0xFFFCE1EC),
        Color(0xFF1B2F55) to Color(0xFFDCEBFB),
    ),
)
