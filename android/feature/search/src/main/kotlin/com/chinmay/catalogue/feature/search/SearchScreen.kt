package com.chinmay.catalogue.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.catalogue.core.designsystem.component.CategoryChip
import com.chinmay.catalogue.core.designsystem.component.CircleButtonStyle
import com.chinmay.catalogue.core.designsystem.component.CircleIconButton
import com.chinmay.catalogue.core.designsystem.component.MessageState
import com.chinmay.catalogue.core.designsystem.component.SearchBarHeight
import com.chinmay.catalogue.core.designsystem.component.SearchField
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.core.designsystem.theme.PillShape
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.core.model.Money
import com.chinmay.catalogue.core.model.Product
import com.chinmay.catalogue.core.model.Rating
import com.chinmay.catalogue.core.ui.DataErrorState
import com.chinmay.catalogue.core.ui.ProductCard
import com.chinmay.catalogue.core.ui.ProductTileSkeleton

@Composable
internal fun SearchRoute(
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onSearch = viewModel::onSearch,
        onSearchFor = viewModel::onSearchFor,
        onClear = viewModel::onClear,
        onClearRecent = viewModel::onClearRecent,
        onCategorySelected = viewModel::onCategorySelected,
        onLoadMore = viewModel::onLoadMore,
        onBack = onBack,
        onProductClick = onProductClick,
    )
}

/**
 * Search: a prominent search bar; suggestions while typing; recent and popular searches before searching;
 * then "Results for …" with a count, category chips, a two-column product grid and "Load more".
 */
@Composable
internal fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onSearchFor: (String) -> Unit,
    onClear: () -> Unit,
    onClearRecent: () -> Unit,
    onCategorySelected: (String?) -> Unit,
    onLoadMore: () -> Unit,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val submit = {
        keyboard?.hide()
        onSearch()
    }
    val searchFor = { term: String ->
        keyboard?.hide()
        onSearchFor(term)
    }
    // Opening search means you want to type: focus the field and show the keyboard straight away.
    LaunchedEffect(Unit) {
        // Tolerant: on a cold start the field may attach a frame later; the keyboard is a convenience, not essential.
        if (state.results == SearchResults.Idle) runCatching { focusRequester.requestFocus() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                Modifier.fillMaxWidth().statusBarsPadding().padding(top = Spacing.md, bottom = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                SearchBarRow(state, onQueryChange, submit, onClear, onBack, focusRequester)
                val found = state.results as? SearchResults.Found
                if (state.suggestions.isEmpty() && found != null && found.products.isNotEmpty()) {
                    ResultChips(state, onCategorySelected)
                }
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, top = Spacing.sm, bottom = Spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
            if (state.suggestions.isNotEmpty()) {
                item(span = full, key = "suggestions") { Suggestions(state.suggestions, searchFor) }
                return@LazyVerticalGrid
            }
            when (val results = state.results) {
                SearchResults.Idle -> item(span = full, key = "idle") {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        if (state.recent.isNotEmpty()) {
                            RecentSearches(state.recent, searchFor, onClearRecent)
                        } else {
                            MessageState(Icons.Filled.Search, stringResource(R.string.feature_search_idle_title), stringResource(R.string.feature_search_idle_message))
                        }
                        PopularSearches(searchFor)
                    }
                }
                SearchResults.Loading -> items(6) { ProductTileSkeleton() }
                is SearchResults.Error -> item(span = full) { DataErrorState(results.error, onRetry = onSearch) }
                is SearchResults.Found -> {
                    if (results.products.isEmpty()) {
                        // No dead ends: an empty result offers something to try next.
                        item(span = full, key = "empty") {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                                MessageState(
                                    Icons.Filled.Search,
                                    stringResource(R.string.feature_search_empty_title, results.term),
                                    stringResource(R.string.feature_search_empty_message),
                                )
                                PopularSearches(searchFor)
                            }
                        }
                    } else {
                        item(span = full, key = "header") { ResultsHeader(results.term, state.filteredProducts.size) }
                        items(state.visibleProducts, key = { it.id }) { product ->
                            ProductCard(product, onClick = { onProductClick(product.id) }, detailed = true)
                        }
                        if (state.remaining > 0) {
                            item(span = full, key = "load-more") {
                                OutlinedButton(onClick = onLoadMore, shape = PillShape, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                                    Text(pluralStringResource(R.plurals.feature_search_load_more, state.remaining, state.remaining))
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
private fun SearchBarRow(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    submit: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester,
) {
    val requiredMessage = stringResource(R.string.feature_search_required)
    Column(Modifier.padding(horizontal = Spacing.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 2.dp)) {
            CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_search_back), onBack, size = SearchBarHeight)
            SearchField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.feature_search_label),
                onSearch = submit,
                onClear = onClear,
                clearDescription = stringResource(R.string.feature_search_clear),
                errorMessage = if (state.showRequiredError) requiredMessage else null,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
            )
            CircleIconButton(
                Icons.Filled.Search,
                stringResource(R.string.feature_search_button),
                onClick = submit,
                style = CircleButtonStyle.Primary,
                size = SearchBarHeight,
            )
        }
        if (state.showRequiredError) {
            Text(
                requiredMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = SearchBarHeight + Spacing.lg, top = Spacing.xs),
            )
        }
    }
}

/** "All" plus one chip per category in the results; filters on the phone, no new request. */
@Composable
private fun ResultChips(state: SearchUiState, onCategorySelected: (String?) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        item(key = "all") {
            CategoryChip(stringResource(R.string.feature_search_all), selected = state.selectedCategory == null, onClick = { onCategorySelected(null) }, icon = Icons.Filled.Check)
        }
        items(state.categories, key = { it.slug }) { category ->
            CategoryChip(category.name, selected = state.selectedCategory == category.slug, onClick = { onCategorySelected(category.slug) }, icon = Icons.Filled.Check)
        }
    }
}

