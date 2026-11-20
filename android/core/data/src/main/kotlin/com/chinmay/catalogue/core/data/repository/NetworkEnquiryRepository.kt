package com.chinmay.catalogue.core.data.repository

import com.chinmay.catalogue.core.data.safeApiCall
import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.EnquiryReceipt
import com.chinmay.catalogue.core.model.EnquiryRequest
import com.chinmay.catalogue.core.network.CatalogueApi
import com.chinmay.catalogue.core.network.dto.asExternalModel
import com.chinmay.catalogue.core.network.dto.asNetworkModel
import kotlinx.serialization.json.Json
import javax.inject.Inject

internal class NetworkEnquiryRepository @Inject constructor(
    private val api: CatalogueApi,
    private val json: Json,
) : EnquiryRepository {

    override suspend fun submit(request: EnquiryRequest): DataResult<EnquiryReceipt> =
        safeApiCall(json) { api.submitEnquiry(request.asNetworkModel()).asExternalModel() }
}
