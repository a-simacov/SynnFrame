package com.synngate.synnframe.presentation.ui.taskx.validator

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.ui.taskx.enums.CompletionOrderType
import com.synngate.synnframe.presentation.ui.taskx.enums.RegularActionsExecutionOrder

class ActionValidator {

    /**
     * Проверяет, может ли действие быть выполнено
     */
    fun canExecuteAction(
        task: TaskX,
        actionId: String
    ): ValidationResult {
        val action = task.plannedActions.find { it.id == actionId }
            ?: return ValidationResult.Error("Действие не найдено")

        // Проверка завершенности действия
        if (action.isCompleted || action.manuallyCompleted || action.isActionCompleted(task.factActions)) {
            // Разрешаем открытие уже выполненного действия только если:
            // 1. Для него установлен признак allowMultipleFactActions
            // 2. Это обычное действие (не начальное и не конечное)
            // 3. Порядок выполнения обычных действий произвольный
            val canOpenCompletedAction = action.canHaveMultipleFactActions() &&
                    action.isRegularAction() &&
                    task.taskType?.regularActionsExecutionOrder == RegularActionsExecutionOrder.ARBITRARY

            if (!canOpenCompletedAction) {
                return ValidationResult.Error("Действие уже выполнено")
            }
        }

        // Проверка на правильный порядок выполнения для НАЧАЛЬНЫХ действий
        if (action.completionOrderType == CompletionOrderType.INITIAL) {
            val initialActions = task.getInitialActions()
            val notCompletedInitial = initialActions.filter { !it.isCompleted && !it.manuallyCompleted }

            // Если это не первое незавершенное начальное действие
            if (notCompletedInitial.isNotEmpty() && notCompletedInitial.first().id != action.id) {
                val firstAction = notCompletedInitial.first()
                return ValidationResult.Error(
                    "Начальные действия должны выполняться в указанном порядке. " +
                            "Выполните сначала: ${firstAction.actionTemplate?.name ?: "Неизвестно"}"
                )
            }
        }
        // Проверка обычных действий - можно выполнять только если все начальные выполнены
        else if (action.completionOrderType == CompletionOrderType.REGULAR) {
            if (!task.areInitialActionsCompleted()) {
                return ValidationResult.Error(
                    "Сначала необходимо выполнить все начальные действия"
                )
            }

            // Проверка строгого порядка для обычных действий
            if (task.taskType?.regularActionsExecutionOrder == RegularActionsExecutionOrder.STRICT) {
                val regularActions = task.getRegularActions()
                val incompleteRegular = regularActions
                    .filter { !it.isCompleted && !it.manuallyCompleted }

                // Если это не первое незавершенное обычное действие
                if (incompleteRegular.isNotEmpty() && incompleteRegular.first().id != action.id) {
                    val firstAction = incompleteRegular.first()
                    return ValidationResult.Error(
                        "Действия должны выполняться в указанном порядке. " +
                                "Выполните сначала: ${firstAction.actionTemplate?.name ?: "Неизвестно"}"
                    )
                }
            }
        }
        // Проверка ФИНАЛЬНЫХ действий - можно выполнять только после всех обычных
        else if (action.completionOrderType == CompletionOrderType.FINAL) {
            // Проверка начальных действий
            if (!task.areInitialActionsCompleted()) {
                return ValidationResult.Error(
                    "Сначала необходимо выполнить все начальные действия"
                )
            }

            // Проверка обычных действий
            val regularActions = task.getRegularActions()
            val allRegularComplete = regularActions.all {
                it.isCompleted || it.manuallyCompleted || it.isActionCompleted(task.factActions)
            }

            if (!allRegularComplete) {
                return ValidationResult.Error(
                    "Сначала необходимо выполнить все обычные действия"
                )
            }

            // Проверка строгого порядка для финальных действий
            val finalActions = task.getFinalActions()
            val incompleteFinal = finalActions
                .filter { !it.isCompleted && !it.manuallyCompleted }

            // Если это не первое незавершенное финальное действие
            if (incompleteFinal.isNotEmpty() && incompleteFinal.first().id != action.id) {
                val firstAction = incompleteFinal.first()
                return ValidationResult.Error(
                    "Завершающие действия должны выполняться в указанном порядке. " +
                            "Выполните сначала: ${firstAction.actionTemplate?.name ?: "Неизвестно"}"
                )
            }
        }

        // Проверка количества для действий с множественным выполнением
        if (action.canHaveMultipleFactActions() &&
            action.isQuantityFulfilled(task.factActions)) {
            return ValidationResult.Error("План по количеству уже выполнен")
        }

        return ValidationResult.Success
    }

    /**
     * Проверяет, можно ли завершить задание
     */
    fun canCompleteTask(task: TaskX): ValidationResult {
        // Проверка разрешения завершения без фактических действий
        val taskType = task.taskType
        if (taskType?.allowCompletionWithoutFactActions == true) {
            return ValidationResult.Success
        }

        // Проверка выполнения всех обязательных действий
        val incompleteActions = task.plannedActions.filter { action ->
            !action.isSkipped &&
                    !action.isCompleted &&
                    !action.manuallyCompleted &&
                    !action.isActionCompleted(task.factActions)
        }

        if (incompleteActions.isNotEmpty()) {
            return ValidationResult.Error(
                "Не все действия выполнены. Осталось: ${incompleteActions.size}"
            )
        }

        return ValidationResult.Success
    }

    /**
     * Возвращает следующее доступное действие для выполнения
     */
    fun getNextAvailableAction(task: TaskX): PlannedAction? {
        // Сначала проверяем начальные действия
        if (!task.areInitialActionsCompleted()) {
            return task.getInitialActions()
                .filter { !it.isCompleted && !it.manuallyCompleted }
                .minByOrNull { it.order }
        }

        // Затем обычные действия
        val regularActions = task.getRegularActions()
            .filter { !it.isCompleted && !it.manuallyCompleted && !it.isActionCompleted(task.factActions) }

        if (regularActions.isNotEmpty()) {
            return if (task.taskType?.isStrictActionOrder() == true) {
                regularActions.minByOrNull { it.order }
            } else {
                regularActions.firstOrNull()
            }
        }

        // В конце финальные действия
        val allRegularCompleted = task.getRegularActions().all {
            it.isCompleted || it.manuallyCompleted || it.isActionCompleted(task.factActions)
        }

        if (allRegularCompleted) {
            return task.getFinalActions()
                .filter { !it.isCompleted && !it.manuallyCompleted }
                .minByOrNull { it.order }
        }

        return null
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()

    val isSuccess: Boolean
        get() = this is Success

    val errorMessage: String?
        get() = (this as? Error)?.message
}