package com.chinmay.catalogue.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chinmay.catalogue.feature.auth.navigation.navigateToLogin
import com.chinmay.catalogue.feature.cart.navigation.navigateToCart
import com.chinmay.catalogue.feature.category.navigation.navigateToCategory
import com.chinmay.catalogue.feature.home.navigation.HomeDestination
import com.chinmay.catalogue.feature.search.navigation.navigateToSearch
import kotlinx.coroutines.launch

/**
 * The whole app: the side drawer around the navigation graph. The drawer opens from Home's ☰ button, or by swiping
 * from the edge on Home (other screens keep the edge swipe for "back").
 */
@Composable
fun CatalogueApp(
    navController: NavHostController = rememberNavController(),
    viewModel: AppShellViewModel = hiltViewModel(),
) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val onHome = backStack?.destination?.route?.endsWith(HomeDestination::class.qualifiedName.orEmpty()) == true

    LaunchedEffect(drawer.isOpen) { if (drawer.isOpen) viewModel.onDrawerOpened() }

    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = onHome || drawer.isOpen,
        drawerContent = {
            AppDrawer(
                state = state,
                onNavigate = { destination ->
                    scope.launch { drawer.close() }
                    when (destination) {
                        DrawerDestination.Home -> navController.popBackStack(HomeDestination, inclusive = false)
                        DrawerDestination.Search -> navController.navigateToSearch()
                        DrawerDestination.Cart -> navController.navigateToCart()
                        DrawerDestination.SignIn -> navController.navigateToLogin()
                        is DrawerDestination.CategoryPage -> navController.navigateToCategory(destination.category.slug, destination.category.name)
                    }
                },
                onDarkThemeChange = viewModel::setDarkTheme,
                onSignOut = {
                    viewModel.signOut()
                    scope.launch { drawer.close() }
                },
            )
        },
    ) {
        CatalogueNavHost(navController = navController, onOpenDrawer = { scope.launch { drawer.open() } })
    }
}
