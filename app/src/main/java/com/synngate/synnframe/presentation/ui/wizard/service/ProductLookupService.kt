package com.synngate.synnframe.presentation.ui.wizard.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Сервис для поиска продуктов по различным критериям
 */
class ProductLookupService(
    private val productRepository: ProductRepository
) : BaseBarcodeScanningService() {

    /**
     * Находит продукт по штрих-коду
     * @param barcode штрих-код продукта
     * @param onResult обработчик результата
     * @param onError обработчик ошибок
     */
    override suspend fun findItemByBarcode(
        barcode: String,
        onResult: (found: Boolean, data: Any?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            withContext(Dispatchers.IO) {
                val product = productRepository.findProductByBarcode(barcode)
                if (product != null) {
                    Timber.d("Продукт найден по штрихкоду: ${product.name}")
                    onResult(true, product)
                } else {
                    Timber.w("Продукт не найден по штрихкоду: $barcode")
                    onResult(false, null)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске продукта по штрихкоду: $barcode")
            onError("Ошибка при поиске продукта: ${e.message}")
        }
    }

    /**
     * Поиск продуктов по текстовому запросу
     * @param query текстовый запрос для поиска
     * @return список продуктов, соответствующих запросу
     */
    suspend fun searchProducts(query: String): List<Product> {
        return withContext(Dispatchers.IO) {
            try {
                // Использование правильного подхода к сбору данных из Flow
                val result = mutableListOf<Product>()
                productRepository.getProductsByNameFilter(query)
                    .collect { productsFromFlow ->
                        // Сохраняем результат
                        result.addAll(productsFromFlow)
                    }
                result
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при поиске продуктов по запросу: $query")
                emptyList()
            }
        }
    }

    /**
     * Получение продукта по ID
     * @param id идентификатор продукта
     * @return продукт или null, если не найден
     */
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

    /**
     * Получение продуктов по списку ID
     * @param ids список идентификаторов продуктов
     * @return список найденных продуктов
     */
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