package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction

class InitialActionsValidator : ActionValidator() {

    fun areInitialActionsCompleted(task: TaskX): Boolean {
        val cacheKey = createCacheKey(task, "initial_completed")

        return getCachedValue(cacheKey) {
            val initialActions = getInitialActionsInternal(task)
            areAllActionsCompleted(initialActions)
        }
    }

    fun getNextInitialActionId(task: TaskX): String? {
        val cacheKey = createCacheKey(task, "next_initial")

        return getCachedValue(cacheKey) {
            val initialActions = getInitialActionsInternal(task)
            getNextIncompleteAction(initialActions)?.id
        }
    }

    fun hasInitialActions(task: TaskX): Boolean {
        val cacheKey = createCacheKey(task, "has_initial")

        return getCachedValue(cacheKey) {
            getInitialActionsInternal(task).isNotEmpty()
        }
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

    fun isInitialActionOutOfOrder(task: TaskX, actionId: String): Boolean {
        val cacheKey = createCacheKey(task, "out_of_order", actionId)

        return getCachedValue(cacheKey) {
            isActionOutOfOrder(task, actionId) { it.isInitialAction }
        }
    }

    private fun getInitialActionsInternal(task: TaskX): List<PlannedAction> {
        val cacheKey = createCacheKey(task, "initial_actions")

        return getCachedValue(cacheKey) {
            task.plannedActions.filter { it.isInitialAction }
        }
    }
}