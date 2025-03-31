package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {

    fun getProducts(): Flow<List<Product>>

    fun getProductsByNameFilter(nameFilter: String): Flow<List<Product>>

    suspend fun getProductById(id: String): Product?

    suspend fun getProductsByIds(ids: Set<String>): List<Product>

    suspend fun findProductByBarcode(barcode: String): Product?

    fun getProductsCount(): Flow<Int>

    suspend fun addProduct(product: Product)

    suspend fun addProducts(products: List<Product>)

    suspend fun updateProduct(product: Product)

    suspend fun deleteProduct(id: String)

    suspend fun deleteAllProducts()

    suspend fun getProductsFromRemote(): ApiResult<List<Product>>
}