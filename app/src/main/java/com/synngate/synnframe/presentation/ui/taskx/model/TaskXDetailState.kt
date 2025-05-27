package com.synngate.synnframe.presentation.ui.taskx.model

import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction

/**
 * Состояние экрана детального просмотра задания
 */
data class TaskXDetailState(
    // Основные данные
    val task: TaskX? = null,
    val taskType: TaskTypeX? = null,

    // Состояние загрузки
    val isLoading: Boolean = false,
    val error: String? = null,

    // Фильтрация действий
    val actionFilter: ActionFilter = ActionFilter.ALL,
    val filteredActions: List<PlannedAction> = emptyList(),

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
    /**
     * Получить отображаемые действия с учетом фильтра
     */
    fun getDisplayActions(): List<PlannedAction> {
        val actions = task?.plannedActions ?: return emptyList()

        return when (actionFilter) {
            ActionFilter.ALL -> actions
            ActionFilter.PENDING -> actions.filter { !it.isCompleted && !it.manuallyCompleted }
            ActionFilter.COMPLETED -> actions.filter { it.isCompleted || it.manuallyCompleted }
            ActionFilter.INITIAL -> actions.filter { it.isInitialAction() }
            ActionFilter.REGULAR -> actions.filter { it.isRegularAction() }
            ActionFilter.FINAL -> actions.filter { it.isFinalAction() }
        }
    }

    /**
     * Проверка наличия действий определенного типа
     */
    fun hasInitialActions(): Boolean = task?.getInitialActions()?.isNotEmpty() == true
    fun hasFinalActions(): Boolean = task?.getFinalActions()?.isNotEmpty() == true

    /**
     * Подсчет действий
     */
    fun getTotalActionsCount(): Int = task?.plannedActions?.size ?: 0
    fun getCompletedActionsCount(): Int = task?.plannedActions?.count { it.isCompleted || it.manuallyCompleted } ?: 0
    fun getPendingActionsCount(): Int = getTotalActionsCount() - getCompletedActionsCount()
}

/**
 * Фильтры для отображения действий
 */
enum class ActionFilter(val displayName: String) {
    ALL("Все"),
    PENDING("К выполнению"),
    COMPLETED("Выполненные"),
    INITIAL("Начальные"),
    REGULAR("Обычные"),
    FINAL("Завершающие")
}