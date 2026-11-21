package com.chinmay.catalogue.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.catalogue.core.designsystem.component.ReadableWidth
import com.chinmay.catalogue.core.designsystem.component.SearchPill
import com.chinmay.catalogue.core.designsystem.component.SkeletonBox
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.core.designsystem.theme.PillShape
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.model.CatalogueHome
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.Money
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.model.Rating
import com.chinmay.catalogue.core.ui.DataErrorState
import com.chinmay.catalogue.core.ui.ProductCard
import com.chinmay.catalogue.core.ui.ProductImage
import com.chinmay.catalogue.core.ui.errorText

/** Stateful entry point: gets the ViewModel from Hilt and hands plain state + callbacks to [HomeScreen]. */
@Composable
internal fun HomeRoute(
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onCartClick: () -> Unit,
    onSignInClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val cartMessage by viewModel.cartMessage.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        account = account,
        cartMessage = cartMessage,
        onMenuClick = onMenuClick,
        onSearchClick = onSearchClick,
        onProductClick = onProductClick,
        onCategoryClick = onCategoryClick,
        onCartClick = onCartClick,
        onAddToCart = { product -> if (account.user == null) onSignInClick() else viewModel.addToCart(product) },
        onCartMessageShown = viewModel::cartMessageShown,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
    )
}

/**
 * Maniro UI Kit - Minimalist Online Store Home Screen:
 * - Clean brand header with logo, search icon & cart count badge.
 * - Rounded search bar with filter icon.
 * - "Popular Product" carousel with clean category & featured thumbnails.
 * - Minimalist filter pills (Active black pill with white text).
 * - 2-Column Product Grid with neutral grey photo tiles and heart badges.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onSearchClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    account: HomeAccount = HomeAccount(),
    cartMessage: CartMessage? = null,
    onMenuClick: () -> Unit = {},
    onCartClick: () -> Unit = {},
    onAddToCart: (Product) -> Unit = {},
    onCartMessageShown: () -> Unit = {},
) {
    val snackbar = remember { SnackbarHostState() }
    CartMessageSnackbar(cartMessage, snackbar, onCartClick, onCartMessageShown)

    // Local selected category filter for the pills (null means "All")
    var selectedCategorySlug by remember { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPadding()) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = (state as? HomeUiState.Success)?.isRefreshing == true,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                // 1. Maniro Brand Header
                item {
                    ManiroHeader(
                        account = account,
                        onMenuClick = onMenuClick,
                        onSearchClick = onSearchClick,
                        onCartClick = onCartClick,
                    )
                }

                // 2. Search Pill
                item {
                    Box(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                        SearchPill(
                            placeholder = stringResource(R.string.feature_home_search_placeholder),
                            onClick = onSearchClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = ReadableWidth),
                        )
                    }
                }

                when (state) {
                    HomeUiState.Loading -> item { LoadingContent() }
                    is HomeUiState.Error -> item { DataErrorState(state.error, onRetry) }
                    is HomeUiState.Success -> {
                        // 3. Popular Product Section (Maniro style)
                        if (state.home.featured.isNotEmpty()) {
                            item {
                                SectionHeaderRow(
                                    title = stringResource(R.string.feature_home_popular_products),
                                    actionLabel = stringResource(R.string.feature_home_see_more),
                                    onAction = { onCategoryClick(state.home.categories.firstOrNull() ?: Category("", "")) },
                                )
                            }
                            item {
                                PopularProductsRow(
                                    featured = state.home.featured,
                                    onProductClick = onProductClick,
                                )
                            }
                        }

                        // 4. Category Filter Pills (All + Categories in Maniro black/grey pills)
                        if (state.home.categories.isNotEmpty()) {
                            item {
                                CategoryPillRow(
                                    categories = state.home.categories,
                                    selectedSlug = selectedCategorySlug,
                                    onSelect = { selectedCategorySlug = it },
                                    onCategoryDoubleClick = onCategoryClick,
                                )
                            }
                        }

                        // 5. 2-Column Product Grid
                        val allProducts = (state.home.featured + state.picks).distinctBy { it.id }
                        val filteredProducts = if (selectedCategorySlug == null) {
                            allProducts
                        } else {
                            allProducts.filter { it.category.slug == selectedCategorySlug }
                        }

                        if (filteredProducts.isNotEmpty()) {
                            val rows = filteredProducts.chunked(2)
                            items(rows, key = { row -> row.first().id }) { pair ->
                                Row(
                                    Modifier.padding(horizontal = Spacing.lg),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                ) {
                                    pair.forEach { product ->
                                        ProductCard(
                                            product = product,
                                            onClick = { onProductClick(product.id) },
                                            onAddToCart = { onAddToCart(product) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    if (pair.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Maniro Top Header:
 * - Brand logo / title "Maniro"
 * - Circular Menu, Search, and Cart icons with count badge
 */
