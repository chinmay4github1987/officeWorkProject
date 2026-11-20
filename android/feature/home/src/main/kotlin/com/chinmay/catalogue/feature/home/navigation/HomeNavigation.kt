package com.chinmay.catalogue.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.feature.home.HomeRoute
import kotlinx.serialization.Serializable

/** Type-safe route: a typo is a compile error, not a crash. */
@Serializable
data object HomeDestination

/**
 * The feature registers its own screen. It exposes callbacks instead of navigating itself,
 * so it doesn't need to know other features exist; :app decides where each callback goes.
 */
fun NavGraphBuilder.homeScreen(
    onSearchClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onCategoryClick: (Category) -> Unit,
) {
    composable<HomeDestination> {
        HomeRoute(onSearchClick = onSearchClick, onProductClick = onProductClick, onCategoryClick = onCategoryClick)
    }
}
