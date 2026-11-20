package com.chinmay.catalogue.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.chinmay.catalogue.core.designsystem.theme.Spacing

/** Height of every primary call to action, so the main action looks and sits the same on every screen. */
val PrimaryButtonHeight = 56.dp

/**
 * The one main action on a screen. Full width with a 56dp height: an easy thumb target.
 * While [loading] it shows a spinner and ignores taps, so the action can't be sent twice.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().height(PrimaryButtonHeight),
    ) {
        when {
            loading -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
            icon != null -> Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        val label = if (loading) loadingText ?: text else text
        Text(label, modifier = if (loading || icon != null) Modifier.padding(start = Spacing.sm) else Modifier)
    }
}
