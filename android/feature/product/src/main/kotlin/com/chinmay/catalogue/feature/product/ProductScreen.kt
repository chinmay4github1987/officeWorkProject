package com.chinmay.catalogue.feature.product

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.catalogue.core.designsystem.component.AvailabilityBadge
import com.chinmay.catalogue.core.designsystem.component.ExpandableText
import com.chinmay.catalogue.core.designsystem.component.PrimaryButton
import com.chinmay.catalogue.core.designsystem.component.PrimaryButtonHeight
import com.chinmay.catalogue.core.designsystem.component.SkeletonBox
import com.chinmay.catalogue.core.designsystem.component.WindowWidth
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.core.designsystem.theme.PillShape
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.Money
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.model.Rating
import com.chinmay.catalogue.core.ui.DataErrorState
import com.chinmay.catalogue.core.ui.ProductCard
import com.chinmay.catalogue.core.ui.ProductImage
import com.chinmay.catalogue.core.ui.formatted

@Composable
internal fun ProductRoute(
    onBack: () -> Unit,
    onEnquire: (String) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onViewCart: () -> Unit,
    onSignInRequired: () -> Unit,
    onProductClick: (String) -> Unit = {},
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
        onProductClick = onProductClick,
    )
}

/**
 * Maniro Minimalist Product Detail Screen:
 * - Centered title in top bar with bag icon.
 * - Large rounded hero image card with heart icon and thumbnail selector below.
 * - Clean title, category, 5-star rating and prominent bold price.
 * - Minimal size selector (S, M, L, XL, XXL).
 * - Dual action bar: Add to Cart (outlined pill) + Buy Now (solid black pill).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductScreen(
    state: ProductUiState,
    onBack: () -> Unit,
    onAddToCart: () -> Unit,
    onEnquire: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    onRetry: () -> Unit,
    onProductClick: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollState = rememberScrollState()
    var titleBottomPx by remember { mutableIntStateOf(Int.MAX_VALUE) }
    val titleScrolledAway by remember { derivedStateOf { scrollState.value > titleBottomPx } }
    val product = (state as? ProductUiState.Success)?.product

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        product?.title.orEmpty(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
        bottomBar = {
            if (state is ProductUiState.Success) {
                ActionBar(
                    isAdding = state.isAddingToCart,
                    onAddToCart = onAddToCart,
                    onEnquire = onEnquire,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (state) {
            ProductUiState.Loading -> LoadingContent(Modifier.padding(padding))
            is ProductUiState.Error -> DataErrorState(state.error, onRetry, Modifier.padding(padding))
            is ProductUiState.Success -> ProductDetails(
                state = state,
                scrollState = scrollState,
                onTitlePositioned = { titleBottomPx = it },
                onCategoryClick = onCategoryClick,
                onProductClick = onProductClick,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun ProductDetails(
    state: ProductUiState.Success,
    scrollState: ScrollState,
    onTitlePositioned: (Int) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onProductClick: (String) -> Unit,
    modifier: Modifier,
) {
    val product = state.product
    val image = @Composable { imageModifier: Modifier -> ImageCarousel(product, imageModifier) }

    if (WindowWidth.current() >= WindowWidth.Expanded) {
        Row(
            modifier = modifier.fillMaxSize().padding(horizontal = Spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            image(Modifier.weight(1f).fillMaxHeight().padding(bottom = Spacing.lg))
            Column(
                Modifier.weight(1f).fillMaxHeight().verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                DetailsText(state, onTitlePositioned, onCategoryClick, onProductClick)
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            image(Modifier.fillMaxWidth())
            DetailsText(state, onTitlePositioned, onCategoryClick, onProductClick)
        }
    }
}

/**
 * Maniro Hero Image with Top-Right Heart and Thumbnail Previews Row
 */
