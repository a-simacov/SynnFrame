package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.domain.entity.Product

interface ProductApi {

    suspend fun getProducts(): ApiResult<List<Product>>
}