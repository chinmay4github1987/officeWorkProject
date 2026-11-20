package com.chinmay.catalogue.feature.enquiry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.catalogue.core.data.repository.AuthRepository
import com.chinmay.catalogue.core.data.repository.CatalogueRepository
import com.chinmay.catalogue.core.data.repository.EnquiryRepository
import com.chinmay.catalogue.core.model.DataError
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.EnquiryReceipt
import com.chinmay.catalogue.core.model.EnquiryRequest
import com.chinmay.catalogue.core.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EnquiryUiState(
    /** Shown at the top so the customer knows what they're asking about. Null while loading. */
    val product: Product? = null,
    val form: EnquiryForm = EnquiryForm(),
    val errors: Map<EnquiryField, FieldError> = emptyMap(),
    val isSubmitting: Boolean = false,
    /** A failure that isn't about one field (offline, server down). Shown as a banner; the form keeps its values. */
    val submitError: DataError? = null,
    /** Set once the backend confirms the email was sent: the screen switches to "Thank you". */
    val receipt: EnquiryReceipt? = null,
)

@HiltViewModel
class EnquiryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalogue: CatalogueRepository,
    private val enquiries: EnquiryRepository,
    auth: AuthRepository,
) : ViewModel() {

    private val productId: String = checkNotNull(savedStateHandle["productId"]) { "EnquiryDestination.productId is required" }

    // Signed in: name, email and phone come from the account, so the customer only checks them.
    private val _uiState = MutableStateFlow(
        EnquiryUiState(form = auth.currentUser.value?.let { EnquiryForm(name = it.fullName, email = it.email, phone = it.phone.orEmpty()) } ?: EnquiryForm()),
    )
    val uiState: StateFlow<EnquiryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Only for the summary card; the form works even if this fails, because the server checks the product.
            val result = catalogue.product(productId)
            if (result is DataResult.Success) _uiState.update { it.copy(product = result.data) }
        }
    }

    fun onFieldChange(field: EnquiryField, value: String) {
        // Editing a field clears its error, so the message doesn't linger once the user is fixing it.
        _uiState.update { it.copy(form = it.form.with(field, value), errors = it.errors - field, submitError = null) }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting || state.receipt != null) return // no double submits

        val errors = EnquiryFormValidator.validate(state.form)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, submitError = null) }
        viewModelScope.launch {
            val form = state.form
            val request = EnquiryRequest(
                productId = productId,
                name = form.name.trim(),
                email = form.email.trim(),
                phone = form.phone.trim().ifEmpty { null },
                message = form.message.trim().ifEmpty { null },
            )
            when (val result = enquiries.submit(request)) {
                is DataResult.Success -> _uiState.update { it.copy(isSubmitting = false, receipt = result.data) }
                is DataResult.Failure -> _uiState.update { current ->
                    val error = result.error
                    if (error is DataError.Validation) {
                        // Put the server's messages on the matching fields.
                        val fieldErrors = EnquiryField.entries.mapNotNull { field ->
                            error.fields[field.apiKey]?.let { field to FieldError.Server(it) }
                        }.toMap()
                        current.copy(isSubmitting = false, errors = fieldErrors, submitError = if (fieldErrors.isEmpty()) error else null)
                    } else {
                        current.copy(isSubmitting = false, submitError = error)
                    }
                }
            }
        }
    }
}
