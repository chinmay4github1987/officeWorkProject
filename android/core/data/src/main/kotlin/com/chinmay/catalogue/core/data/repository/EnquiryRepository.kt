package com.chinmay.catalogue.core.data.repository

import com.chinmay.catalogue.core.model.DataResult
import com.chinmay.catalogue.core.model.EnquiryReceipt
import com.chinmay.catalogue.core.model.EnquiryRequest

interface EnquiryRepository {
    suspend fun submit(request: EnquiryRequest): DataResult<EnquiryReceipt>
}
