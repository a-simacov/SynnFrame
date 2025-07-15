package com.synngate.synnframe.presentation.ui.taskx.model

import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.model.buffer.BufferDisplayItem
import com.synngate.synnframe.presentation.ui.taskx.model.filter.FilterItem

data class TaskXDetailState(
    // Основные данные
    val task: TaskX? = null,
    val taskType: TaskTypeX? = null,

    // Состояние загрузки
    val isLoading: Boolean = false,
    val error: String? = null,

    // Фильтрация действий
    val actionFilter: ActionFilter = ActionFilter.ALL,
    val actionUiModels: List<PlannedActionUI> = emptyList(),

    // Диалоги
    val showExitDialog: Boolean = false,
    val isProcessingAction: Boolean = false,
    val showCameraScannerForSearch: Boolean = false,
    val showCompletionDialog: Boolean = false,
    val taskCompletionResult: TaskCompletionResult? = null,
    val exitDialogOperationResult: OperationResult? = null,

    // Диалог ошибки порядка выполнения
    val showValidationErrorDialog: Boolean = false,
    val validationErrorMessage: String? = null,

    // Информация о пользователе
    val currentUserId: String? = null,

    // Поиск и фильтры
    val showSearchBar: Boolean = false,
    val searchValue: String = "",
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val activeFilters: List<FilterItem> = emptyList(),
    val showFilters: Boolean = true,

    // Буфер задания
    val bufferItems: List<BufferDisplayItem> = emptyList(),
    val showBufferItems: Boolean = false
) {

    fun getDisplayActions(): List<PlannedActionUI> {
        return when (actionFilter) {
            ActionFilter.ALL -> actionUiModels.sortedBy { it.order }
            ActionFilter.PENDING -> actionUiModels.filter { !it.isCompleted }.sortedBy { it.order }
            ActionFilter.COMPLETED -> actionUiModels.filter { it.isCompleted }.sortedBy { it.order }
            ActionFilter.INITIAL -> actionUiModels.filter { it.isInitialAction }.sortedBy { it.order }
            ActionFilter.REGULAR -> actionUiModels.filter { !it.isInitialAction && !it.isFinalAction }.sortedBy { it.order }
            ActionFilter.FINAL -> actionUiModels.filter { it.isFinalAction }.sortedBy { it.order }
            ActionFilter.CURRENT -> getCurrentActions()
        }
    }

    fun hasInitialActions(): Boolean = actionUiModels.any { it.isInitialAction }
    fun hasFinalActions(): Boolean = actionUiModels.any { it.isFinalAction }

    fun getTotalActionsCount(): Int = actionUiModels.size
    fun getCompletedActionsCount(): Int = actionUiModels.count { it.isCompleted }
    fun getPendingActionsCount(): Int = getTotalActionsCount() - getCompletedActionsCount()

    fun getFilteredActions(): List<PlannedActionUI> {
        val displayActions = getDisplayActions()

        // Если нет активных фильтров, возвращаем обычный список отсортированный по order
        if (activeFilters.isEmpty()) {
            return displayActions.sortedBy { it.order }
        }

        // Фильтруем действия по всем активным фильтрам и сортируем по order
        return displayActions.filter { actionUI ->
            val action = actionUI.action

            // Проверяем соответствие каждому фильтру
            activeFilters.all { filter ->
                when (filter.field) {
                    FactActionField.STORAGE_BIN ->
                        action.storageBin?.code == (filter.data as? BinX)?.code

                    FactActionField.STORAGE_PALLET ->
                        action.storagePallet?.code == (filter.data as? Pallet)?.code

                    FactActionField.STORAGE_PRODUCT_CLASSIFIER ->
                        action.storageProductClassifier?.id == (filter.data as? Product)?.id

                    FactActionField.STORAGE_PRODUCT ->
                        action.storageProduct?.product?.id == (filter.data as? TaskProduct)?.product?.id

                    FactActionField.ALLOCATION_BIN ->
                        action.placementBin?.code == (filter.data as? BinX)?.code

                    FactActionField.ALLOCATION_PALLET ->
                        action.placementPallet?.code == (filter.data as? Pallet)?.code

                    else -> true // Для других типов полей всегда true
                }
            }
        }.sortedBy { it.order }
    }

    private fun getCurrentActions(): List<PlannedActionUI> {
        // 1. Проверяем наличие невыполненных начальных действий
        val incompleteInitialActions = actionUiModels.filter { it.isInitialAction && !it.isCompleted }
        if (incompleteInitialActions.isNotEmpty()) {
            return incompleteInitialActions.sortedBy { it.order }
        }

        // 2. Проверяем наличие невыполненных обычных действий
        val incompleteRegularActions = actionUiModels.filter {
            !it.isInitialAction && !it.isFinalAction && !it.isCompleted
        }
        if (incompleteRegularActions.isNotEmpty()) {
            // Если у нас строгий порядок выполнения, возвращаем только доступные для выполнения действия
            if (task?.taskType?.isStrictActionOrder() == true) {
                // Находим действие с минимальным порядковым номером
                val firstAction = incompleteRegularActions.minByOrNull { it.order }
                if (firstAction != null) {
                    // Если есть действия с одинаковым порядковым номером, возвращаем их все отсортированными
                    return incompleteRegularActions.filter { it.order == firstAction.order }.sortedBy { it.order }
                }
            }
            return incompleteRegularActions.sortedBy { it.order }
        }

        // 3. Проверяем наличие невыполненных финальных действий
        val incompleteFinalActions = actionUiModels.filter { it.isFinalAction && !it.isCompleted }
        if (incompleteFinalActions.isNotEmpty()) {
            // Для финальных действий всегда возвращаем только первое невыполненное
            val firstFinalAction = incompleteFinalActions.minByOrNull { it.order }
            if (firstFinalAction != null) {
                return incompleteFinalActions.filter { it.order == firstFinalAction.order }.sortedBy { it.order }
            }
            return incompleteFinalActions.sortedBy { it.order }
        }

        // 4. Если все действия выполнены, возвращаем пустой список
        return emptyList()
    }
}

enum class ActionFilter(val stringResId: Int) {
    ALL(R.string.action_filter_all),
    PENDING(R.string.action_filter_toexecute),
    COMPLETED(R.string.action_filter_executed),
    INITIAL(R.string.action_filter_initial),
    REGULAR(R.string.action_filter_regular),
    FINAL(R.string.action_filter_final),
    CURRENT(R.string.action_filter_current)
}