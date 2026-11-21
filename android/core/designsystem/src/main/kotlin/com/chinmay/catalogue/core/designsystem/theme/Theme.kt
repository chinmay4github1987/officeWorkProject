package com.chinmay.catalogue.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/**
 * The app theme: "Warm Coast" in light or dark. The customer picks which in the side drawer (sun / moon);
 * :app passes it in. Dynamic colour (Material You) is deliberately off: the business wants a consistent brand.
 */
@Composable
fun CatalogueTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
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
