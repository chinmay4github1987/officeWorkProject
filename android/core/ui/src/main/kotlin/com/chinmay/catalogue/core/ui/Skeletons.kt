package com.chinmay.catalogue.core.ui

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chinmay.catalogue.core.designsystem.component.SkeletonBox

/** Same footprint as [ProductTile], so the grid doesn't jump when products load. */
@Composable
fun ProductTileSkeleton(modifier: Modifier = Modifier) {
    SkeletonBox(modifier.fillMaxWidth().aspectRatio(0.62f), MaterialTheme.shapes.large)
}