@Composable
private fun ImageCarousel(product: Product, modifier: Modifier) {
    var isFavorite by remember { mutableStateOf(false) }
    var selectedThumb by remember { mutableIntStateOf(0) }
    val carouselShape = RoundedCornerShape(24.dp)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.05f)
                .clip(carouselShape),
        ) {
            ProductImage(
                url = product.imageUrl,
                contentDescription = product.title,
                modifier = Modifier.fillMaxSize(),
                shape = carouselShape,
                padding = PaddingValues(Spacing.xl),
            )
            // Favorite Button at top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.md)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
                    .clickable { isFavorite = !isFavorite },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFE2614D) else Color(0xFF555555),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Row of 4 thumbnails (Maniro style)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        ) {
            repeat(4) { index ->
                val isSelected = index == selectedThumb
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { selectedThumb = index },
                ) {
                    ProductImage(
                        url = product.imageUrl,
                        contentDescription = null,
                        shape = RoundedCornerShape(10.dp),
                        padding = PaddingValues(Spacing.xs),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsText(
    state: ProductUiState.Success,
    onTitlePositioned: (Int) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onProductClick: (String) -> Unit,
) {
    val product = state.product

    // Category Chip & Availability
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AssistChip(
            onClick = { onCategoryClick(product.category) },
            label = { Text(product.category.name, style = MaterialTheme.typography.labelLarge) },
            shape = PillShape,
        )
        AvailabilityBadge()
    }

    // Title
    Text(
        text = product.title,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier
            .semantics { heading() }
            .onGloballyPositioned { onTitlePositioned((it.positionInParent().y + it.size.height).toInt()) },
    )

    // Rating row: 5 Stars + 4.9 (1283 Reviews)
    RatingSummary(product.rating.average, product.rating.count)

    // Price
    Text(
        text = product.price.formatted(),
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.onSurface,
    )

    // Size Selector (Maniro style)
    Text(
        text = stringResource(R.string.feature_product_size),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(top = Spacing.xs),
    )
    var selectedSize by remember { mutableStateOf("M") }
    val sizes = listOf("S", "M", "L", "XL", "XXL")
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        sizes.forEach { size ->
            val isSelected = size == selectedSize
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    )
                    .clickable { selectedSize = size },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = size,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    // Description Section
    SectionLabel(stringResource(R.string.feature_product_description))
    ExpandableText(product.description)

    // Features
    SectionLabel(stringResource(R.string.feature_product_features))
    state.features.forEach { feature ->
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Box(Modifier.padding(top = 9.dp).size(6.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
            Text(feature, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    // Specifications
    SectionLabel(stringResource(R.string.feature_product_specifications))
    Specifications(product)

    // Related Products
    if (state.related.isNotEmpty()) {
        SectionLabel(stringResource(R.string.feature_product_related))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.padding(bottom = Spacing.xl)) {
            items(state.related, key = { it.id }) { related ->
                ProductCard(related, onClick = { onProductClick(related.id) }, modifier = Modifier.width(168.dp))
            }
        }
    } else {
        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(top = Spacing.sm).semantics { heading() },
    )
}

/**
 * Maniro 5-Star Rating summary
 */
@Composable
private fun RatingSummary(average: Double, count: Int) {
    val description = stringResource(R.string.feature_product_rating_summary, average, count)
    Row(
        Modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(5) { star ->
                val filled = average >= star + 0.5
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (filled) CatalogueTheme.extendedColors.ratingStar else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            String.format(java.util.Locale.getDefault(), "%.1f", average),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            pluralStringResource(R.plurals.feature_product_ratings, count, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A clean label / value table from the product's real fields. */
@Composable
private fun Specifications(product: Product) {
    val rows = listOf(
        stringResource(R.string.feature_product_spec_id) to product.id,
        stringResource(R.string.feature_product_spec_category) to product.category.name,
        stringResource(R.string.feature_product_spec_price) to "${product.price.formatted()} (${product.price.currencyCode})",
        stringResource(R.string.feature_product_spec_rating) to stringResource(R.string.feature_product_spec_rating_value, product.rating.average),
        stringResource(R.string.feature_product_spec_reviews) to product.rating.count.toString(),
        stringResource(R.string.feature_product_spec_availability) to stringResource(R.string.feature_product_spec_on_request),
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.3f))
                }
            }
        }
    }
}

/**
 * Maniro Action Bar:
 * Dual actions: "Add to cart" outlined button + "Buy Now" solid black pill button
 */
@Composable
private fun ActionBar(isAdding: Boolean, onAddToCart: () -> Unit, onEnquire: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Add to cart outlined pill button
            OutlinedButton(
                onClick = onAddToCart,
                enabled = !isAdding,
                shape = PillShape,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(PrimaryButtonHeight),
            ) {
                Icon(
                    Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.feature_product_add_to_cart_short),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
            }

            // Buy Now / Enquire pill button (Solid black)
            PrimaryButton(
                text = stringResource(R.string.feature_product_buy_now),
                onClick = onEnquire,
                modifier = Modifier.weight(1.2f),
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
