package com.chinmay.catalogue.feature.product

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.catalogue.core.designsystem.component.ExpandableText
import com.chinmay.catalogue.core.designsystem.component.PrimaryButton
import com.chinmay.catalogue.core.designsystem.component.PrimaryButtonHeight
import com.chinmay.catalogue.core.designsystem.component.RatingBadge
import com.chinmay.catalogue.core.designsystem.component.SkeletonBox
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.Money
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.model.Rating
import com.chinmay.catalogue.core.ui.DataErrorState
import com.chinmay.catalogue.core.ui.ProductImage
import com.chinmay.catalogue.core.ui.formatted

@Composable
internal fun ProductRoute(
    onBack: () -> Unit,
    onEnquire: (String) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onViewCart: () -> Unit,
    onSignInRequired: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val added = stringResource(R.string.feature_product_added)
    val viewCart = stringResource(R.string.feature_product_view_cart)
    val failed = stringResource(R.string.feature_product_add_failed)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ProductEvent.AddedToCart -> {
                    val result = snackbarHostState.showSnackbar(added, actionLabel = viewCart, withDismissAction = true, duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) onViewCart()
                }
                ProductEvent.SignInRequired -> onSignInRequired()
                is ProductEvent.AddToCartFailed -> snackbarHostState.showSnackbar(failed, withDismissAction = true)
            }
        }
    }
    ProductScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAddToCart = viewModel::addToCart,
        onEnquire = { onEnquire(viewModel.productId) },
        onCategoryClick = onCategoryClick,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductScreen(
    state: ProductUiState,
    onBack: () -> Unit,
    onAddToCart: () -> Unit,
    onEnquire: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    onRetry: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollState = rememberScrollState()
    // Where the product title ends inside the scrolling content. Once it scrolls off screen,
    // the title moves into the top bar so you always know which product you're looking at.
    var titleBottomPx by remember { mutableIntStateOf(Int.MAX_VALUE) }
    val titleScrolledAway by remember { derivedStateOf { scrollState.value > titleBottomPx } }
    val product = (state as? ProductUiState.Success)?.product

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(visible = titleScrolledAway && product != null, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            product?.title.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.feature_product_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (titleScrolledAway) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.background,
                ),
            )
        },
        // The calls to action stay on screen however long the description is.
        bottomBar = {
            if (state is ProductUiState.Success) ActionBar(isAdding = state.isAddingToCart, onAddToCart = onAddToCart, onEnquire = onEnquire)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (state) {
            ProductUiState.Loading -> LoadingContent(Modifier.padding(padding))
            is ProductUiState.Error -> DataErrorState(state.error, onRetry, Modifier.padding(padding))
            is ProductUiState.Success -> ProductDetails(
                product = state.product,
                scrollState = scrollState,
                onTitlePositioned = { titleBottomPx = it },
                onCategoryClick = onCategoryClick,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun ProductDetails(
    product: Product,
    scrollState: ScrollState,
    onTitlePositioned: (Int) -> Unit,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ProductImage(
            url = product.imageUrl,
            contentDescription = product.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            shape = MaterialTheme.shapes.extraLarge,
            padding = PaddingValues(Spacing.xxl),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            AssistChip(onClick = { onCategoryClick(product.category) }, label = { Text(product.category.name) })
            RatingBadge(product.rating.average, product.rating.count)
        }
        Text(
            product.title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .semantics { heading() }
                .onGloballyPositioned { onTitlePositioned((it.positionInParent().y + it.size.height).toInt()) },
        )
        Text(product.price.formatted(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(Modifier.padding(vertical = Spacing.sm), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            stringResource(R.string.feature_product_about),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        // A long description no longer pushes everything else away; the preview is enough to scan.
        ExpandableText(product.description, Modifier.padding(bottom = Spacing.xl))
    }
}

@Composable
private fun ActionBar(isAdding: Boolean, onAddToCart: () -> Unit, onEnquire: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
        Column(
            Modifier.navigationBarsPadding().padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Cart is the main path (ask about several products at once); a quick question about this one is secondary.
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(
                    onClick = onEnquire,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f).height(PrimaryButtonHeight),
                ) {
                    Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.feature_product_enquire_short), modifier = Modifier.padding(start = Spacing.sm))
                }
                PrimaryButton(
                    text = stringResource(R.string.feature_product_add_to_cart),
                    onClick = onAddToCart,
                    icon = Icons.Filled.ShoppingCart,
                    loading = isAdding,
                    modifier = Modifier.weight(1.4f),
                )
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
        SkeletonBox(Modifier.fillMaxWidth(0.3f).height(32.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(120.dp))
    }
}

@Preview
@Composable
private fun ProductScreenPreview() {
    val product = Product(
        "1", "Wireless Noise-Cancelling Headphones", "Comfortable over-ear headphones with 30 hours of battery life. ".repeat(8),
        Money(99.0, "USD"), Category("electronics", "Electronics"), "", Rating(4.6, 210),
    )
    CatalogueTheme {
        ProductScreen(ProductUiState.Success(product), onBack = {}, onAddToCart = {}, onEnquire = {}, onCategoryClick = {}, onRetry = {})
    }
}
