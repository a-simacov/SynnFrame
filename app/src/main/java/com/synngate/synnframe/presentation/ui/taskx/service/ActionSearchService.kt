package com.synngate.synnframe.presentation.ui.taskx.service

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.model.PlannedActionUI
import com.synngate.synnframe.presentation.ui.taskx.model.filter.PlannedActionsFilter
import com.synngate.synnframe.presentation.ui.taskx.model.filter.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ActionSearchService(
    private val productUseCases: ProductUseCases
) {
    /**
     * Выполняет поиск действий по значению с учетом текущего фильтра и порядка выполнения
     */
    suspend fun searchActions(
        value: String,
        task: TaskX,
        currentFilter: PlannedActionsFilter
    ): SearchResult = withContext(Dispatchers.IO) {
        if (value.isBlank()) {
            return@withContext SearchResult.Error("Search value cannot be empty")
        }

        val searchableFields = task.taskType?.searchActionFieldsTypes ?: emptyList()
        if (searchableFields.isEmpty()) {
            return@withContext SearchResult.Error("Search is not configured for this task type")
        }

        // Шаг 1: Определить множество действий для поиска с учетом порядка выполнения
        val availableActions = getAvailableActionsForSearch(task)
        if (availableActions.isEmpty()) {
            return@withContext SearchResult.Error("No actions available for search")
        }

        // Шаг 2: Фильтруем действия с учетом уже установленных фильтров
        val filteredActions = if (currentFilter.hasActiveFilters()) {
            currentFilter.filterActions(availableActions)
        } else {
            availableActions
        }

        if (filteredActions.isEmpty()) {
            return@withContext SearchResult.NotFound("No actions matching current filters")
        }

        // Шаг 3: Пытаемся найти объект по значению для каждого типа поля
        for (searchableField in searchableFields) {
            val field = searchableField.actionField

            val result = when (field) {
                FactActionField.STORAGE_BIN ->
                    searchBin(value, filteredActions, true)

                FactActionField.ALLOCATION_BIN ->
                    searchBin(value, filteredActions, false)

                FactActionField.STORAGE_PALLET ->
                    searchPallet(value, filteredActions, true)

                FactActionField.ALLOCATION_PALLET ->
                    searchPallet(value, filteredActions, false)

                FactActionField.STORAGE_PRODUCT_CLASSIFIER ->
                    searchProductClassifier(value, filteredActions)

                FactActionField.STORAGE_PRODUCT ->
                    searchTaskProduct(value, filteredActions)

                else -> null
            }

            // Если для данного типа поля нашли хотя бы одно действие, возвращаем результат
            if (result != null) {
                return@withContext result
            }
        }

        // Если после проверки всех типов полей ничего не нашли
        return@withContext SearchResult.NotFound("No results found for query '$value'")
    }

    /**
     * Получает доступные для поиска действия с учетом порядка выполнения
     */
    private fun getAvailableActionsForSearch(task: TaskX): List<PlannedActionUI> {
        // Проверяем наличие невыполненных начальных действий
        val hasUncompletedInitialActions = task.getInitialActions().any {
            !it.isFullyCompleted(task.factActions)
        }
        val isTaskInProgress = task.status == TaskXStatus.IN_PROGRESS

        // Если есть невыполненные начальные действия, поиск только по ним
        if (hasUncompletedInitialActions) {
            return task.getInitialActions()
                .filter { !it.isFullyCompleted(task.factActions) }
                .map { PlannedActionUI.fromDomain(it, task.factActions, isTaskInProgress) }
                .sortedBy { it.order }
        }

        // Если начальные действия выполнены, проверяем регулярные
        val regularActions = task.getRegularActions()
        val hasUncompletedRegularActions = regularActions.any {
            !it.isFullyCompleted(task.factActions)
        }

        // Если есть невыполненные регулярные действия
        if (hasUncompletedRegularActions) {
            val isStrictOrder = task.taskType?.isStrictActionOrder() == true

            return if (isStrictOrder) {
                // При строгом порядке возвращаем только действия до первого невыполненного
                val firstIncomplete = regularActions
                    .filter { !it.isFullyCompleted(task.factActions) }
                    .minByOrNull { it.order }

                if (firstIncomplete != null) {
                    regularActions
                        .filter { it.order <= firstIncomplete.order }
                        .map { PlannedActionUI.fromDomain(it, task.factActions, isTaskInProgress) }
                } else {
                    emptyList()
                }
            } else {
                // При произвольном порядке возвращаем только невыполненные регулярные действия
                regularActions
                    .filter { !it.isFullyCompleted(task.factActions) }
                    .map { PlannedActionUI.fromDomain(it, task.factActions, isTaskInProgress) }
            }
        }

        // Если все регулярные действия выполнены, проверяем завершающие
        val finalActions = task.getFinalActions()
        val hasUncompletedFinalActions = finalActions.any {
            !it.isFullyCompleted(task.factActions)
        }

        if (hasUncompletedFinalActions) {
            return finalActions
                .filter { !it.isFullyCompleted(task.factActions) }
                .map { PlannedActionUI.fromDomain(it, task.factActions, isTaskInProgress) }
                .sortedBy { it.order }
        }

        // Если все действия выполнены, возвращаем пустой список
        return emptyList()
    }

    /**
     * Поиск ячейки среди действий
     */
    private suspend fun searchBin(
        value: String,
        actions: List<PlannedActionUI>,
        isStorage: Boolean
    ): SearchResult? {
        // Создаем объект ячейки из строки
        val bin = BinX(code = value, zone = "")

        // Ищем действия с такой ячейкой
        val foundActions = actions.filter { actionUI ->
            val action = actionUI.action
            if (isStorage) {
                action.storageBin?.code == value
            } else {
                action.placementBin?.code == value
            }
        }

        if (foundActions.isNotEmpty()) {
            val field = if (isStorage) FactActionField.STORAGE_BIN else FactActionField.ALLOCATION_BIN
            return SearchResult.Success(
                field = field,
                value = bin,
                actionIds = foundActions.map { it.id }
            )
        }

        return null
    }

    /**
     * Поиск паллеты среди действий
     */
    private suspend fun searchPallet(
        value: String,
        actions: List<PlannedActionUI>,
        isStorage: Boolean
    ): SearchResult? {
        // Создаем объект паллеты из строки
        val pallet = Pallet(code = value, isClosed = false)

        // Ищем действия с такой паллетой
        val foundActions = actions.filter { actionUI ->
            val action = actionUI.action
            if (isStorage) {
                action.storagePallet?.code == value
            } else {
                action.placementPallet?.code == value
            }
        }

        if (foundActions.isNotEmpty()) {
            val field = if (isStorage) FactActionField.STORAGE_PALLET else FactActionField.ALLOCATION_PALLET
            return SearchResult.Success(
                field = field,
                value = pallet,
                actionIds = foundActions.map { it.id }
            )
        }

        return null
    }

    /**
     * Поиск товара классификатора среди действий
     */
    private suspend fun searchProductClassifier(
        value: String,
        actions: List<PlannedActionUI>
    ): SearchResult? {
        try {
            // Сначала ищем товар по штрихкоду
            var product = productUseCases.findProductByBarcode(value)

            // Если не нашли по штрихкоду, пробуем по ID
            if (product == null) {
                product = productUseCases.getProductById(value)
            }

            if (product != null) {
                // Ищем действия с таким товаром
                val foundActions = actions.filter { actionUI ->
                    val action = actionUI.action
                    action.storageProductClassifier?.id == product.id
                }

                if (foundActions.isNotEmpty()) {
                    return SearchResult.Success(
                        field = FactActionField.STORAGE_PRODUCT_CLASSIFIER,
                        value = product,
                        actionIds = foundActions.map { it.id }
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error searching for product: $value")
        }

        return null
    }

    /**
     * Поиск товара задания среди действий
     */
    private suspend fun searchTaskProduct(
        value: String,
        actions: List<PlannedActionUI>
    ): SearchResult? {
        try {
            // Сначала ищем товар по штрихкоду
            var baseProduct = productUseCases.findProductByBarcode(value)

            // Если не нашли по штрихкоду, пробуем по ID
            if (baseProduct == null) {
                baseProduct = productUseCases.getProductById(value)
            }

            if (baseProduct != null) {
                // Ищем действия с таким товаром
                val foundActions = actions.filter { actionUI ->
                    val action = actionUI.action
                    action.storageProduct?.product?.id == baseProduct.id
                }

                if (foundActions.isNotEmpty()) {
                    // Берем первое найденное действие для создания TaskProduct
                    val firstAction = foundActions.first().action
                    val taskProduct = firstAction.storageProduct

                    if (taskProduct != null) {
                        return SearchResult.Success(
                            field = FactActionField.STORAGE_PRODUCT,
                            value = taskProduct,
                            actionIds = foundActions.map { it.id }
                        )
                    } else {
                        // Если нет готового TaskProduct, создаем его из базового товара
                        val newTaskProduct = TaskProduct(
                            id = java.util.UUID.randomUUID().toString(),
                            product = baseProduct,
                            status = com.synngate.synnframe.domain.entity.taskx.ProductStatus.STANDARD
                        )

                        return SearchResult.Success(
                            field = FactActionField.STORAGE_PRODUCT,
                            value = newTaskProduct,
                            actionIds = foundActions.map { it.id }
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error searching for task product: $value")
        }

        return null
    }
}