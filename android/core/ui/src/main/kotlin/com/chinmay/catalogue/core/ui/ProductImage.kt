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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
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
 * Product photo on the brand's soft grey tile, with a skeleton while loading
 * and a quiet fallback if the image fails. ContentScale.Fit so products are never cropped.
 * Fake Store photos have white backgrounds, so the photo is multiplied with the tile colour: white becomes the
 * tile grey and the product looks cut out, like the reference design's model shots. Other colours barely change.
 */
@Composable
fun ProductImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    padding: PaddingValues = PaddingValues(12.dp),
) {
    val tile = CatalogueTheme.extendedColors.productImageBackground
    Box(
        modifier
            .clip(shape)
            .background(tile),
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current).data(url).crossfade(true).build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(tile, BlendMode.Modulate),
            loading = { SkeletonBox(Modifier.fillMaxSize(), shape) },
            error = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.core_ui_no_image), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                }
            },
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}
