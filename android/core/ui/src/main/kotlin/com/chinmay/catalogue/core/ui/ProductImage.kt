package com.chinmay.catalogue.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.compose.LocalPlatformContext
import com.chinmay.catalogue.core.designsystem.component.SkeletonBox
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme

/**
 * Product photo on a white card (Fake Store images have white backgrounds), with a skeleton while loading
 * and a quiet fallback if the image fails. ContentScale.Fit so products are never cropped.
 */
@Composable
fun ProductImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    padding: PaddingValues = PaddingValues(12.dp),
) {
    Box(
        modifier
            .clip(shape)
            .background(CatalogueTheme.extendedColors.productImageBackground),
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current).data(url).crossfade(true).build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            loading = { SkeletonBox(Modifier.fillMaxSize(), shape) },
            error = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No image", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                }
            },
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}
