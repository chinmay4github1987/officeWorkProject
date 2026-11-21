package com.chinmay.catalogue.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }

/** The brand look is dark. Also used by :app to pick light status-bar icons. */
const val BRAND_IS_DARK = true

/**
 * The app theme. Dynamic colour (Material You) is deliberately off: the business wants a consistent brand.
 * The brand is "dark luxe", so the app is dark whatever the system setting. Set [BRAND_IS_DARK] to false
 * to follow the system instead (the light scheme is the same brand on ivory).
 */
@Composable
fun CatalogueTheme(
    darkTheme: Boolean = BRAND_IS_DARK || isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = CatalogueTypography,
            shapes = CatalogueShapes,
            content = content,
        )
    }
}

object CatalogueTheme {
    val extendedColors: CatalogueExtendedColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current
}
