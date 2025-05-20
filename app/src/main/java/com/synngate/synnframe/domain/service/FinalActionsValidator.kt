package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction

class FinalActionsValidator(
    private val initialActionsValidator: InitialActionsValidator
) : ActionValidator() {

    sealed class ActionBlockReason {
        object InitialActionsNotCompleted : ActionBlockReason()
        object RegularActionsNotCompleted : ActionBlockReason()
        object OutOfOrder : ActionBlockReason()
        object None : ActionBlockReason()
    }

    override fun onCacheCleared() {
        initialActionsValidator.clearCache()
    }

    fun canExecuteFinalActions(task: TaskX): Boolean {
        val cacheKey = createCacheKey(task, "can_exec_final")

        return getCachedValue(cacheKey) {
            if (!initialActionsValidator.areInitialActionsCompleted(task)) {
                return@getCachedValue false
            }

            areAllActionsCompleted(getRegularActionsInternal(task))
        }
    }

    fun getNextActionIdInStrictOrder(task: TaskX): String? {
        val cacheKey = createCacheKey(task, "next_strict")

        return getCachedValue(cacheKey) {
            val nextInitialActionId = initialActionsValidator.getNextInitialActionId(task)
            if (nextInitialActionId != null) {
                return@getCachedValue nextInitialActionId
            }

            val regularActions = getRegularActionsInternal(task)
            val nextRegularAction = getNextIncompleteAction(regularActions)
            if (nextRegularAction != null) {
                return@getCachedValue nextRegularAction.id
            }

            if (canExecuteFinalActions(task)) {
                val finalActions = getFinalActionsInternal(task)
                getNextIncompleteAction(finalActions)?.id
            } else {
                null
            }
        }
    }

    fun getActionBlockReason(task: TaskX, actionId: String): ActionBlockReason {
        val cacheKey = createCacheKey(task, "block_reason", actionId)

        return getCachedValue(cacheKey) {
            val action = task.plannedActions.find { it.id == actionId }
                ?: return@getCachedValue ActionBlockReason.None

            if (action.isInitialAction) {
                return@getCachedValue ActionBlockReason.None
            }

            if (!initialActionsValidator.areInitialActionsCompleted(task)) {
                return@getCachedValue ActionBlockReason.InitialActionsNotCompleted
            }

            if (action.isFinalAction) {
                val regularActions = getRegularActionsInternal(task)
                if (!areAllActionsCompleted(regularActions)) {
                    return@getCachedValue ActionBlockReason.RegularActionsNotCompleted
                }
            }

            val nextActionId = getNextActionIdInStrictOrder(task)
            if (nextActionId != null && nextActionId != actionId) {
                return@getCachedValue ActionBlockReason.OutOfOrder
            }

            if (!checkStrictOrderExecutionInternal(task, actionId)) {
                return@getCachedValue ActionBlockReason.OutOfOrder
            }

            ActionBlockReason.None
        }
    }

    fun isInitialActionOutOfOrder(task: TaskX, actionId: String): Boolean {
        return initialActionsValidator.isInitialActionOutOfOrder(task, actionId)
    }

    private fun checkStrictOrderExecutionInternal(task: TaskX, actionId: String): Boolean {
        val action = task.plannedActions.find { it.id == actionId } ?: return true

        return task.plannedActions.all { otherAction ->
            otherAction.id == actionId ||
                    !action.canBeExecutedAfter(otherAction) ||
                    otherAction.isCompleted ||
                    otherAction.isSkipped
        }
    }

    private fun getRegularActionsInternal(task: TaskX): List<PlannedAction> {
        val cacheKey = createCacheKey(task, "regular_actions")

        return getCachedValue(cacheKey) {
            task.plannedActions.filter { !it.isFinalAction && !it.isInitialAction }
        }
    }

    private fun getFinalActionsInternal(task: TaskX): List<PlannedAction> {
        val cacheKey = createCacheKey(task, "final_actions")

        return getCachedValue(cacheKey) {
            task.plannedActions.filter { it.isFinalAction }
                .sortedBy { it.order }
        }
    }
}