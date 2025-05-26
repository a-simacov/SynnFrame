package com.synngate.synnframe.presentation.ui.taskx.manager

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.ui.taskx.entity.SearchActionFieldType
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField

class ActionSearchManager(
    private val taskXService: TaskXService
) {

    /**
     * Выполняет поиск действий по значению
     */
    suspend fun searchActions(
        task: TaskX,
        searchValue: String
    ): SearchResult {
        if (searchValue.isBlank()) {
            return SearchResult.Error("Значение для поиска не может быть пустым")
        }

        val searchableFields = task.taskType?.searchActionFieldsTypes ?: emptyList()
        if (searchableFields.isEmpty()) {
            return SearchResult.Error("Для данного типа задания поиск не настроен")
        }

        val foundActionIds = mutableSetOf<String>()

        // Локальный поиск
        for (field in searchableFields.filter { !it.isRemoteSearch }) {
            val localResults = performLocalSearch(task, searchValue, field.actionField)
            foundActionIds.addAll(localResults)
        }

        // Удаленный поиск
        for (field in searchableFields.filter { it.isRemoteSearch }) {
            val remoteResults = performRemoteSearch(task, searchValue, field)
            foundActionIds.addAll(remoteResults)
        }

        return when {
            foundActionIds.isEmpty() -> SearchResult.NotFound("Действия не найдены")
            foundActionIds.size == 1 -> SearchResult.SingleResult(foundActionIds.first())
            else -> SearchResult.MultipleResults(foundActionIds.toList())
        }
    }

    /**
     * Локальный поиск по плановым действиям
     */
    private fun performLocalSearch(
        task: TaskX,
        searchValue: String,
        fieldType: FactActionField
    ): List<String> {
        return task.plannedActions.filter { action ->
            when (fieldType) {
                FactActionField.STORAGE_BIN ->
                    action.storageBin?.code?.equals(searchValue, ignoreCase = true) == true

                FactActionField.STORAGE_PALLET ->
                    action.storagePallet?.code?.equals(searchValue, ignoreCase = true) == true

                FactActionField.ALLOCATION_BIN ->
                    action.placementBin?.code?.equals(searchValue, ignoreCase = true) == true

                FactActionField.ALLOCATION_PALLET ->
                    action.placementPallet?.code?.equals(searchValue, ignoreCase = true) == true

                FactActionField.STORAGE_PRODUCT_CLASSIFIER ->
                    matchesProduct(action, searchValue)

                FactActionField.STORAGE_PRODUCT ->
                    matchesTaskProduct(action, searchValue)

                else -> false
            }
        }.map { it.id }
    }

    /**
     * Удаленный поиск через API
     */
    private suspend fun performRemoteSearch(
        task: TaskX,
        searchValue: String,
        field: SearchActionFieldType
    ): List<String> {
        if (field.endpoint.isBlank()) return emptyList()

        val result = taskXService.searchActions(
            taskId = task.id,
            searchValue = searchValue,
            fieldType = field.actionField.name
        )

        return result.getOrNull() ?: emptyList()
    }

    /**
     * Проверка соответствия товара
     */
    private fun matchesProduct(action: PlannedAction, searchValue: String): Boolean {
        val product = action.storageProductClassifier ?: return false
        return product.id == searchValue ||
                product.articleNumber == searchValue
    }

    /**
     * Проверка соответствия товара задания
     */
    private fun matchesTaskProduct(action: PlannedAction, searchValue: String): Boolean {
        val taskProduct = action.storageProduct ?: return false
        val product = taskProduct.product

        return product.id == searchValue ||
                product.articleNumber == searchValue
    }

    /**
     * Возвращает поля, которые можно сохранить в буфер
     */
    fun getSavableFields(task: TaskX): List<SearchActionFieldType> {
        return task.taskType?.searchActionFieldsTypes?.filter { it.saveToTaskBuffer } ?: emptyList()
    }
}

sealed class SearchResult {
    data class SingleResult(val actionId: String) : SearchResult()
    data class MultipleResults(val actionIds: List<String>) : SearchResult()
    data class NotFound(val message: String) : SearchResult()
    data class Error(val message: String) : SearchResult()
}