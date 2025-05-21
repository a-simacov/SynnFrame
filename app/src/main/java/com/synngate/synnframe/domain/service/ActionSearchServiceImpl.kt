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
                    // Проверяем, имеет ли действие шаги с типом BIN
                    if (action.hasPlacementObjectType(ActionObjectType.BIN)) {
                        action.placementBin?.code?.lowercase() == searchLower
                    } else false
                }
                ActionObjectType.PALLET -> {
                    // Проверяем паллеты в шагах хранения и размещения
                    if (action.hasStorageObjectType(ActionObjectType.PALLET) &&
                        action.storagePallet?.code?.lowercase() == searchLower) {
                        true
                    } else if (action.hasPlacementObjectType(ActionObjectType.PALLET) &&
                        action.placementPallet?.code?.lowercase() == searchLower) {
                        true
                    } else {
                        false
                    }
                }
                ActionObjectType.CLASSIFIER_PRODUCT -> {
                    // Проверяем товары в шагах хранения
                    if (action.hasStorageObjectType(ActionObjectType.CLASSIFIER_PRODUCT) ||
                        action.hasStorageObjectType(ActionObjectType.TASK_PRODUCT)) {

                        if (action.storageProduct?.product?.id == searchValue) {
                            true
                        } else {
                            val productFromBarcode = productRepository.findProductByBarcode(searchValue)
                            if (productFromBarcode != null) {
                                action.storageProduct?.product?.id == productFromBarcode.id
                            } else {
                                action.storageProduct?.product?.getAllBarcodes()?.any {
                                    it.lowercase() == searchLower
                                } == true
                            }
                        }
                    } else false
                }
                ActionObjectType.TASK_PRODUCT -> {
                    // Проверяем товары в шагах хранения
                    if (action.hasStorageObjectType(ActionObjectType.TASK_PRODUCT) ||
                        action.hasStorageObjectType(ActionObjectType.CLASSIFIER_PRODUCT)) {

                        action.storageProduct?.product?.id == searchValue ||
                                productRepository.findProductByBarcode(searchValue)?.let { foundProduct ->
                                    action.storageProduct?.product?.id == foundProduct.id
                                } == true
                    } else false
                }
                ActionObjectType.PRODUCT_QUANTITY -> {
                    false // Для количества товара поиск не применяется
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

        val processedEndpoint = endpoint
            .replace("{taskId}", taskId)
            .replace("{actionId}", currentActionId ?: "")

        return when (val result = actionSearchApi.searchAction(processedEndpoint, searchValue)) {
            is ApiResult.Success -> {
                if (!result.data.results.isNullOrEmpty()) {
                    result.data.results
                } else {
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