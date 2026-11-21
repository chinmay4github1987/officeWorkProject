package com.chinmay.catalogue.core.network.dto

import com.chinmay.catalogue.core.model.User
import kotlinx.serialization.Serializable

/**
 * How the signed-in user is saved on the phone (by :core:data's session store), so sign-in survives restarts.
 * Only what the app shows: never a password or an address.
 */
@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null,
)

fun UserDto.asExternalModel() = User(
    id = id,
    username = username,
    email = email,
    firstName = firstName,
    lastName = lastName,
    phone = phone,
)

fun User.asNetworkModel() = UserDto(id, username, email, firstName, lastName, phone)
