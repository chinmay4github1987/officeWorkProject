package com.chinmay.catalogue.feature.search.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chinmay.catalogue.feature.search.SearchRoute
import kotlinx.serialization.Serializable

@Serializable
data object SearchDestination

fun NavController.navigateToSearch() = navigate(SearchDestination)

fun NavGraphBuilder.searchScreen(onBack: () -> Unit, onProductClick: (String) -> Unit) {
    composable<SearchDestination> {
        SearchRoute(onBack = onBack, onProductClick = onProductClick)
    }
}
