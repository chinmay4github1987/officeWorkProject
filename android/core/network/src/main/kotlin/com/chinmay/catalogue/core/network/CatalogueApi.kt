package com.chinmay.catalogue.core.network

import com.chinmay.catalogue.core.network.dto.AddCartItemDto
import com.chinmay.catalogue.core.network.dto.CartDto
import com.chinmay.catalogue.core.network.dto.CartEnquiryRequestDto
import com.chinmay.catalogue.core.network.dto.CartQuantityDto
import com.chinmay.catalogue.core.network.dto.CategoryPageDto
import com.chinmay.catalogue.core.network.dto.EnquiryReceiptDto
import com.chinmay.catalogue.core.network.dto.EnquiryRequestDto
import com.chinmay.catalogue.core.network.dto.HomeDto
import com.chinmay.catalogue.core.network.dto.LoginRequestDto
import com.chinmay.catalogue.core.network.dto.ProductDto
import com.chinmay.catalogue.core.network.dto.ProductListDto
import com.chinmay.catalogue.core.network.dto.SessionDto
import com.chinmay.catalogue.core.network.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Our backend's v1 API. The app never talks to Fake Store: the backend owns this contract,
 * so the product data source can change without an app release.
 * Retrofit's suspend functions are main-safe: they run the call on OkHttp's own threads.
 */
interface CatalogueApi {
    @GET("api/v1/home")
    suspend fun home(): HomeDto

    @GET("api/v1/categories/{slug}")
    suspend fun category(@Path("slug") slug: String): CategoryPageDto

    @GET("api/v1/products")
    suspend fun search(@Query("search") term: String): ProductListDto

    @GET("api/v1/products/{id}")
    suspend fun product(@Path("id") id: String): ProductDto

    @POST("api/v1/enquiries")
    suspend fun submitEnquiry(@Body body: EnquiryRequestDto): EnquiryReceiptDto

    // ── Account and cart. These need the session token, which AuthInterceptor adds. ──

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): SessionDto

    @GET("api/v1/me")
    suspend fun me(): UserDto

    @GET("api/v1/cart")
    suspend fun cart(): CartDto

    @POST("api/v1/cart/items")
    suspend fun addToCart(@Body body: AddCartItemDto): CartDto

    @PUT("api/v1/cart/items/{productId}")
    suspend fun setCartQuantity(@Path("productId") productId: String, @Body body: CartQuantityDto): CartDto

    @DELETE("api/v1/cart/items/{productId}")
    suspend fun removeFromCart(@Path("productId") productId: String): CartDto

    @POST("api/v1/cart/enquiry")
    suspend fun submitCartEnquiry(@Body body: CartEnquiryRequestDto): EnquiryReceiptDto
}