/** Matching product names and categories while typing; tapping one searches for it. */
@Composable
private fun Suggestions(suggestions: List<String>, onPick: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                stringResource(R.string.feature_search_suggestions_heading),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.lg, top = Spacing.md, bottom = Spacing.xs).semantics { heading() },
            )
            suggestions.forEachIndexed { index, suggestion ->
                if (index > 0) HorizontalDivider(Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(role = Role.Button) { onPick(suggestion) }
                        .padding(horizontal = Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Text(suggestion, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** This phone's last searches, one tap to repeat; "Clear" forgets them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentSearches(recent: List<String>, onPick: (String) -> Unit, onClear: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.feature_search_recent_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            TextButton(onClick = onClear) { Text(stringResource(R.string.feature_search_recent_clear)) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            recent.forEach { term ->
                CategoryChip(label = term, selected = false, onClick = { onPick(term) }, icon = Icons.Filled.Refresh)
            }
        }
    }
}

/** Starting points, so an empty screen always has a next step. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PopularSearches(onPick: (String) -> Unit) {
    val popular = stringArrayResource(R.array.feature_search_suggestions).toList()
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            stringResource(R.string.feature_search_suggestions_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            popular.forEach { term -> CategoryChip(label = term, selected = false, onClick = { onPick(term) }) }
        }
    }
}

/** "Results for “ssd”" with the count; announced by screen readers when results arrive or a chip changes them. */
@Composable
private fun ResultsHeader(term: String, count: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.feature_search_results_title, term),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Text(
            pluralStringResource(R.plurals.feature_search_results_count, count, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.sm).semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SearchScreenPreview() {
    val jacket = Category("mens-clothing", "Men's clothing")
    val products = listOf(
        Product("3", "Mens Cotton Jacket", "", Money(55.99, "USD"), jacket, "", Rating(4.7, 500)),
        Product("4", "Mens Casual Slim Fit", "", Money(15.99, "USD"), jacket, "", Rating(2.1, 430)),
    )
    CatalogueTheme {
        SearchScreen(
            SearchUiState(query = "jacket", results = SearchResults.Found("jacket", products)),
            {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}