@Composable
private fun ManiroHeader(
    account: HomeAccount,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCartClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)),
            ) {
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.feature_home_menu),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = "Maniro",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Search button
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)),
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = stringResource(R.string.feature_home_search),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Cart with notification/badge
            val cartLabel = if (account.cartCount > 0) {
                pluralStringResource(R.plurals.feature_home_cart_with_items, account.cartCount, account.cartCount)
            } else {
                stringResource(R.string.feature_home_cart)
            }
            BadgedBox(
                badge = {
                    if (account.cartCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ) {
                            Text(account.cartCount.coerceAtMost(99).toString(), fontSize = 10.sp)
                        }
                    }
                },
            ) {
                IconButton(
                    onClick = onCartClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        Icons.Filled.ShoppingCart,
                        contentDescription = cartLabel,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** Section title with "See More" on right, exactly like Maniro UI Kit */
@Composable
private fun SectionHeaderRow(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(vertical = 4.dp),
            )
        }
    }
}

/**
 * Maniro "Popular Product" horizontal carousel:
 * Rounded squircle tiles with photo on light grey background + title below.
 */
@Composable
private fun PopularProductsRow(
    featured: List<Product>,
    onProductClick: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(featured, key = { it.id }) { product ->
            Column(
                modifier = Modifier
                    .width(96.dp)
                    .clickable { onProductClick(product.id) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(20.dp)),
                ) {
                    ProductImage(
                        url = product.imageUrl,
                        contentDescription = product.title,
                        shape = RoundedCornerShape(20.dp),
                        padding = PaddingValues(Spacing.sm),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * Maniro Minimalist Category Filter Pills:
 * - "All" pill (selected = solid black background, white text)
 * - Category pills (unselected = soft grey background, dark text)
 */
@Composable
private fun CategoryPillRow(
    categories: List<Category>,
    selectedSlug: String?,
    onSelect: (String?) -> Unit,
    onCategoryDoubleClick: (Category) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // "All" pill
        item(key = "all") {
            val isSelected = selectedSlug == null
            Surface(
                onClick = { onSelect(null) },
                shape = PillShape,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                contentColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Text(
                    text = stringResource(R.string.feature_home_all_categories),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = 8.dp),
                )
            }
        }

        // Each category
        items(categories, key = { it.slug }) { category ->
            val isSelected = selectedSlug == category.slug
            Surface(
                onClick = {
                    if (isSelected) {
                        onCategoryDoubleClick(category)
                    } else {
                        onSelect(category.slug)
                    }
                },
                shape = PillShape,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                contentColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = 8.dp),
                )
            }
        }
    }
}

/** "Added X to cart · View cart", or why it failed. Shown once per tap. */
@Composable
private fun CartMessageSnackbar(
    message: CartMessage?,
    host: SnackbarHostState,
    onViewCart: () -> Unit,
    onShown: () -> Unit,
) {
    val text = when (message) {
        is CartMessage.Added -> stringResource(R.string.feature_home_added, message.title)
        is CartMessage.Failed -> errorText(message.error).first
        null -> null
    }
    val action = stringResource(R.string.feature_home_view_cart)
    LaunchedEffect(message) {
        if (message == null || text == null) return@LaunchedEffect
        val result = host.showSnackbar(
            text,
            actionLabel = if (message is CartMessage.Added) action else null,
            duration = SnackbarDuration.Short,
        )
        onShown()
        if (result == SnackbarResult.ActionPerformed) onViewCart()
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.padding(top = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            repeat(4) {
                SkeletonBox(Modifier.size(96.dp), RoundedCornerShape(20.dp))
            }
        }
        Row(
            Modifier.padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            repeat(2) {
                SkeletonBox(Modifier.weight(1f).height(240.dp), RoundedCornerShape(18.dp))
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun HomeScreenPreview() {
    val category = Category("mens-clothing", "Men's clothing")
    val product = Product("3", "Mens Cotton Jacket", "", Money(55.99, "USD"), category, "", Rating(4.7, 500))
    CatalogueTheme {
        HomeScreen(
            state = HomeUiState.Success(CatalogueHome(listOf(product), listOf(category)), picks = listOf(product)),
            onSearchClick = {},
            onProductClick = {},
            onCategoryClick = {},
            onRetry = {},
            onRefresh = {},
        )
    }
}
