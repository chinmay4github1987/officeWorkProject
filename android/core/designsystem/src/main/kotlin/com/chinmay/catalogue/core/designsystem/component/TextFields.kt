package com.chinmay.catalogue.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The brand's input style, as in the reference: a soft grey field with no visible border until it's focused,
 * then an ink outline. Pass it as `colors = catalogueTextFieldColors()` (with `shape = PillShape` for single-line
 * fields) to every OutlinedTextField so all forms match. Labels and icons stay ≥ 4.5:1; errors keep Material's.
 */
@Composable
fun catalogueTextFieldColors(): TextFieldColors {
    val c = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = c.surfaceContainer,
        unfocusedContainerColor = c.surfaceContainer,
        disabledContainerColor = c.surfaceContainerLow,
        errorContainerColor = c.surfaceContainer,
        focusedBorderColor = c.onSurface,
        unfocusedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
        focusedLabelColor = c.onSurface,
        unfocusedLabelColor = c.onSurfaceVariant,
        focusedLeadingIconColor = c.onSurface,
        unfocusedLeadingIconColor = c.onSurfaceVariant,
        focusedTrailingIconColor = c.onSurface,
        unfocusedTrailingIconColor = c.onSurfaceVariant,
        cursorColor = c.onSurface,
        focusedPlaceholderColor = c.onSurfaceVariant,
        unfocusedPlaceholderColor = c.onSurfaceVariant,
    )
}
