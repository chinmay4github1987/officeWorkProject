package com.chinmay.catalogue.core.model

/**
 * The result of any data operation. Repositories never throw to the UI: they return Success or a typed Failure,
 * so every screen is forced (by the compiler) to handle errors.
 */
sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Failure(val error: DataError) : DataResult<Nothing>
}

/** What went wrong, in terms a screen can act on. No HTTP or exception types leak past :core:data. */
sealed interface DataError {
    /** No connection, timeout, server unreachable. Retrying may help. */
    data object Network : DataError

    /** The product or category doesn't exist. */
    data object NotFound : DataError

    /** The server rejected the input. Keys are field names ("email"), values are messages to show. */
    data class Validation(val fields: Map<String, String>) : DataError

    /** Too many requests (enquiries and logins are rate limited). */
    data object RateLimited : DataError

    /** Login failed: the username and password don't match. */
    data object InvalidCredentials : DataError

    /** The session is missing or expired. The app has already signed out; the screen should offer sign-in. */
    data object Unauthorized : DataError

    /** Anything else. [message] is safe to show when present. */
    data class Server(val message: String?) : DataError
}

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(data))
    is DataResult.Failure -> this
}
