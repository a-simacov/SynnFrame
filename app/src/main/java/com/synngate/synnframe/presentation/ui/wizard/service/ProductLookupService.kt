package com.synngate.synnframe.presentation.ui.wizard.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.domain.service.TaskContextManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ProductLookupService(
    private val productRepository: ProductRepository,
    private val taskContextManager: TaskContextManager? = null
) : BaseLookupService<Product>() {

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

    override suspend fun findEntityInContext(barcode: String): Product? {
        val currentTask = taskContextManager?.lastStartedTaskX?.value

        if (currentTask != null) {
            return currentTask.plannedActions
                .flatMap { action ->
                    listOfNotNull(action.storageProduct?.product)
                }
                .distinct()
                .firstOrNull { product ->
                    product.getAllBarcodes().any { it == barcode }
                }
        }
        return null
    }

    override suspend fun findEntityInRepository(barcode: String): Product? {
        return productRepository.findProductByBarcode(barcode)
    }

    override suspend fun createLocalEntity(barcode: String): Product? {
        return null
    }

    override suspend fun searchEntitiesInContext(
        query: String,
        additionalParams: Map<String, Any>
    ): List<Product> {
        val currentTask = taskContextManager?.lastStartedTaskX?.value ?: return emptyList()

        val taskProducts = currentTask.plannedActions
            .flatMap { action ->
                listOfNotNull(action.storageProduct?.product)
            }
            .distinct()

        return if (query.isEmpty()) {
            taskProducts
        } else {
            taskProducts.filter { product ->
                product.name.contains(query, ignoreCase = true) ||
                        product.articleNumber.contains(query, ignoreCase = true) ||
                        product.getAllBarcodes().any { it.contains(query, ignoreCase = true) }
            }
        }
    }

    override suspend fun searchEntitiesInRepository(
        query: String,
        additionalParams: Map<String, Any>
    ): List<Product> {
        try {
            val planProductIds = additionalParams["planProductIds"] as? Set<String>

            return if (query.isEmpty() && planProductIds != null && planProductIds.isNotEmpty()) {
                productRepository.getProductsByIds(planProductIds)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске продуктов: query=$query")
            return emptyList()
        }
    }

    override fun getEntityId(entity: Product): String? {
        return entity.id
    }
}