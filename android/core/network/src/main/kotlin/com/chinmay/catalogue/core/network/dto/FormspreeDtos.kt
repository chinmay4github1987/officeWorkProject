package com.chinmay.catalogue.core.network.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A successful submission: {"ok": true, "next": "..."}. */
@Serializable
data class FormspreeResponseDto(val ok: Boolean = false)

/** A rejected submission (HTTP 422): {"error": "...", "errors": [{"field": "email", "message": "..."}]}. */
@Serializable
data class FormspreeErrorDto(val error: String? = null, val errors: List<FormspreeFieldErrorDto> = emptyList())

@Serializable
data class FormspreeFieldErrorDto(val field: String? = null, val message: String = "")

/** Field name -> message for the errors that name a field; null if the body isn't Formspree's error JSON. */
fun parseFormspreeErrors(body: String?, json: Json): FormspreeErrorDto? =
    body?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString(FormspreeErrorDto.serializer(), it) }.getOrNull() }
