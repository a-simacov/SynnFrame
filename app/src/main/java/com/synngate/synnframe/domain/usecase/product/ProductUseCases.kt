package com.synngate.synnframe.domain.usecase.product

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.io.IOException

class ProductUseCases(
    private val productRepository: ProductRepository
) : BaseUseCase {

    fun getProducts(): Flow<List<Product>> =
        productRepository.getProducts()

    fun getProductsByNameFilter(nameFilter: String): Flow<List<Product>> =
        productRepository.getProductsByNameFilter(nameFilter)

    suspend fun getProductById(id: String): Product? =
        productRepository.getProductById(id)

    suspend fun getProductsByIds(ids: Set<String>): List<Product> {
        return productRepository.getProductsByIds(ids)
    }

    suspend fun findProductByBarcode(barcode: String): Product? =
        productRepository.findProductByBarcode(barcode)

    fun getProductsCount(): Flow<Int> =
        productRepository.getProductsCount()

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

                        Timber.i("Synced products: ${products.size}")
                        Result.success(products.size)
                    } else {
                        Timber.w("Empty product synchronization response")
                        Result.failure(IOException("Empty product synchronization response"))
                    }
                }

                is ApiResult.Error -> {
                    // Извлекаем сообщение об ошибке из ответа
                    val errorBody = response.message
                    Timber.w("Product synchronization failed: $errorBody (${response.code})")
                    Result.failure(IOException("Product synchronization failed: $errorBody (${response.code})"))
                }
            }
        } catch (e: Exception) {
            Timber.e("Exception during product synchronization: ${e.message}")
            Result.failure(e)
        }
    }
}