package com.synngate.synnframe.presentation.service.webserver.controller

import com.synngate.synnframe.data.remote.dto.ProductDto
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.presentation.service.webserver.WebServerConstants
import com.synngate.synnframe.presentation.service.webserver.WebServerSyncIntegrator
import com.synngate.synnframe.presentation.service.webserver.util.respondError
import com.synngate.synnframe.presentation.service.webserver.util.respondSuccess
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.util.Locale

class ProductsController(
    private val productRepository: ProductRepository,
    private val syncIntegrator: WebServerSyncIntegrator,
    private val saveSyncHistoryRecord: suspend (Int, Int, Int, Long) -> Unit
) : WebServerController {

    @Serializable
    data class ProductsResponse(
        val total: Int,
        val new: Int,
        val updated: Int,
        val fullUpdate: Boolean,
        val processingTime: Long
    )

    suspend fun handleProducts(call: ApplicationCall) {
        val startTime = System.currentTimeMillis()

        try {
            // Получаем данные запроса
            val productsData = call.receive<List<ProductDto>>()

            if (productsData.isEmpty()) {
                call.respondError("No products received", code = 400, statusCode = HttpStatusCode.BadRequest)
                return
            }

            // Конвертируем DTO в доменные модели
            val products = productsData.map { it.toDomainModel() }

            // Для каждого товара проверяем, есть ли он уже в базе
            val newProducts = mutableListOf<Product>()
            val updatedProducts = mutableListOf<Product>()

            for (product in products) {
                val existingProduct = productRepository.getProductById(product.id)
                if (existingProduct == null) {
                    newProducts.add(product)
                } else {
                    updatedProducts.add(product)
                }
            }

            // Определяем нужно ли полное обновление
            val shouldClearExisting = products.size > 100 &&
                    products.size > (productRepository.getProductsCount().first() * 0.9)

            // Применяем изменения
            if (shouldClearExisting) {
                productRepository.deleteAllProducts()
                productRepository.addProducts(products)
//                logger.logInfo(String.format(WebServerConstants.LOG_PRODUCTS_FULL_UPDATE, products.size))
            } else {
                if (newProducts.isNotEmpty()) {
                    productRepository.addProducts(newProducts)
                }
                for (product in updatedProducts) {
                    productRepository.updateProduct(product)
                }
            }

            // Вычисляем время выполнения
            val duration = System.currentTimeMillis() - startTime

            if (shouldClearExisting)
                Timber.i(String.format(Locale.getDefault(), WebServerConstants.LOG_PRODUCTS_FULL_UPDATE, products.size))
            else
                Timber.i(
                    String.format(Locale.getDefault(), WebServerConstants.LOG_PRODUCTS_RECEIVED, products.size, newProducts.size, updatedProducts.size, duration)
                )

            // Сохраняем запись в историю синхронизаций
            try {
                saveSyncHistoryRecord(
                    0,
                    products.size,
                    0,
                    duration
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to save sync history record")
            }

            // Обновляем UI через интегратор
            syncIntegrator.updateSyncProgress(
                productsDownloaded = products.size,
                operation = WebServerConstants.OPERATION_PRODUCTS_RECEIVED
            )

            // Отправляем ответ
            call.respondSuccess(
                ProductsResponse(
                    total = products.size,
                    new = if (shouldClearExisting) products.size else newProducts.size,
                    updated = if (shouldClearExisting) 0 else updatedProducts.size,
                    fullUpdate = shouldClearExisting,
                    processingTime = duration
                )
            )

        } catch (e: Exception) {
            handleError(call, e, "products endpoint")
            call.respondError("Failed to process products: ${e.message}")
        }
    }
}