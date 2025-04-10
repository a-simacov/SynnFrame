package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.datasource.ProductDataSource
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.repository.WizardProductRepository

class WizardProductRepositoryImpl(
    private val productDataSource: ProductDataSource
) : WizardProductRepository {

    override suspend fun getProducts(query: String?): List<Product> {
        return productDataSource.getProducts(query)
    }

    override suspend fun getProductsByIds(ids: Set<String>): List<Product> {
        return productDataSource.getProductsByIds(ids)
    }

    override suspend fun findProductByBarcode(barcode: String): Product? {
        return productDataSource.findProductByBarcode(barcode)
    }
}