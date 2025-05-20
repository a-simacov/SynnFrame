package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction

abstract class ActionValidator {

    protected val cache = HashMap<String, Any?>()

    fun clearCache() {
        cache.clear()
        onCacheCleared()
    }

    protected open fun onCacheCleared() {}

    protected inline fun <T> getCachedValue(
        key: String,
        compute: () -> T
    ): T {
        @Suppress("UNCHECKED_CAST")
        return if (cache.containsKey(key)) {
            cache[key] as T
        } else {
            val result = compute()
            cache[key] = result
            result
        }
    }

    protected fun createCacheKey(task: TaskX, prefix: String, vararg additionalData: Any?): String {
        val suffix = if (additionalData.isEmpty()) ""
        else "_" + additionalData.joinToString("_")
        return "${prefix}_${task.id}_${task.lastModifiedAt}$suffix"
    }

    protected fun areAllActionsCompleted(actions: List<PlannedAction>): Boolean {
        return actions.all { it.isCompleted || it.isSkipped }
    }

    protected fun getNextIncompleteAction(actions: List<PlannedAction>): PlannedAction? {
        return actions
            .filter { !it.isCompleted && !it.isSkipped }
            .minByOrNull { it.order }
    }

    protected fun isActionOutOfOrder(
        task: TaskX,
        actionId: String,
        filter: (PlannedAction) -> Boolean
    ): Boolean {
        val action = task.plannedActions.find { it.id == actionId && filter(it) } ?: return false

        val firstIncompleteAction = task.plannedActions
            .filter { filter(it) && !it.isCompleted && !it.isSkipped }
            .minByOrNull { it.order }

        return firstIncompleteAction != null &&
                firstIncompleteAction.id != actionId &&
                action.order > firstIncompleteAction.order
    }
}