package com.chinmay.catalogue.feature.enquiry

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.chinmay.catalogue.core.designsystem.theme.CatalogueTheme
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.EnquiryReceipt
import com.chinmay.catalogue.core.testing.TestData
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Enquiry screen rendered on the JVM (Robolectric): errors, failure and the "Thank You!" confirmation. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h3200dp")
class EnquiryScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var backToProduct = false

    private fun show(state: EnquiryUiState) = compose.setContent {
        CatalogueTheme {
            EnquiryScreen(state = state, onFieldChange = { _, _ -> }, onSubmit = {}, onClose = { backToProduct = true }, onContinueBrowsing = {})
        }
    }

    @Test
    fun `field errors are shown next to the fields`() {
        show(
            EnquiryUiState(
                product = TestData.products[1],
                errors = mapOf(EnquiryField.Phone to FieldError.Required, EnquiryField.Email to FieldError.InvalidEmail),
            ),
        )

        compose.onNodeWithText("Enter your phone number").assertIsDisplayed()
        compose.onNodeWithText("Enter a valid email address, like name@example.com").assertIsDisplayed()
    }

    @Test
    fun `a failed submission keeps the typed details and explains what happened`() {
        show(EnquiryUiState(form = EnquiryForm(name = "Asha Rao"), submitError = DataError.Network))

        compose.onNodeWithText("Asha Rao").assertIsDisplayed()
        compose.onNodeWithText("Check your connection, then try again.").assertIsDisplayed()
    }

    @Test
    fun `a confirmed enquiry shows Thank You with the product and goes back to it`() {
        show(EnquiryUiState(product = TestData.products[1], receipt = EnquiryReceipt("ENQ-TEST0001", "2026-11-21T10:00:00Z")))

        compose.onNodeWithText("Thank You!").assertIsDisplayed()
        compose.onNodeWithText("Your enquiry has been submitted successfully. Our sales team will contact you shortly.").assertIsDisplayed()
        compose.onNodeWithText("Cotton Jacket").assertIsDisplayed()
        compose.onNodeWithText("Back to product").performClick()
        assertTrue(backToProduct)
    }
}
