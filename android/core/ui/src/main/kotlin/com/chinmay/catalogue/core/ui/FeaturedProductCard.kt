package com.chinmay.catalogue.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.chinmay.catalogue.core.designsystem.component.focusRing
import com.chinmay.catalogue.core.designsystem.component.pressScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chinmay.catalogue.core.designsystem.component.RatingBadge
import com.chinmay.catalogue.core.designsystem.component.SkeletonBox
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.model.Product

/** Large card for "Top rated" carousels. The rank badge makes the curated order visible. */
@Composable
fun FeaturedProductCard(product: Product, rank: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interaction,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.pressScale(interaction).focusRing(interaction, MaterialTheme.shapes.large).width(FeaturedCardWidth),
    ) {
        Box {
            ProductImage(
                url = product.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp).padding(Spacing.sm),
                padding = PaddingValues(20.dp),
            )
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(Spacing.md).align(Alignment.TopStart),
            ) {
                Text(
                    "#$rank",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                product.category.name.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(product.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(product.price.formatted(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                RatingBadge(product.rating.average, product.rating.count)
            }
        }
    }
}

val FeaturedCardWidth = 264.dp

@Composable
fun FeaturedProductCardSkeleton(modifier: Modifier = Modifier) {
    SkeletonBox(modifier.width(FeaturedCardWidth).height(340.dp), MaterialTheme.shapes.large)
}
