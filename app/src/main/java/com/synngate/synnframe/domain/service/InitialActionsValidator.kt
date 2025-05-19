package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX

class InitialActionsValidator {

    private val completionCache = mutableMapOf<String, Boolean>()
    private val nextActionCache = mutableMapOf<String, String?>()

    fun areInitialActionsCompleted(task: TaskX): Boolean {
        val cacheKey = "${task.id}_${task.lastModifiedAt}"
        if (completionCache.containsKey(cacheKey)) {
            return completionCache[cacheKey]!!
        }

        val result = task.plannedActions
            .filter { it.isInitialAction }
            .all { it.isCompleted || it.isSkipped }

        completionCache[cacheKey] = result

        return result
    }

    fun getNextInitialActionId(task: TaskX): String? {
        val cacheKey = "${task.id}_${task.lastModifiedAt}"
        if (nextActionCache.containsKey(cacheKey)) {
            return nextActionCache[cacheKey]
        }

        val result = task.plannedActions
            .filter { it.isInitialAction && !it.isCompleted && !it.isSkipped }
            .minByOrNull { it.order }
            ?.id

        nextActionCache[cacheKey] = result

        return result
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

    fun clearCache() {
        completionCache.clear()
        nextActionCache.clear()
    }
}