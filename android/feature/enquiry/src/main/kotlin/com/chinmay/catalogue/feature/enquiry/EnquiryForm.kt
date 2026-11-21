package com.chinmay.catalogue.feature.enquiry

/** The fields on the form. Keys match the data layer's field names, so its Validation errors map straight back. */
enum class EnquiryField(val apiKey: String) {
    Name("name"),
    Email("email"),
    Phone("phone"),
    Company("company"),
    Message("message"),
}

data class EnquiryForm(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val company: String = "",
    val message: String = "",
) {
    fun valueOf(field: EnquiryField) = when (field) {
        EnquiryField.Name -> name
        EnquiryField.Email -> email
        EnquiryField.Phone -> phone
        EnquiryField.Company -> company
        EnquiryField.Message -> message
    }

    fun with(field: EnquiryField, value: String) = when (field) {
        EnquiryField.Name -> copy(name = value)
        EnquiryField.Email -> copy(email = value)
        EnquiryField.Phone -> copy(phone = value)
        EnquiryField.Company -> copy(company = value)
        EnquiryField.Message -> copy(message = value)
    }
}

/** Why a field is invalid. The screen turns these into translated messages. */
sealed interface FieldError {
    data object Required : FieldError
    data object InvalidEmail : FieldError
    data object InvalidPhone : FieldError
    data class TooShort(val min: Int) : FieldError
    data class TooLong(val max: Int) : FieldError

    /** The server's own message, already written for users. */
    data class Server(val message: String) : FieldError
}

/**
 * Checks before sending, for instant feedback (Formspree checks the email too).
 * Name, email, phone and message are required; company is optional.
 */
object EnquiryFormValidator {
    const val NAME_MIN = 2
    const val NAME_MAX = 100
    const val EMAIL_MAX = 254
    const val PHONE_MAX = 30
    const val COMPANY_MAX = 100
    const val MESSAGE_MIN = 10
    const val MESSAGE_MAX = 2000

    private val email = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    /** Digits with optional +, spaces, dashes and brackets; 7 to 15 digits (the international maximum). */
    private val phone = Regex("^\\+?[0-9 ()-]{6,24}$")

    fun validate(form: EnquiryForm): Map<EnquiryField, FieldError> = buildMap {
        val name = form.name.trim()
        when {
            name.isEmpty() -> put(EnquiryField.Name, FieldError.Required)
            name.length < NAME_MIN -> put(EnquiryField.Name, FieldError.TooShort(NAME_MIN))
            name.length > NAME_MAX -> put(EnquiryField.Name, FieldError.TooLong(NAME_MAX))
        }

        val emailValue = form.email.trim()
        when {
            emailValue.isEmpty() -> put(EnquiryField.Email, FieldError.Required)
            emailValue.length > EMAIL_MAX -> put(EnquiryField.Email, FieldError.TooLong(EMAIL_MAX))
            !email.matches(emailValue) -> put(EnquiryField.Email, FieldError.InvalidEmail)
        }

        val phoneValue = form.phone.trim()
        val digits = phoneValue.count { it.isDigit() }
        when {
            phoneValue.isEmpty() -> put(EnquiryField.Phone, FieldError.Required)
            phoneValue.length > PHONE_MAX -> put(EnquiryField.Phone, FieldError.TooLong(PHONE_MAX))
            !phone.matches(phoneValue) || digits !in 7..15 -> put(EnquiryField.Phone, FieldError.InvalidPhone)
        }

        if (form.company.trim().length > COMPANY_MAX) put(EnquiryField.Company, FieldError.TooLong(COMPANY_MAX))

        val message = form.message.trim()
        when {
            message.isEmpty() -> put(EnquiryField.Message, FieldError.Required)
            message.length < MESSAGE_MIN -> put(EnquiryField.Message, FieldError.TooShort(MESSAGE_MIN))
            message.length > MESSAGE_MAX -> put(EnquiryField.Message, FieldError.TooLong(MESSAGE_MAX))
        }
    }
}
