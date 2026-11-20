package com.chinmay.catalogue.feature.enquiry.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.chinmay.catalogue.feature.enquiry.EnquiryRoute
import kotlinx.serialization.Serializable

@Serializable
data class EnquiryDestination(val productId: String)

fun NavController.navigateToEnquiry(productId: String) = navigate(EnquiryDestination(productId))

fun NavGraphBuilder.enquiryScreen(onClose: () -> Unit, onContinueBrowsing: () -> Unit) {
    composable<EnquiryDestination> {
        EnquiryRoute(onClose = onClose, onContinueBrowsing = onContinueBrowsing)
    }
}
