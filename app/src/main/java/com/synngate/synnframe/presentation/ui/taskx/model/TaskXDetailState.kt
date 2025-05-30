package com.synngate.synnframe.presentation.ui.taskx.model

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

    // Буфер задания
    val bufferItems: List<BufferDisplayItem> = emptyList(),
    val showBufferItems: Boolean = false // Флаг для отображения буфера
) {

    fun getDisplayActions(): List<PlannedActionUI> {
        return when (actionFilter) {
            ActionFilter.ALL -> actionUiModels
            ActionFilter.PENDING -> actionUiModels.filter { !it.isCompleted }
            ActionFilter.COMPLETED -> actionUiModels.filter { it.isCompleted }
            ActionFilter.INITIAL -> actionUiModels.filter { it.isInitialAction }
            ActionFilter.REGULAR -> actionUiModels.filter { !it.isInitialAction && !it.isFinalAction }
            ActionFilter.FINAL -> actionUiModels.filter { it.isFinalAction }
        }
    }

    fun hasInitialActions(): Boolean = actionUiModels.any { it.isInitialAction }
    fun hasFinalActions(): Boolean = actionUiModels.any { it.isFinalAction }

    fun getTotalActionsCount(): Int = actionUiModels.size
    fun getCompletedActionsCount(): Int = actionUiModels.count { it.isCompleted }
    fun getPendingActionsCount(): Int = getTotalActionsCount() - getCompletedActionsCount()

    fun getFilteredActions(): List<PlannedActionUI> {
        val displayActions = getDisplayActions()

        // Если нет активных фильтров, возвращаем обычный список
        if (activeFilters.isEmpty()) {
            return displayActions
        }

        // Фильтруем действия по всем активным фильтрам
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
        }
    }
}

enum class ActionFilter(val displayName: String) {
    ALL("Все"),
    PENDING("К выполнению"),
    COMPLETED("Выполненные"),
    INITIAL("Начальные"),
    REGULAR("Обычные"),
    FINAL("Завершающие")
}