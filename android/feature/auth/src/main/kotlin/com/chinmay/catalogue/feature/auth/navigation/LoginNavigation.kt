package com.chinmay.catalogue.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chinmay.catalogue.feature.auth.LoginRoute
import kotlinx.serialization.Serializable

@Serializable
data object LoginDestination

fun NavController.navigateToLogin() = navigate(LoginDestination) { launchSingleTop = true }

/** [onSignedIn] decides where to go next (usually back to where sign-in was needed). */
fun NavGraphBuilder.loginScreen(onClose: () -> Unit, onSignedIn: () -> Unit) {
    composable<LoginDestination> {
        LoginRoute(onClose = onClose, onSignedIn = onSignedIn)
    }
}
