package com.chinmay.catalogue.core.network

import com.chinmay.catalogue.core.network.dto.FormspreeResponseDto
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Formspree (https://formspree.io): receives the enquiry form, stores the submission in its dashboard and emails it
 * to the form's inbox (the sales team). The app only knows the public form ID: no SMTP server, no password.
 * Wrong or missing fields: HTTP 422 with {"errors":[{"field":"email","message":"..."}]}.
 */
interface FormspreeApi {
    @Headers("Accept: application/json")
    @POST("f/{formId}")
    suspend fun submit(@Path("formId") formId: String, @Body fields: Map<String, String>): FormspreeResponseDto
}

/** Which Formspree form receives enquiries. Blank in a build without FORMSPREE_FORM_ID (see android/.env.example). */
data class FormspreeConfig(val formId: String) {
    val isConfigured: Boolean get() = formId.isNotBlank()
}
