package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX

class FinalActionsValidator(
    private val initialActionsValidator: InitialActionsValidator
) {

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
}