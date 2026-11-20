package com.chinmay.catalogue.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.chinmay.catalogue.core.designsystem.component.pressScale
import androidx.compose.ui.text.style.TextOverflow
import com.chinmay.catalogue.core.designsystem.component.RatingBadge
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.model.Product

/** Grid tile: image, two-line title, price and rating. The whole card is one tap target. */
@Composable
fun ProductTile(product: Product, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interaction,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = modifier.pressScale(interaction),
    ) {
        ProductImage(
            url = product.imageUrl,
            contentDescription = null, // the title below already describes the product
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(Spacing.sm),
        )
        Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                product.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(product.price.formatted(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            RatingBadge(product.rating.average, product.rating.count, Modifier.padding(bottom = Spacing.xs))
        }
    }
}
