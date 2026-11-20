package com.chinmay.catalogue.feature.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.catalogue.core.designsystem.component.SearchPill
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
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onSearchClick = onSearchClick,
        onProductClick = onProductClick,
        onCategoryClick = onCategoryClick,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
    )
}

/** Stateless: renders whatever state it is given, so it can be previewed and tested without a ViewModel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onSearchClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(contentWindowInsets = WindowInsets(0)) { padding ->
        PullToRefreshBox(
            isRefreshing = (state as? HomeUiState.Success)?.isRefreshing == true,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                item { Header(onSearchClick) }
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
                        // A simple 2-column grid inside the list (a nested LazyVerticalGrid can't scroll inside a LazyColumn).
                        state.home.categories.chunked(2).forEachIndexed { row, pair ->
                            item(key = "categories-$row") {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                ) {
                                    pair.forEachIndexed { col, category ->
                                        CategoryCard(category, index = row * 2 + col, onClick = { onCategoryClick(category) }, modifier = Modifier.weight(1f))
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
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
private fun Header(onSearchClick: () -> Unit) {
    Column(
        Modifier.statusBarsPadding().padding(horizontal = Spacing.lg).padding(top = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            stringResource(R.string.feature_home_title),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.feature_home_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
        SearchPill(stringResource(R.string.feature_home_search_placeholder), onClick = onSearchClick)
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
                SkeletonBox(Modifier.weight(1f).height(96.dp), MaterialTheme.shapes.large)
                SkeletonBox(Modifier.weight(1f).height(96.dp), MaterialTheme.shapes.large)
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
