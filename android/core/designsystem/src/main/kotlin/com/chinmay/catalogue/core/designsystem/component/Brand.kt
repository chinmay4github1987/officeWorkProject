package com.chinmay.catalogue.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.chinmay.catalogue.core.designsystem.R
import com.chinmay.catalogue.core.designsystem.icon.CatalogueIcons
import com.chinmay.catalogue.core.designsystem.theme.PillShape
import com.chinmay.catalogue.core.designsystem.theme.Spacing

/**
 * The reference's banner edge: rounded top corners and a soft wave along the bottom.
 * [cornerRadius] in px-independent dp; the wave rises by [waveHeightFraction] of the height.
 */
class WaveShape(private val cornerRadius: Float = 28f, private val waveHeightFraction: Float = 0.08f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = cornerRadius * density.density
        val wave = size.height * waveHeightFraction
        val path = Path().apply {
            moveTo(0f, r)
            quadraticTo(0f, 0f, r, 0f)
            lineTo(size.width - r, 0f)
            quadraticTo(size.width, 0f, size.width, r)
            lineTo(size.width, size.height - wave)
            // One gentle S-curve across the bottom.
            cubicTo(
                size.width * 0.72f, size.height + wave * 0.6f,
                size.width * 0.32f, size.height - wave * 2.2f,
                0f, size.height - wave * 0.4f,
            )
            close()
        }
        return Outline.Generic(path)
    }
}

/** A section title with an optional "See all" action on the right, as in the reference ("Shop by Category · See all"). */
@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/**
 * Stock status. Fake Store has no stock levels, so this never claims "in stock": it says the product can be ordered
 * on request, which is what an enquiry does.
 */
@Composable
fun AvailabilityBadge(modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, shape = PillShape, modifier = modifier) {
        Row(
            Modifier.padding(horizontal = Spacing.sm + 2.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(CatalogueIcons.Box, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(stringResource(R.string.core_designsystem_available_on_request), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * The light/dark switch for the side drawer: a pill with a sun half and a moon half; the chosen half is filled.
 * Each half is a radio-style option for screen readers ("Light theme, selected").
 */
@Composable
fun ThemeSwitch(darkSelected: Boolean, onSelect: (dark: Boolean) -> Unit, modifier: Modifier = Modifier) {
    Surface(shape = PillShape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Row(Modifier.padding(4.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ThemeOption(CatalogueIcons.Sun, stringResource(R.string.core_designsystem_theme_light), selected = !darkSelected) { onSelect(false) }
            ThemeOption(CatalogueIcons.Moon, stringResource(R.string.core_designsystem_theme_dark), selected = darkSelected) { onSelect(true) }
        }
    }
}

@Composable
private fun ThemeOption(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) colors.primary else colors.surfaceContainer, label = "themeOption")
    val content by animateColorAsState(if (selected) colors.onPrimary else colors.onSurfaceVariant, label = "themeOptionContent")
    Surface(
        shape = PillShape,
        color = container,
        contentColor = content,
        modifier = Modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        Row(
            Modifier.height(40.dp).padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
