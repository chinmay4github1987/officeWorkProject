package com.chinmay.catalogue.feature.cart.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chinmay.catalogue.feature.cart.CartRoute
import kotlinx.serialization.Serializable

@Serializable
data object CartDestination

fun NavController.navigateToCart() = navigate(CartDestination) { launchSingleTop = true }

fun NavGraphBuilder.cartScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onProductClick: (String) -> Unit,
    onContinueBrowsing: () -> Unit,
) {
    composable<CartDestination> {
        CartRoute(onBack = onBack, onSignIn = onSignIn, onProductClick = onProductClick, onContinueBrowsing = onContinueBrowsing)
    }
}
