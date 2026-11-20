package com.chinmay.catalogue.feature.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.catalogue.core.designsystem.component.RatingBadge
import com.chinmay.catalogue.core.designsystem.component.SkeletonBox
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.ui.DataErrorState
import com.chinmay.catalogue.core.ui.ProductImage
import com.chinmay.catalogue.core.ui.formatted

@Composable
internal fun ProductRoute(
    onBack: () -> Unit,
    onEnquire: (String) -> Unit,
    onCategoryClick: (Category) -> Unit,
    viewModel: ProductViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProductScreen(state, onBack, { onEnquire(viewModel.productId) }, onCategoryClick, viewModel::retry)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductScreen(
    state: ProductUiState,
    onBack: () -> Unit,
    onEnquire: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.feature_product_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        // The call to action stays on screen however long the description is.
        bottomBar = { if (state is ProductUiState.Success) EnquireBar(onEnquire) },
    ) { padding ->
        when (state) {
            ProductUiState.Loading -> LoadingContent(Modifier.padding(padding))
            is ProductUiState.Error -> DataErrorState(state.error, onRetry, Modifier.padding(padding))
            is ProductUiState.Success -> ProductDetails(state.product, onCategoryClick, Modifier.padding(padding))
        }
    }
}

@Composable
private fun ProductDetails(product: Product, onCategoryClick: (Category) -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ProductImage(
            url = product.imageUrl,
            contentDescription = product.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            shape = MaterialTheme.shapes.extraLarge,
            padding = PaddingValues(Spacing.xxl),
        )
        AssistChip(onClick = { onCategoryClick(product.category) }, label = { Text(product.category.name) })
        Text(product.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(product.price.formatted(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            RatingBadge(product.rating.average, product.rating.count)
        }
        HorizontalDivider(Modifier.padding(vertical = Spacing.sm))
        Text(stringResource(R.string.feature_product_about), style = MaterialTheme.typography.titleMedium)
        Text(
            product.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.xl),
        )
    }
}

@Composable
private fun EnquireBar(onEnquire: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
        Column(
            Modifier.navigationBarsPadding().padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Button(onClick = onEnquire, modifier = Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.medium) {
                Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.feature_product_enquire), modifier = Modifier.padding(start = Spacing.sm))
            }
            Text(
                stringResource(R.string.feature_product_enquire_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    Column(modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(1f), MaterialTheme.shapes.extraLarge)
        SkeletonBox(Modifier.fillMaxWidth(0.4f).height(32.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(56.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(120.dp))
    }
}
