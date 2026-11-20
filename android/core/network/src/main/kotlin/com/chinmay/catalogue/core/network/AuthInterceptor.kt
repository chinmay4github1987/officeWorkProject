package com.chinmay.catalogue.core.network

import okhttp3.Interceptor
import okhttp3.Response

/** Where the current session token comes from. Implemented by :core:data, which owns the session. */
fun interface SessionTokenProvider {
    /** The token, or null when signed out. Must be fast: it runs on every request. */
    fun token(): String?
}

/** Adds `Authorization: Bearer <token>` while signed in. Public endpoints simply ignore it. */
class AuthInterceptor(private val tokens: SessionTokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokens.token() ?: return chain.proceed(chain.request())
        return chain.proceed(chain.request().newBuilder().header("Authorization", "Bearer $token").build())
    }
}
