package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX

class FinalActionsValidator(
    private val initialActionsValidator: InitialActionsValidator
) {

    sealed class ActionBlockReason {
        object InitialActionsNotCompleted : ActionBlockReason()
        object RegularActionsNotCompleted : ActionBlockReason()
        object OutOfOrder : ActionBlockReason()
        object None : ActionBlockReason()
    }

    fun canExecuteFinalActions(task: TaskX): Boolean {
        if (!initialActionsValidator.areInitialActionsCompleted(task)) {
            return false
        }

        return task.plannedActions
            .filter { !it.isFinalAction && !it.isInitialAction }
            .all { it.isCompleted || it.isSkipped }
    }

    fun getNextActionIdInStrictOrder(task: TaskX): String? {
        val nextInitialActionId = initialActionsValidator.getNextInitialActionId(task)
        if (nextInitialActionId != null) {
            return nextInitialActionId
        }

        val nextRegularAction = task.plannedActions
            .filter { !it.isFinalAction && !it.isInitialAction }
            .sortedBy { it.order }
            .firstOrNull { !it.isCompleted && !it.isSkipped }

        if (nextRegularAction != null) {
            return nextRegularAction.id
        }

        if (canExecuteFinalActions(task)) {
            return task.plannedActions
                .filter { it.isFinalAction }
                .sortedBy { it.order }
                .firstOrNull { !it.isCompleted && !it.isSkipped }
                ?.id
        }

        return null
    }

    fun getActionBlockReason(task: TaskX, actionId: String): ActionBlockReason {
        val action = task.plannedActions.find { it.id == actionId } ?: return ActionBlockReason.None

        if (action.isInitialAction) {
            return ActionBlockReason.None
        }

        if (!initialActionsValidator.areInitialActionsCompleted(task)) {
            return ActionBlockReason.InitialActionsNotCompleted
        }

        if (action.isFinalAction) {
            val allRegularCompleted = task.plannedActions
                .filter { !it.isFinalAction && !it.isInitialAction }
                .all { it.isCompleted || it.isSkipped }

            if (!allRegularCompleted) {
                return ActionBlockReason.RegularActionsNotCompleted
            }
        }

        val nextActionId = getNextActionIdInStrictOrder(task)
        if (nextActionId != null && nextActionId != actionId) {
            return ActionBlockReason.OutOfOrder
        }

        if (!checkStrictOrderExecution(task, actionId)) {
            return ActionBlockReason.OutOfOrder
        }

        return ActionBlockReason.None
    }

    private fun checkStrictOrderExecution(task: TaskX, actionId: String): Boolean {
        val action = task.plannedActions.find { it.id == actionId } ?: return true

        // Проверяем, что все действия, которые должны быть выполнены до этого,
        // уже выполнены или пропущены
        return task.plannedActions.all { otherAction ->
            otherAction.id == actionId ||
                    !action.canBeExecutedAfter(otherAction) ||
                    otherAction.isCompleted ||
                    otherAction.isSkipped
        }
    }

    fun isInitialActionOutOfOrder(task: TaskX, actionId: String): Boolean {
        val action = task.plannedActions.find { it.id == actionId } ?: return false
        if (!action.isInitialAction) return false

        // Находим первое не выполненное начальное действие
        val firstIncompleteInitialAction = task.plannedActions
            .filter { it.isInitialAction && !it.isCompleted && !it.isSkipped }
            .minByOrNull { it.order }

        // Если это не текущее действие, и у текущего действия order больше,
        // значит порядок нарушен
        return firstIncompleteInitialAction != null &&
                firstIncompleteInitialAction.id != actionId &&
                action.order > firstIncompleteInitialAction.order
    }
}