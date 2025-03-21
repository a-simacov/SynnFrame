package com.synngate.synnframe.domain.usecase.product

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.io.IOException

/**
 * Use Case класс для операций с товарами
 */
class ProductUseCases(
    private val productRepository: ProductRepository,
    private val loggingService: LoggingService
) : BaseUseCase {

    // Базовые операции
    fun getProducts(): Flow<List<Product>> =
        productRepository.getProducts()

    fun getProductsByNameFilter(nameFilter: String): Flow<List<Product>> =
        productRepository.getProductsByNameFilter(nameFilter)

    suspend fun getProductById(id: String): Product? =
        productRepository.getProductById(id)

    /**
     * Получение товаров по списку идентификаторов
     */
    suspend fun getProductsByIds(ids: Set<String>): List<Product> {
        return productRepository.getProductsByIds(ids)
    }

    suspend fun findProductByBarcode(barcode: String): Product? =
        productRepository.findProductByBarcode(barcode)

    fun getProductsCount(): Flow<Int> =
        productRepository.getProductsCount()

    // Операции с бизнес-логикой
    suspend fun addProduct(product: Product): Result<Unit> {
        return try {
            // Валидация товара
            validateProduct(product)

            // Добавление товара через репозиторий
            productRepository.addProduct(product)

            // Логирование успешного добавления
            loggingService.logInfo("Добавлен товар: ${product.name}")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error adding product")
            loggingService.logError("Ошибка при добавлении товара: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun addProducts(products: List<Product>): Result<Int> {
        return try {
            // Добавление списка товаров через репозиторий
            productRepository.addProducts(products)

            // Логирование успешного добавления
            loggingService.logInfo("Добавлено товаров: ${products.size}")

            Result.success(products.size)
        } catch (e: Exception) {
            Timber.e(e, "Error adding products")
            loggingService.logError("Ошибка при добавлении товаров: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateProduct(product: Product): Result<Unit> {
        return try {
            // Валидация товара
            validateProduct(product)

            // Обновление товара через репозиторий
            productRepository.updateProduct(product)

            // Логирование успешного обновления
            loggingService.logInfo("Обновлен товар: ${product.name}")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating product")
            loggingService.logError("Ошибка при обновлении товара: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(id: String): Result<Unit> {
        return try {
            // Получаем товар перед удалением
            val product = productRepository.getProductById(id)
            if (product == null) {
                return Result.failure(IllegalArgumentException("Product not found: $id"))
            }

            // Удаление товара через репозиторий
            productRepository.deleteProduct(id)

            // Логирование успешного удаления
            loggingService.logInfo("Удален товар: ${product.name}")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting product")
            loggingService.logError("Ошибка при удалении товара: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteAllProducts(): Result<Unit> {
        return try {
            // Удаление всех товаров через репозиторий
            productRepository.deleteAllProducts()

            // Логирование успешного удаления
            loggingService.logInfo("Удалены все товары")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting all products")
            loggingService.logError("Ошибка при удалении всех товаров: ${e.message}")
            Result.failure(e)
        }
    }

    // Бизнес-логика синхронизации, перенесенная из репозитория
    suspend fun syncProductsWithServer(): Result<Int> {
        return try {
            // Получаем товары с сервера
            val response = productRepository.getProductsFromRemote()

            when (response) {
                is ApiResult.Success -> {
                    val products = response.data
                    if (products.isNotEmpty()) {
                        // Очищаем существующие товары и добавляем новые
                        productRepository.deleteAllProducts()
                        productRepository.addProducts(products)

                        loggingService.logInfo("Синхронизировано товаров: ${products.size}")
                        Result.success(products.size)
                    } else {
                        loggingService.logWarning("Пустой ответ при синхронизации товаров")
                        Result.failure(IOException("Empty product synchronization response"))
                    }
                }

                is ApiResult.Error -> {
                    // Извлекаем сообщение об ошибке из ответа
                    val errorBody = response.message
                    loggingService.logWarning("Ошибка синхронизации товаров: $errorBody (${response.code})")
                    Result.failure(IOException("Product synchronization failed: $errorBody (${response.code})"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during product synchronization")
            loggingService.logError("Исключение при синхронизации товаров: ${e.message}")
            Result.failure(e)
        }
    }

    // Вспомогательные методы
    private fun validateProduct(product: Product) {
        if (product.name.isBlank()) {
            throw IllegalArgumentException("Product name cannot be empty")
        }

        if (product.mainUnitId.isBlank() && product.units.isNotEmpty()) {
            throw IllegalArgumentException("Main unit ID cannot be empty when units are specified")
        }

        // Дополнительные правила валидации по необходимости
    }
}