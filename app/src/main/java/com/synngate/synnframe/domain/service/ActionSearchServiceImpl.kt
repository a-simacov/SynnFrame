package com.synngate.synnframe.domain.service

import com.synngate.synnframe.data.remote.api.ActionSearchApi
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.action.SearchableActionObject
import com.synngate.synnframe.domain.repository.ProductRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

class ActionSearchServiceImpl(
    private val actionSearchApi: ActionSearchApi,
    private val productRepository: ProductRepository
) : ActionSearchService {

    override suspend fun searchActions(
        searchValue: String,
        searchableObjects: List<SearchableActionObject>,
        plannedActions: List<PlannedAction>,
        taskId: String,
        currentActionId: String?
    ): Result<List<String>> = coroutineScope {
        try {
            val foundActionIds = mutableSetOf<String>()

            // Используем параллельное выполнение для всех поисков
            searchableObjects.map { searchObject ->
                async {
                    if (searchObject.isRemoteSearch) {
                        remoteSearch(searchObject, searchValue, taskId, currentActionId)
                    } else {
                        localSearch(searchObject, searchValue, plannedActions)
                    }
                }
            }.forEach { deferred ->
                val result = deferred.await()
                foundActionIds.addAll(result)
            }

            Result.success(foundActionIds.toList())
        } catch (e: Exception) {
            Timber.e(e, "Error searching actions: $searchValue")
            Result.failure(e)
        }
    }

    private suspend fun localSearch(
        searchObject: SearchableActionObject,
        searchValue: String,
        plannedActions: List<PlannedAction>
    ): List<String> {
        val foundActionIds = mutableListOf<String>()
        val searchLower = searchValue.lowercase()

        for (action in plannedActions) {
            val found = when (searchObject.objectType) {
                ActionObjectType.BIN -> {
                    action.placementBin?.code?.lowercase() == searchLower
                }
                ActionObjectType.PALLET -> {
                    action.storagePallet?.code?.lowercase() == searchLower ||
                            action.placementPallet?.code?.lowercase() == searchLower
                }
                ActionObjectType.CLASSIFIER_PRODUCT -> {
                    // Сначала пробуем искать по ID
                    if (action.storageProduct?.product?.id == searchValue) {
                        true
                    } else {
                        // Если не найдено по ID, пробуем найти по штрихкоду через ProductRepository
                        val productFromBarcode = productRepository.findProductByBarcode(searchValue)
                        if (productFromBarcode != null) {
                            action.storageProduct?.product?.id == productFromBarcode.id
                        } else {
                            // Проверяем штрихкоды в локальных данных продукта
                            action.storageProduct?.product?.getAllBarcodes()?.any {
                                it.lowercase() == searchLower
                            } == true
                        }
                    }
                }
                ActionObjectType.TASK_PRODUCT -> {
                    // Для TASK_PRODUCT ищем только по ID продукта
                    action.storageProduct?.product?.id == searchValue ||
                            // Или по штрихкоду через репозиторий
                            productRepository.findProductByBarcode(searchValue)?.let { foundProduct ->
                                action.storageProduct?.product?.id == foundProduct.id
                            } == true
                }
                ActionObjectType.PRODUCT_QUANTITY -> {
                    // Для количества не ищем по штрихкоду, только через другие типы
                    false
                }
            }

            if (found) {
                foundActionIds.add(action.id)
            }
        }

        return foundActionIds
    }

    private suspend fun remoteSearch(
        searchObject: SearchableActionObject,
        searchValue: String,
        taskId: String,
        currentActionId: String?
    ): List<String> {
        val endpoint = searchObject.endpoint ?: return emptyList()

        // Заменяем параметры в endpoint
        val processedEndpoint = endpoint
            .replace("{taskId}", taskId)
            .replace("{actionId}", currentActionId ?: "")

        Timber.d("Remote search: $searchValue, endpoint: $processedEndpoint")

        return when (val result = actionSearchApi.searchAction(processedEndpoint, searchValue)) {
            is ApiResult.Success -> {
                // Сначала проверяем поле results для множественных результатов
                if (!result.data.results.isNullOrEmpty()) {
                    result.data.results
                } else {
                    // Для обратной совместимости проверяем поле result
                    result.data.result?.let { listOf(it) } ?: emptyList()
                }
            }
            is ApiResult.Error -> {
                Timber.e("Remote search error: ${result.message}")
                emptyList()
            }
        }
    }
}