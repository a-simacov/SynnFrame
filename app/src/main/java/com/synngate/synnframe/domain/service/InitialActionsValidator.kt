package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX

class InitialActionsValidator {

    fun areInitialActionsCompleted(task: TaskX): Boolean {
        return task.plannedActions
            .filter { it.isInitialAction }
            .all { it.isCompleted || it.isSkipped }
    }

    fun getNextInitialActionId(task: TaskX): String? {
        return task.plannedActions
            .filter { it.isInitialAction }
            .sortedBy { it.order }
            .firstOrNull { !it.isCompleted && !it.isSkipped }
            ?.id
    }

    fun hasInitialActions(task: TaskX): Boolean {
        return task.plannedActions.any { it.isInitialAction }
    }

    fun getInitialActions(task: TaskX): List<String> {
        return task.plannedActions
            .filter { it.isInitialAction }
            .sortedBy { it.order }
            .map { it.id }
    }

    fun canExecuteRegularAction(task: TaskX, actionId: String): Boolean {
        val action = task.plannedActions.find { it.id == actionId }

        if (action?.isInitialAction == true) {
            return true
        }

        if (hasInitialActions(task)) {
            return areInitialActionsCompleted(task)
        }

        return true
    }
}