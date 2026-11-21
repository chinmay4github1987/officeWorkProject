package com.chinmay.catalogue.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.catalogue.core.designsystem.component.ReadableWidth
import com.chinmay.catalogue.core.designsystem.component.SearchPill
import com.chinmay.catalogue.core.designsystem.component.WindowWidth
import com.chinmay.catalogue.core.designsystem.component.SectionHeader
import com.chinmay.catalogue.core.designsystem.component.SkeletonBox
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.CatalogueHome
import com.chinmay.catalogue.core.model.Money
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.model.Rating
import com.chinmay.catalogue.core.ui.CategoryCard
import com.chinmay.catalogue.core.ui.DataErrorState
import com.chinmay.catalogue.core.ui.FeaturedProductCard
import com.chinmay.catalogue.core.ui.FeaturedProductCardSkeleton

/** Stateful entry point: gets the ViewModel from Hilt and hands plain state + callbacks to [HomeScreen]. */
@Composable
internal fun HomeRoute(
    onSearchClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onCartClick: () -> Unit,
    onSignInClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        account = account,
        onSearchClick = onSearchClick,
        onProductClick = onProductClick,
        onCategoryClick = onCategoryClick,
        onCartClick = onCartClick,
        onSignInClick = onSignInClick,
        onSignOutClick = viewModel::signOut,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
    )
}

/** Stateless: renders whatever state it is given, so it can be previewed and tested without a ViewModel. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onSearchClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    account: HomeAccount = HomeAccount(),
    onCartClick: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
) {
    val columns = WindowWidth.gridColumns()
    Scaffold(contentWindowInsets = WindowInsets(0)) { padding ->
        PullToRefreshBox(
            isRefreshing = (state as? HomeUiState.Success)?.isRefreshing == true,
            onRefresh = onRefresh,
            // Content stops at the status bar, so the pinned search bar never slides under the clock.
            modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                item { Header(account, onCartClick, onSignInClick, onSignOutClick) }
                // Search stays pinned while you scroll: the fastest route to a product is always one tap away.
                stickyHeader(key = "search") {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        SearchPill(
                            stringResource(R.string.feature_home_search_placeholder),
                            onClick = onSearchClick,
                            // Search fields don't need to span a whole tablet or TV screen.
                            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm).widthIn(max = ReadableWidth),
                        )
                    }
                }
                when (state) {
                    HomeUiState.Loading -> item { LoadingContent() }
                    is HomeUiState.Error -> item { DataErrorState(state.error, onRetry) }
                    is HomeUiState.Success -> {
                        item {
                            SectionHeader(
                                stringResource(R.string.feature_home_top_rated),
                                subtitle = stringResource(R.string.feature_home_top_rated_subtitle),
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = Spacing.lg),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                            ) {
                                itemsIndexed(state.home.featured, key = { _, p -> p.id }) { index, product ->
                                    FeaturedProductCard(product, rank = index + 1, onClick = { onProductClick(product.id) })
                                }
                            }
                        }
                        item { SectionHeader(stringResource(R.string.feature_home_categories)) }
                        // A simple grid inside the list (a nested LazyVerticalGrid can't scroll inside a LazyColumn).
                        // 2 columns on phones, 3 on unfolded foldables and portrait tablets, 4 on wide windows and TV.
                        state.home.categories.chunked(columns).forEachIndexed { row, cells ->
                            item(key = "categories-$columns-$row") {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                ) {
                                    cells.forEachIndexed { col, category ->
                                        CategoryCard(category, index = row * columns + col, onClick = { onCategoryClick(category) }, modifier = Modifier.weight(1f))
                                    }
                                    repeat(columns - cells.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(account: HomeAccount, onCartClick: () -> Unit, onSignInClick: () -> Unit, onSignOutClick: () -> Unit) {
    Column(Modifier.padding(horizontal = Spacing.lg).padding(top = Spacing.lg)) {
        // Top row: small gold "eyebrow" (who you are / the collection) with round actions on the right.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                (account.user?.let { stringResource(R.string.feature_home_greeting, it.firstName) } ?: stringResource(R.string.feature_home_eyebrow)).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            AccountButton(account, onSignInClick, onSignOutClick)
            Spacer(Modifier.width(Spacing.sm))
            CartButton(account.cartCount, onCartClick)
        }
        Spacer(Modifier.height(Spacing.md))
        // Editorial headline: large serif, the one place the page "speaks".
        Text(
            stringResource(R.string.feature_home_title),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.feature_home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Round, softly raised header actions (44dp: comfortably above the 40dp minimum and easy to hit with D-pad). */
@Composable
private fun headerButtonColors() = IconButtonDefaults.filledTonalIconButtonColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
)

/** Cart with a count badge. The badge is part of the button's label for screen readers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartButton(count: Int, onClick: () -> Unit) {
    val label = if (count > 0) {
        pluralStringResource(R.plurals.feature_home_cart_with_items, count, count)
    } else {
        stringResource(R.string.feature_home_cart)
    }
    FilledTonalIconButton(
        onClick = onClick,
        colors = headerButtonColors(),
        modifier = Modifier.size(44.dp).semantics { contentDescription = label },
    ) {
        BadgedBox(badge = { if (count > 0) Badge { Text(if (count > 99) "99+" else count.toString()) } }) {
            Icon(Icons.Filled.ShoppingCart, contentDescription = null)
        }
    }
}

/** Signed out: goes straight to sign-in. Signed in: a small menu with who you are and Sign out. */
@Composable
private fun AccountButton(account: HomeAccount, onSignInClick: () -> Unit, onSignOutClick: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val user = account.user
    Box {
        FilledTonalIconButton(
            onClick = { if (user == null) onSignInClick() else menuOpen = true },
            colors = headerButtonColors(),
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                Icons.Filled.AccountCircle,
                contentDescription = stringResource(if (user == null) R.string.feature_home_sign_in else R.string.feature_home_account),
            )
        }
        DropdownMenu(expanded = menuOpen && user != null, onDismissRequest = { menuOpen = false }) {
            if (user != null) {
                Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
                    Text(user.fullName, style = MaterialTheme.typography.titleSmall)
                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.feature_home_sign_out)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onSignOutClick()
                    },
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        SkeletonBox(Modifier.padding(horizontal = Spacing.lg).fillMaxWidth(0.4f).height(24.dp))
        Row(Modifier.padding(start = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            repeat(2) { FeaturedProductCardSkeleton() }
        }
        repeat(2) {
            Row(Modifier.padding(horizontal = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SkeletonBox(Modifier.weight(1f).height(104.dp), MaterialTheme.shapes.large)
                SkeletonBox(Modifier.weight(1f).height(104.dp), MaterialTheme.shapes.large)
            }
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    val category = Category("electronics", "Electronics")
    val product = Product("1", "Wireless Headphones", "", Money(99.0, "USD"), category, "", Rating(4.6, 210))
    CatalogueTheme {
        HomeScreen(
            state = HomeUiState.Success(CatalogueHome(listOf(product, product.copy(id = "2")), listOf(category, Category("jewelery", "Jewelery")))),
            onSearchClick = {}, onProductClick = {}, onCategoryClick = {}, onRetry = {}, onRefresh = {},
        )
    }
}
