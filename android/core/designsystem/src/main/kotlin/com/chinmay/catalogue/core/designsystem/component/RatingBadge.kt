package com.chinmay.catalogue.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.chinmay.catalogue.core.designsystem.R
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import java.util.Locale

/** "★ 4.6 (120)". Screen readers hear "Rated 4.6 out of 5 from 120 reviews" instead of symbols. */
@Composable
fun RatingBadge(average: Double, count: Int, modifier: Modifier = Modifier, compact: Boolean = false) {
    val value = String.format(Locale.getDefault(), "%.1f", average)
    val description = stringResource(R.string.core_designsystem_rating_description, value, count)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp),
    ) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = CatalogueTheme.extendedColors.ratingStar, modifier = Modifier.size(if (compact) 14.dp else 16.dp))
        Text(value, style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        // Compact (two-column grids on small phones) drops the visible count; screen readers still hear it.
        if (!compact) Text("($count)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
