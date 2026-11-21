package com.chinmay.catalogue.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chinmay.catalogue.core.designsystem.theme.PillShape
import com.chinmay.catalogue.core.designsystem.theme.Spacing

/**
 * A filter pill ("All · Electronics · Jewelery"): a soft surface when off; solid teal with light text (and [icon],
 * if given) when selected. At least 44dp tall so it's an easy tap target.
 */
@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = PillShape,
        color = if (selected) colors.primary else colors.surfaceContainerLowest,
        contentColor = if (selected) colors.onPrimary else colors.onSurface,
        border = if (selected) null else BorderStroke(1.dp, colors.outlineVariant),
        modifier = modifier
            .heightIn(min = 44.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.lg + 2.dp, vertical = Spacing.md - 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm - 2.dp),
        ) {
            if (icon != null && selected) Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** How a [CircleIconButton] looks. */
enum class CircleButtonStyle {
    /** Card-coloured with a hairline ring: back, close, menu. */
    Plain,

    /** Soft tinted: header actions (search, bag), like the reference's rounded-square buttons. */
    Tonal,

    /** Solid teal: the one highlighted action (run the search). */
    Primary,

    /** Solid coral: "add" actions (add to cart, +1). */
    Accent,
}

/** A round icon button in one of four [CircleButtonStyle]s. 48dp by default (the minimum touch target). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CircleButtonStyle = CircleButtonStyle.Plain,
    size: Dp = 48.dp,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (style) {
        CircleButtonStyle.Plain -> colors.surfaceContainerLowest to colors.onSurface
        CircleButtonStyle.Tonal -> colors.primaryContainer to colors.onPrimaryContainer
        CircleButtonStyle.Primary -> colors.primary to colors.onPrimary
        CircleButtonStyle.Accent -> colors.secondary to colors.onSecondary
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = container,
        contentColor = content,
        border = if (style == CircleButtonStyle.Plain) BorderStroke(1.dp, colors.outlineVariant) else null,
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(if (size < 40.dp) 18.dp else 22.dp),
                tint = if (enabled) LocalContentColor.current else colors.outline,
            )
        }
    }
}
