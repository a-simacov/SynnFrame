package com.synngate.synnframe.domain.service

import com.synngate.synnframe.data.remote.api.ActionSearchApi
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.action.SearchableActionObject
import com.synngate.synnframe.domain.repository.ProductRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

/**
 * Реализация сервиса поиска действий
 */
class ActionSearchServiceImpl(
    private val actionSearchApi: ActionSearchApi? = null,
    private val productRepository: ProductRepository? = null
) : ActionSearchService {

    override suspend fun searchActions(
        searchValue: String,
        searchableObjects: List<SearchableActionObject>,
        plannedActions: List<PlannedAction>
    ): Result<List<String>> {
        return try {
            if (searchValue.isBlank()) {
                return Result.success(emptyList())
            }

            val foundActionIds = mutableSetOf<String>()

            coroutineScope {
                // Параллельно выполняем поиск по всем настроенным объектам
                val results = searchableObjects.map { searchConfig ->
                    async {
                        if (searchConfig.isRemoteSearch) {
                            searchRemote(searchValue, searchConfig)
                        } else {
                            searchLocal(searchValue, searchConfig, plannedActions)
                        }
                    }
                }.awaitAll()

                // Объединяем результаты
                results.forEach { result ->
                    result.getOrNull()?.let { foundActionIds.addAll(it) }
                }
            }

            Result.success(foundActionIds.toList())
        } catch (e: Exception) {
            Timber.e(e, "Error searching actions for value: $searchValue")
            Result.failure(e)
        }
    }

    /**
     * Локальный поиск в загруженных данных
     */
    private suspend fun searchLocal(
        searchValue: String,
        searchConfig: SearchableActionObject,
        plannedActions: List<PlannedAction>
    ): Result<List<String>> {
        return try {
            val foundActions = when (searchConfig.objectType) {
                ActionObjectType.BIN -> searchByBin(searchValue, plannedActions)
                ActionObjectType.PALLET -> searchByPallet(searchValue, plannedActions)
                ActionObjectType.CLASSIFIER_PRODUCT -> searchByProduct(searchValue, plannedActions)
                ActionObjectType.TASK_PRODUCT -> searchByTaskProduct(searchValue, plannedActions)
                ActionObjectType.PRODUCT_QUANTITY -> emptyList() // Количество не ищется
            }

            Result.success(foundActions)
        } catch (e: Exception) {
            Timber.e(e, "Error in local search for ${searchConfig.objectType}")
            Result.failure(e)
        }
    }

    /**
     * Удаленный поиск через API
     */
    private suspend fun searchRemote(
        searchValue: String,
        searchConfig: SearchableActionObject
    ): Result<List<String>> {
        val endpoint = searchConfig.endpoint ?: return Result.success(emptyList())
        val api = actionSearchApi ?: return Result.success(emptyList())

        return try {
            when (val result = api.searchAction(endpoint, searchValue)) {
                is ApiResult.Success -> {
                    val actionId = result.data.result
                    if (actionId != null) {
                        Result.success(listOf(actionId))
                    } else {
                        Result.success(emptyList())
                    }
                }
                is ApiResult.Error -> {
                    Timber.w("Remote search error: ${result.message}")
                    Result.success(emptyList())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in remote search")
            Result.failure(e)
        }
    }

    /**
     * Поиск по коду ячейки
     */
    private fun searchByBin(
        searchValue: String,
        plannedActions: List<PlannedAction>
    ): List<String> {
        return plannedActions
            .filter { action ->
                action.placementBin?.code.equals(searchValue, ignoreCase = true)
            }
            .map { it.id }
    }

    /**
     * Поиск по коду паллеты
     */
    private fun searchByPallet(
        searchValue: String,
        plannedActions: List<PlannedAction>
    ): List<String> {
        return plannedActions
            .filter { action ->
                action.storagePallet?.code.equals(searchValue, ignoreCase = true) ||
                        action.placementPallet?.code.equals(searchValue, ignoreCase = true)
            }
            .map { it.id }
    }

    /**
     * Поиск по продукту (ID или штрихкод)
     */
    private suspend fun searchByProduct(
        searchValue: String,
        plannedActions: List<PlannedAction>
    ): List<String> {
        // Сначала ищем по ID продукта
        val actionsById = plannedActions
            .filter { action ->
                action.storageProduct?.product?.id.equals(searchValue, ignoreCase = true)
            }
            .map { it.id }

        if (actionsById.isNotEmpty()) {
            return actionsById
        }

        // Если не нашли по ID, ищем по штрихкоду
        val product = productRepository?.findProductByBarcode(searchValue)
        if (product != null) {
            return plannedActions
                .filter { action ->
                    action.storageProduct?.product?.id == product.id
                }
                .map { it.id }
        }

        return emptyList()
    }

    /**
     * Поиск по товару задания
     */
    private suspend fun searchByTaskProduct(
        searchValue: String,
        plannedActions: List<PlannedAction>
    ): List<String> {
        // Поиск по ID продукта в TaskProduct
        val actionsById = plannedActions
            .filter { action ->
                action.storageProduct?.product?.id.equals(searchValue, ignoreCase = true)
            }
            .map { it.id }

        if (actionsById.isNotEmpty()) {
            return actionsById
        }

        // Поиск по штрихкоду
        val product = productRepository?.findProductByBarcode(searchValue)
        if (product != null) {
            return plannedActions
                .filter { action ->
                    action.storageProduct?.product?.id == product.id
                }
                .map { it.id }
        }

        return emptyList()
    }
}