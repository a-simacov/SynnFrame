package com.synngate.synnframe.data.datasource

import com.synngate.synnframe.domain.entity.Product

// Интерфейс источника данных для продуктов
interface ProductDataSource {
    suspend fun getProducts(query: String? = null): List<Product>
    suspend fun getProductsByIds(ids: Set<String>): List<Product>
    suspend fun findProductByBarcode(barcode: String): Product?
}