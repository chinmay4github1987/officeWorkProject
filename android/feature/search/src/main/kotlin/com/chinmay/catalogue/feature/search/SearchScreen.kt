package com.chinmay.catalogue.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.catalogue.core.designsystem.component.catalogueTextFieldColors
import com.chinmay.catalogue.core.designsystem.component.MessageState
import com.chinmay.catalogue.core.designsystem.theme.Spacing
import com.chinmay.catalogue.core.ui.DataErrorState
import com.chinmay.catalogue.core.ui.ProductTile
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
        onClear = viewModel::onClear,
        onBack = onBack,
        onProductClick = onProductClick,
    )
}

@Composable
internal fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val submit = {
        keyboard?.hide()
        onSearch()
    }
    // A suggestion is a shortcut for typing the word and tapping Search.
    val searchFor = { term: String ->
        onQueryChange(term)
        submit()
    }
    // Opening search means you want to type: focus the field and show the keyboard straight away.
    LaunchedEffect(Unit) { if (state.results == SearchResults.Idle) focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(top = Spacing.md).padding(horizontal = Spacing.sm),
                // Top-aligned with matching 56dp heights, so the button doesn't jump when the error text appears.
                verticalAlignment = Alignment.Top,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.feature_search_back))
                }
                OutlinedTextField(
                    colors = catalogueTextFieldColors(),
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    placeholder = { Text(stringResource(R.string.feature_search_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    isError = state.showRequiredError,
                    supportingText = if (state.showRequiredError) {
                        { Text(stringResource(R.string.feature_search_required)) }
                    } else null,
                    trailingIcon = if (state.query.isNotEmpty()) {
                        {
                            IconButton(onClick = onClear) {
                                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.feature_search_clear))
                            }
                        }
                    } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submit() }),
                )
                Button(
                    onClick = submit,
                    modifier = Modifier.padding(start = Spacing.sm, end = Spacing.sm).height(56.dp),
                    contentPadding = PaddingValues(horizontal = Spacing.lg),
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.padding(end = Spacing.xs))
                    Text(stringResource(R.string.feature_search_button))
                }
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
            when (val results = state.results) {
                SearchResults.Idle -> item(span = full) {
                    Column {
                        MessageState(Icons.Filled.Search, stringResource(R.string.feature_search_idle_title), stringResource(R.string.feature_search_idle_message))
                        Suggestions(onPick = searchFor)
                    }
                }
                SearchResults.Loading -> items(6) { ProductTileSkeleton() }
                is SearchResults.Error -> item(span = full) { DataErrorState(results.error, onRetry = onSearch) }
                is SearchResults.Found -> {
                    if (results.products.isEmpty()) {
                        item(span = full) {
                            // No dead ends: an empty result offers something to try next.
                            Column {
                                MessageState(
                                    Icons.Filled.Search,
                                    stringResource(R.string.feature_search_empty_title, results.term),
                                    stringResource(R.string.feature_search_empty_message),
                                )
                                Suggestions(onPick = searchFor)
                            }
                        }
                    } else {
                        item(span = full) {
                            // Announced by screen readers when results arrive.
                            Text(
                                pluralStringResource(R.plurals.feature_search_results, results.products.size, results.products.size, results.term),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }
                        items(results.products, key = { it.id }) { product ->
                            ProductTile(product, onClick = { onProductClick(product.id) })
                        }
                    }
                }
            }
        }
    }
}

/** Tappable starting points, so an empty screen always has a next step. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Suggestions(onPick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.feature_search_suggestions_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        ) {
            stringArrayResource(R.array.feature_search_suggestions).forEach { term ->
                SuggestionChip(onClick = { onPick(term) }, label = { Text(term) }, shape = MaterialTheme.shapes.extraLarge)
            }
        }
    }
}
