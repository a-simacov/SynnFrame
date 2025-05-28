package com.synngate.synnframe.presentation.ui.taskx.model

import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX

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
    val currentUserId: String? = null
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
}

enum class ActionFilter(val displayName: String) {
    ALL("Все"),
    PENDING("К выполнению"),
    COMPLETED("Выполненные"),
    INITIAL("Начальные"),
    REGULAR("Обычные"),
    FINAL("Завершающие")
}