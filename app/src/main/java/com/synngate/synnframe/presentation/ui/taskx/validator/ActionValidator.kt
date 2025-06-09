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
            ?: return ValidationResult.Error("Action not found")

        if (action.isFullyCompleted(task.factActions)) {
            val canOpenCompletedAction = action.canHaveMultipleFactActions() &&
                    action.isRegularAction() &&
                    task.taskType?.regularActionsExecutionOrder == RegularActionsExecutionOrder.ARBITRARY

            if (!canOpenCompletedAction) {
                return ValidationResult.Error("Action already completed")
            }
        }

        return when (action.completionOrderType) {
            CompletionOrderType.INITIAL -> validateInitialAction(task, action)
            CompletionOrderType.REGULAR -> validateRegularAction(task, action)
            CompletionOrderType.FINAL -> validateFinalAction(task, action)
        }
    }

    private fun validateInitialAction(task: TaskX, action: PlannedAction): ValidationResult {
        val notCompletedInitial = task.getInitialActions()
            .filter { !it.isFullyCompleted(task.factActions) }

        if (notCompletedInitial.isNotEmpty() && notCompletedInitial.first().id != action.id) {
            val firstAction = notCompletedInitial.first()
            return ValidationResult.Error(
                "Initial actions must be performed in the specified order. " +
                        "Complete first: ${firstAction.actionTemplate?.name ?: "Unknown"}"
            )
        }

        return ValidationResult.Success
    }

    private fun validateRegularAction(task: TaskX, action: PlannedAction): ValidationResult {
        if (!task.areInitialActionsCompleted()) {
            return ValidationResult.Error("All initial actions must be completed first")
        }

        if (task.taskType?.regularActionsExecutionOrder == RegularActionsExecutionOrder.STRICT) {
            val incompleteRegular = task.getRegularActions()
                .filter { !it.isFullyCompleted(task.factActions) }

            if (incompleteRegular.isNotEmpty() && incompleteRegular.first().id != action.id) {
                val firstAction = incompleteRegular.first()
                return ValidationResult.Error(
                    "Actions must be performed in the specified order. " +
                            "Complete first: ${firstAction.actionTemplate?.name ?: "Unknown"}"
                )
            }
        }

        if (action.canHaveMultipleFactActions() &&
            action.isQuantityFulfilled(task.factActions) &&
            task.taskType?.regularActionsExecutionOrder == RegularActionsExecutionOrder.STRICT) {
            return ValidationResult.Error("Quantity plan is already fulfilled")
        }

        return ValidationResult.Success
    }

    private fun validateFinalAction(task: TaskX, action: PlannedAction): ValidationResult {
        if (!task.areInitialActionsCompleted()) {
            return ValidationResult.Error("All initial actions must be completed first")
        }

        val regularActions = task.getRegularActions()
        val allRegularComplete = regularActions.all { it.isFullyCompleted(task.factActions) }

        if (!allRegularComplete) {
            return ValidationResult.Error("All regular actions must be completed first")
        }

        val incompleteFinal = task.getFinalActions()
            .filter { !it.isFullyCompleted(task.factActions) }

        if (incompleteFinal.isNotEmpty() && incompleteFinal.first().id != action.id) {
            val firstAction = incompleteFinal.first()
            return ValidationResult.Error(
                "Final actions must be performed in the specified order. " +
                        "Complete first: ${firstAction.actionTemplate?.name ?: "Unknown"}"
            )
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
                "Not all actions are completed. Remaining: ${incompleteActions.size}"
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