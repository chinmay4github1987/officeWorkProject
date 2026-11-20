package com.chinmay.catalogue.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.chinmay.catalogue.feature.auth.navigation.loginScreen
import com.chinmay.catalogue.feature.auth.navigation.navigateToLogin
import com.chinmay.catalogue.feature.cart.navigation.cartScreen
import com.chinmay.catalogue.feature.cart.navigation.navigateToCart
import com.chinmay.catalogue.feature.category.navigation.categoryScreen
import com.chinmay.catalogue.feature.category.navigation.navigateToCategory
import com.chinmay.catalogue.feature.enquiry.navigation.enquiryScreen
import com.chinmay.catalogue.feature.enquiry.navigation.navigateToEnquiry
import com.chinmay.catalogue.feature.home.navigation.HomeDestination
import com.chinmay.catalogue.feature.home.navigation.homeScreen
import com.chinmay.catalogue.feature.product.navigation.navigateToProduct
import com.chinmay.catalogue.feature.product.navigation.productScreen
import com.chinmay.catalogue.feature.search.navigation.navigateToSearch
import com.chinmay.catalogue.feature.search.navigation.searchScreen

/**
 * The whole app's navigation graph. Features expose callbacks ("a product was tapped");
 * this is the only place that decides where each callback leads. Features stay independent.
 */
@Composable
fun CatalogueNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = HomeDestination,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) + fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(200)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) + fadeOut(tween(300)) },
    ) {
        homeScreen(
            onSearchClick = navController::navigateToSearch,
            onProductClick = navController::navigateToProduct,
            onCategoryClick = { navController.navigateToCategory(it.slug, it.name) },
            onCartClick = navController::navigateToCart,
            onSignInClick = navController::navigateToLogin,
        )
        categoryScreen(
            onBack = navController::popBackStack,
            onProductClick = navController::navigateToProduct,
        )
        searchScreen(
            onBack = navController::popBackStack,
            onProductClick = navController::navigateToProduct,
        )
        productScreen(
            onBack = navController::popBackStack,
            onEnquire = navController::navigateToEnquiry,
            onCategoryClick = { navController.navigateToCategory(it.slug, it.name) },
            onViewCart = navController::navigateToCart,
            // Sign in, then come straight back to this product to add it.
            onSignInRequired = navController::navigateToLogin,
        )
        loginScreen(
            onClose = navController::popBackStack,
            // Back to wherever sign-in was needed (product, cart or home); those screens react to the new session.
            onSignedIn = { navController.popBackStack() },
        )
        cartScreen(
            onBack = navController::popBackStack,
            onSignIn = navController::navigateToLogin,
            onProductClick = navController::navigateToProduct,
            onContinueBrowsing = { navController.popBackStack(HomeDestination, inclusive = false) },
        )
        enquiryScreen(
            onClose = navController::popBackStack,
            // "Continue browsing" goes back to Home and clears the product/enquiry screens from the back stack.
            onContinueBrowsing = { navController.popBackStack(HomeDestination, inclusive = false) },
        )
    }
}
