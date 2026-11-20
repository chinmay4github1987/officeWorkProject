package com.chinmay.catalogue.feature.category.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chinmay.catalogue.feature.category.CategoryRoute
import kotlinx.serialization.Serializable

/** The name travels with the slug so the top bar shows a title immediately, before the page loads. */
@Serializable
data class CategoryDestination(val slug: String, val name: String)

fun NavController.navigateToCategory(slug: String, name: String) = navigate(CategoryDestination(slug, name))

fun NavGraphBuilder.categoryScreen(onBack: () -> Unit, onProductClick: (String) -> Unit) {
    composable<CategoryDestination> {
        CategoryRoute(onBack = onBack, onProductClick = onProductClick)
    }
}
