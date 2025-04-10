package com.synngate.synnframe.data.datasource.mock

import com.synngate.synnframe.data.datasource.ProductDataSource
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product

class MockProductDataSource : ProductDataSource {
    private val products = listOf(
        Product(
            id = "p1",
            name = "Наушники вкладыши",
            accountingModel = AccountingModel.QTY,
            articleNumber = "H-12345",
            mainUnitId = "u1",
            units = emptyList()
        ),
        Product(
            id = "p2",
            name = "Молоко",
            accountingModel = AccountingModel.QTY,
            articleNumber = "M-67890",
            mainUnitId = "u2",
            units = emptyList()
        ),
        // Можно добавить больше тестовых данных
    )

    override suspend fun getProducts(query: String?): List<Product> {
        return if (query.isNullOrEmpty()) {
            products
        } else {
            products.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.articleNumber.contains(query, ignoreCase = true)
            }
        }
    }

    override suspend fun getProductsByIds(ids: Set<String>): List<Product> {
        return products.filter { it.id in ids }
    }

    override suspend fun findProductByBarcode(barcode: String): Product? {
        // Для теста просто возвращаем первый продукт
        return products.firstOrNull()
    }
}