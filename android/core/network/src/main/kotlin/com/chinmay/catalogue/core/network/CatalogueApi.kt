package com.chinmay.catalogue.core.network

import com.chinmay.catalogue.core.network.dto.CategoryPageDto
import com.chinmay.catalogue.core.network.dto.EnquiryReceiptDto
import com.chinmay.catalogue.core.network.dto.EnquiryRequestDto
import com.chinmay.catalogue.core.network.dto.HomeDto
import com.chinmay.catalogue.core.network.dto.ProductDto
import com.chinmay.catalogue.core.network.dto.ProductListDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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
}
