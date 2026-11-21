package com.chinmay.catalogue.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chinmay.catalogue.core.designsystem.component.focusRing
import com.chinmay.catalogue.core.designsystem.component.pressScale
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.model.Category

/** Tinted category card. Colours cycle by position, so a new category needs no design work. */
@Composable
fun CategoryCard(category: Category, index: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accents = CatalogueTheme.extendedColors.categoryAccents
    val (container, content) = accents[index % accents.size]
    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interaction,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = modifier.pressScale(interaction).focusRing(interaction, MaterialTheme.shapes.large).fillMaxWidth().height(104.dp),
    ) {
        Box(Modifier.fillMaxWidth().padding(Spacing.lg)) {
            Text(
                category.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.TopStart).padding(end = Spacing.xl),
            )
            // The arrow sits in a soft circle so the card reads as "go somewhere", not just a label.
            Box(
                Modifier.align(Alignment.BottomEnd).size(32.dp).background(content.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
