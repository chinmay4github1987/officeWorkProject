package com.chinmay.catalogue.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.model.Category

/** Tinted category card. Colours cycle by position, so a new category needs no design work. */
@Composable
fun CategoryCard(category: Category, index: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accents = CatalogueTheme.extendedColors.categoryAccents
    val (container, content) = accents[index % accents.size]
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = modifier.fillMaxWidth().height(96.dp),
    ) {
        Box(Modifier.fillMaxWidth().padding(Spacing.lg)) {
            Text(category.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.TopStart))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}
