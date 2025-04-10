package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.Product

interface WizardProductRepository {
    suspend fun getProducts(query: String? = null): List<Product>
    suspend fun getProductsByIds(ids: Set<String>): List<Product>
    suspend fun findProductByBarcode(barcode: String): Product?
}