// Файл: com.synngate.synnframe.domain.usecase.product.ProductUseCases.kt

package com.synngate.synnframe.domain.usecase.product

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Use Case класс для операций с товарами
 */
class ProductUseCases(
    private val productRepository: ProductRepository,
    private val logRepository: LogRepository
) : BaseUseCase {

    // Базовые операции
    fun getProducts(): Flow<List<Product>> =
        productRepository.getProducts()

    fun getProductsByNameFilter(nameFilter: String): Flow<List<Product>> =
        productRepository.getProductsByNameFilter(nameFilter)

    suspend fun getProductById(id: String): Product? =
        productRepository.getProductById(id)

    suspend fun findProductByBarcode(barcode: String): Product? =
        productRepository.findProductByBarcode(barcode)

    fun getProductsCount(): Flow<Int> =
        productRepository.getProductsCount()

    // Операции с бизнес-логикой
    suspend fun syncProductsWithServer(): Result<Int> {
        return try {
            val result = productRepository.syncProductsWithServer()

            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                logRepository.logInfo("Синхронизировано товаров: $count")
            } else {
                logRepository.logWarning("Ошибка синхронизации товаров: ${result.exceptionOrNull()?.message}")
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "Exception during products synchronization")
            logRepository.logError("Исключение при синхронизации товаров: ${e.message}")
            Result.failure(e)
        }
    }
}