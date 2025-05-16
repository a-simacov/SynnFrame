package com.synngate.synnframe.presentation.ui.wizard.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ProductLookupService(
    private val productRepository: ProductRepository
) : BaseBarcodeScanningService() {

    override suspend fun findItemByBarcode(
        barcode: String,
        onResult: (found: Boolean, data: Any?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            withContext(Dispatchers.IO) {
                val product = productRepository.findProductByBarcode(barcode)
                if (product != null) {
                    onResult(true, product)
                } else {
                    onResult(false, null)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске продукта по штрихкоду: $barcode")
            onError("Ошибка при поиске продукта: ${e.message}")
        }
    }

    suspend fun getProductById(id: String): Product? {
        return withContext(Dispatchers.IO) {
            try {
                productRepository.getProductById(id)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при получении продукта по ID: $id")
                null
            }
        }
    }

    suspend fun getProductsByIds(ids: Set<String>): List<Product> {
        return withContext(Dispatchers.IO) {
            try {
                if (ids.isEmpty()) {
                    return@withContext emptyList()
                }
                productRepository.getProductsByIds(ids)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при получении продуктов по ID: $ids")
                emptyList()
            }
        }
    }
}