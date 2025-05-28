package com.synngate.synnframe.presentation.ui.taskx.validator

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.presentation.ui.taskx.enums.CompletionOrderType
import com.synngate.synnframe.presentation.ui.taskx.enums.RegularActionsExecutionOrder

class ActionValidator {

    fun canExecuteAction(
        task: TaskX,
        actionId: String
    ): ValidationResult {
        val action = task.plannedActions.find { it.id == actionId }
            ?: return ValidationResult.Error("Действие не найдено")

        if (action.isFullyCompleted(task.factActions)) {
            val canOpenCompletedAction = action.canHaveMultipleFactActions() &&
                    action.isRegularAction() &&
                    task.taskType?.regularActionsExecutionOrder == RegularActionsExecutionOrder.ARBITRARY

            if (!canOpenCompletedAction) {
                return ValidationResult.Error("Действие уже выполнено")
            }
        }

        if (action.completionOrderType == CompletionOrderType.INITIAL) {
            val initialActions = task.getInitialActions()
            val notCompletedInitial = initialActions.filter {
                !it.isFullyCompleted(task.factActions)
            }

            if (notCompletedInitial.isNotEmpty() && notCompletedInitial.first().id != action.id) {
                val firstAction = notCompletedInitial.first()
                return ValidationResult.Error(
                    "Начальные действия должны выполняться в указанном порядке. " +
                            "Выполните сначала: ${firstAction.actionTemplate?.name ?: "Неизвестно"}"
                )
            }
        }
        else if (action.completionOrderType == CompletionOrderType.REGULAR) {
            if (!task.areInitialActionsCompleted()) {
                return ValidationResult.Error(
                    "Сначала необходимо выполнить все начальные действия"
                )
            }

            if (task.taskType?.regularActionsExecutionOrder == RegularActionsExecutionOrder.STRICT) {
                val regularActions = task.getRegularActions()
                val incompleteRegular = regularActions
                    .filter { !it.isFullyCompleted(task.factActions) }

                if (incompleteRegular.isNotEmpty() && incompleteRegular.first().id != action.id) {
                    val firstAction = incompleteRegular.first()
                    return ValidationResult.Error(
                        "Действия должны выполняться в указанном порядке. " +
                                "Выполните сначала: ${firstAction.actionTemplate?.name ?: "Неизвестно"}"
                    )
                }
            }
        }
        else if (action.completionOrderType == CompletionOrderType.FINAL) {
            if (!task.areInitialActionsCompleted()) {
                return ValidationResult.Error(
                    "Сначала необходимо выполнить все начальные действия"
                )
            }

            val regularActions = task.getRegularActions()
            val allRegularComplete = regularActions.all {
                it.isFullyCompleted(task.factActions)
            }

            if (!allRegularComplete) {
                return ValidationResult.Error(
                    "Сначала необходимо выполнить все обычные действия"
                )
            }

            val finalActions = task.getFinalActions()
            val incompleteFinal = finalActions
                .filter { !it.isFullyCompleted(task.factActions) }

            if (incompleteFinal.isNotEmpty() && incompleteFinal.first().id != action.id) {
                val firstAction = incompleteFinal.first()
                return ValidationResult.Error(
                    "Завершающие действия должны выполняться в указанном порядке. " +
                            "Выполните сначала: ${firstAction.actionTemplate?.name ?: "Неизвестно"}"
                )
            }
        }

        if (action.canHaveMultipleFactActions() &&
            action.isQuantityFulfilled(task.factActions) &&
            task.taskType?.regularActionsExecutionOrder == RegularActionsExecutionOrder.STRICT) {
            return ValidationResult.Error("План по количеству уже выполнен")
        }

        return ValidationResult.Success
    }

    fun canCompleteTask(task: TaskX): ValidationResult {
        val taskType = task.taskType
        if (taskType?.allowCompletionWithoutFactActions == true) {
            return ValidationResult.Success
        }

        val incompleteActions = task.plannedActions.filter { action ->
            !action.isSkipped && !action.isFullyCompleted(task.factActions)
        }

        if (incompleteActions.isNotEmpty()) {
            return ValidationResult.Error(
                "Не все действия выполнены. Осталось: ${incompleteActions.size}"
            )
        }

        return ValidationResult.Success
    }

    fun getNextAvailableAction(task: TaskX): PlannedAction? {
        val initialActions = task.getInitialActions()
        val incompleteInitialActions = initialActions.filter {
            !it.isFullyCompleted(task.factActions)
        }

        if (incompleteInitialActions.isNotEmpty()) {
            return incompleteInitialActions.minByOrNull { it.order }
        }

        val regularActions = task.getRegularActions()
            .filter { !it.isFullyCompleted(task.factActions) }

        if (regularActions.isNotEmpty()) {
            return if (task.taskType?.isStrictActionOrder() == true) {
                regularActions.minByOrNull { it.order }
            } else {
                regularActions.firstOrNull()
            }
        }

        val regularActionsCompleted = task.getRegularActions().all {
            it.isFullyCompleted(task.factActions)
        }

        if (regularActionsCompleted) {
            return task.getFinalActions()
                .filter { !it.isFullyCompleted(task.factActions) }
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