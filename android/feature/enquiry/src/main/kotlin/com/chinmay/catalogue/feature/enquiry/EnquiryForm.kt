package com.chinmay.catalogue.feature.enquiry

/** The fields on the form. Keys match the backend's field names, so server errors map straight back. */
enum class EnquiryField(val apiKey: String) {
    Name("name"),
    Email("email"),
    Phone("phone"),
    Message("message"),
}

data class EnquiryForm(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val message: String = "",
) {
    fun valueOf(field: EnquiryField) = when (field) {
        EnquiryField.Name -> name
        EnquiryField.Email -> email
        EnquiryField.Phone -> phone
        EnquiryField.Message -> message
    }

    fun with(field: EnquiryField, value: String) = when (field) {
        EnquiryField.Name -> copy(name = value)
        EnquiryField.Email -> copy(email = value)
        EnquiryField.Phone -> copy(phone = value)
        EnquiryField.Message -> copy(message = value)
    }
}

/** Why a field is invalid. The screen turns these into translated messages. */
sealed interface FieldError {
    data object Required : FieldError
    data object InvalidEmail : FieldError
    data object InvalidPhone : FieldError
    data class TooLong(val max: Int) : FieldError

    /** The server's own message, already written for users. */
    data class Server(val message: String) : FieldError
}

/**
 * Client-side checks for instant feedback. They mirror the backend's rules (backend/src/http/schemas.ts),
 * but the backend still validates: the client can never be trusted.
 */
object EnquiryFormValidator {
    const val NAME_MAX = 100
    const val EMAIL_MAX = 254
    const val PHONE_MAX = 30
    const val MESSAGE_MAX = 2000

    private val email = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private val phone = Regex("^\\+?[0-9 ()-]{6,20}$")

    fun validate(form: EnquiryForm): Map<EnquiryField, FieldError> = buildMap {
        val name = form.name.trim()
        when {
            name.isEmpty() -> put(EnquiryField.Name, FieldError.Required)
            name.length > NAME_MAX -> put(EnquiryField.Name, FieldError.TooLong(NAME_MAX))
        }

        val emailValue = form.email.trim()
        when {
            emailValue.isEmpty() -> put(EnquiryField.Email, FieldError.Required)
            emailValue.length > EMAIL_MAX -> put(EnquiryField.Email, FieldError.TooLong(EMAIL_MAX))
            !email.matches(emailValue) -> put(EnquiryField.Email, FieldError.InvalidEmail)
        }

        val phoneValue = form.phone.trim()
        when {
            phoneValue.isEmpty() -> Unit // optional
            phoneValue.length > PHONE_MAX -> put(EnquiryField.Phone, FieldError.TooLong(PHONE_MAX))
            !phone.matches(phoneValue) -> put(EnquiryField.Phone, FieldError.InvalidPhone)
        }

        if (form.message.trim().length > MESSAGE_MAX) put(EnquiryField.Message, FieldError.TooLong(MESSAGE_MAX))
    }
}
