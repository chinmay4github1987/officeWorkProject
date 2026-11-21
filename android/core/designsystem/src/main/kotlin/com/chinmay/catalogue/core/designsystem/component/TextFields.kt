package com.chinmay.catalogue.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable

/**
 * The brand's input style: a softly raised field with a hairline border that turns champagne when focused.
 * Pass it as `colors = catalogueTextFieldColors()` to every OutlinedTextField so all forms match.
 * Labels and icons stay readable (≥ 4.5:1) in every state; errors keep Material's error colours.
 */
@Composable
fun catalogueTextFieldColors(): TextFieldColors {
    val c = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = c.surfaceContainerLowest,
        unfocusedContainerColor = c.surfaceContainerLowest,
        disabledContainerColor = c.surfaceContainerLow,
        errorContainerColor = c.surfaceContainerLowest,
        focusedBorderColor = c.primary,
        unfocusedBorderColor = c.outlineVariant,
        disabledBorderColor = c.outlineVariant,
        focusedLabelColor = c.primary,
        unfocusedLabelColor = c.onSurfaceVariant,
        focusedLeadingIconColor = c.primary,
        unfocusedLeadingIconColor = c.onSurfaceVariant,
        cursorColor = c.primary,
        focusedPlaceholderColor = c.onSurfaceVariant,
        unfocusedPlaceholderColor = c.onSurfaceVariant,
    )
}
