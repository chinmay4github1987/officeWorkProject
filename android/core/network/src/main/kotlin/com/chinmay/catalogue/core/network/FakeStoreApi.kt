package com.chinmay.catalogue.core.network

import com.chinmay.catalogue.core.network.dto.FakeStoreCartDto
import com.chinmay.catalogue.core.network.dto.FakeStoreLoginRequestDto
import com.chinmay.catalogue.core.network.dto.FakeStoreProductDto
import com.chinmay.catalogue.core.network.dto.FakeStoreTokenDto
import com.chinmay.catalogue.core.network.dto.FakeStoreUserDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * https://fakestoreapi.com, called straight from the app (there is no backend in between).
 * Retrofit's suspend functions are main-safe: they run the call on OkHttp's own threads.
 * Fake Store never saves writes (new carts, cart changes), so the app only reads from it, apart from login.
 */
interface FakeStoreApi {
    @GET("products")
    suspend fun products(): List<FakeStoreProductDto>

    /** Raw body: an unknown ID is HTTP 200 with an EMPTY body, which the JSON converter can't parse. */
    @GET("products/{id}")
    suspend fun product(@Path("id") id: String): ResponseBody

    /** Raw names, e.g. "men's clothing". */
    @GET("products/categories")
    suspend fun categories(): List<String>

    /** Wrong credentials: HTTP 401 with a plain-text body. */
    @POST("auth/login")
    suspend fun login(@Body body: FakeStoreLoginRequestDto): FakeStoreTokenDto

    @GET("users/{id}")
    suspend fun user(@Path("id") id: String): FakeStoreUserDto

    @GET("carts/user/{userId}")
    suspend fun cartsOfUser(@Path("userId") userId: String): List<FakeStoreCartDto>
}
