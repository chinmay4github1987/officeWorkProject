package com.chinmay.catalogue.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chinmay.catalogue.core.designsystem.component.AvailabilityBadge
import com.chinmay.catalogue.core.designsystem.component.pressScale
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.model.Product

/**
 * Maniro-style Minimalist Product Card:
 * - Neutral rounded image container with top-right favorite heart button.
 * - Bold product title, clean rating star + review count.
 * - Prominent price in bold typography.
 */
@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAddToCart: (() -> Unit)? = null,
    detailed: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    var isFavorite by remember { mutableStateOf(false) }
    val imageShape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Image Container with Favorite Icon at top-right
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.92f)
                .clip(imageShape),
        ) {
            ProductImage(
                url = product.imageUrl,
                contentDescription = product.title,
                shape = imageShape,
                padding = PaddingValues(Spacing.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.92f),
            )
            // Favorite Heart Button (Maniro style)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.88f))
                    .clickable { isFavorite = !isFavorite },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFE2614D) else Color(0xFF777777),
                    modifier = Modifier.size(17.dp),
                )
            }
        }

        // Product Info (Maniro style)
        Text(
            text = product.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Rating row: Star + 4.9 (1208 Reviews)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = CatalogueTheme.extendedColors.ratingStar,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "${product.rating.average}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "(${product.rating.count} Reviews)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Price
        Text(
            text = product.price.formatted(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (detailed) {
            AvailabilityBadge(Modifier.padding(top = 2.dp))
            TextButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                modifier = Modifier.heightIn(min = 32.dp),
            ) {
                Text(stringResource(R.string.core_ui_view_details), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
