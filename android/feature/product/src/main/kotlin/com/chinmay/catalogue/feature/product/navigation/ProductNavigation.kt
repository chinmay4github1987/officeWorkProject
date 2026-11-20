package com.chinmay.catalogue.feature.product.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chinmay.catalogue.core.model.Category
import com.chinmay.catalogue.feature.product.ProductRoute
import kotlinx.serialization.Serializable

@Serializable
data class ProductDestination(val productId: String)

fun NavController.navigateToProduct(productId: String) = navigate(ProductDestination(productId))

fun NavGraphBuilder.productScreen(
    onBack: () -> Unit,
    onEnquire: (String) -> Unit,
    onCategoryClick: (Category) -> Unit,
) {
    composable<ProductDestination> {
        ProductRoute(onBack = onBack, onEnquire = onEnquire, onCategoryClick = onCategoryClick)
    }
}
