package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction

/**
 * Валидатор финальных действий с улучшенным кэшированием и производительностью
 */
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
        // При очистке нашего кэша очищаем также кэш начальных действий
        initialActionsValidator.clearCache()
    }

    /**
     * Проверяет, можно ли выполнить финальные действия
     */
    fun canExecuteFinalActions(task: TaskX): Boolean {
        val cacheKey = createCacheKey(task, "can_exec_final")

        return getCachedValue(cacheKey) {
            // Сначала должны быть выполнены все начальные действия
            if (!initialActionsValidator.areInitialActionsCompleted(task)) {
                return@getCachedValue false
            }

            // Затем все обычные действия
            areAllActionsCompleted(getRegularActionsInternal(task))
        }
    }

    /**
     * Получает ID следующего действия в строгом порядке выполнения
     */
    fun getNextActionIdInStrictOrder(task: TaskX): String? {
        val cacheKey = createCacheKey(task, "next_strict")

        return getCachedValue(cacheKey) {
            // Сначала проверяем начальные действия
            val nextInitialActionId = initialActionsValidator.getNextInitialActionId(task)
            if (nextInitialActionId != null) {
                return@getCachedValue nextInitialActionId
            }

            // Затем обычные действия
            val regularActions = getRegularActionsInternal(task)
            val nextRegularAction = getNextIncompleteAction(regularActions)
            if (nextRegularAction != null) {
                return@getCachedValue nextRegularAction.id
            }

            // Если можно выполнять финальные действия, находим следующее из них
            if (canExecuteFinalActions(task)) {
                val finalActions = getFinalActionsInternal(task)
                getNextIncompleteAction(finalActions)?.id
            } else {
                null
            }
        }
    }

    /**
     * Определяет причину блокировки действия, если оно не может быть выполнено
     */
    fun getActionBlockReason(task: TaskX, actionId: String): ActionBlockReason {
        val cacheKey = createCacheKey(task, "block_reason", actionId)

        return getCachedValue(cacheKey) {
            val action = task.plannedActions.find { it.id == actionId }
                ?: return@getCachedValue ActionBlockReason.None

            // Начальные действия всегда можно выполнять
            if (action.isInitialAction) {
                return@getCachedValue ActionBlockReason.None
            }

            // Проверяем выполнение начальных действий
            if (!initialActionsValidator.areInitialActionsCompleted(task)) {
                return@getCachedValue ActionBlockReason.InitialActionsNotCompleted
            }

            // Для финальных действий проверяем, выполнены ли все обычные
            if (action.isFinalAction) {
                val regularActions = getRegularActionsInternal(task)
                if (!areAllActionsCompleted(regularActions)) {
                    return@getCachedValue ActionBlockReason.RegularActionsNotCompleted
                }
            }

            // Проверяем строгий порядок выполнения
            val nextActionId = getNextActionIdInStrictOrder(task)
            if (nextActionId != null && nextActionId != actionId) {
                return@getCachedValue ActionBlockReason.OutOfOrder
            }

            // Проверяем, что все предшествующие действия выполнены
            if (!checkStrictOrderExecutionInternal(task, actionId)) {
                return@getCachedValue ActionBlockReason.OutOfOrder
            }

            ActionBlockReason.None
        }
    }

    /**
     * Проверяет, нарушает ли начальное действие порядок выполнения
     */
    fun isInitialActionOutOfOrder(task: TaskX, actionId: String): Boolean {
        return initialActionsValidator.isInitialActionOutOfOrder(task, actionId)
    }

    /**
     * Внутренний метод для проверки строгого порядка выполнения
     */
    private fun checkStrictOrderExecutionInternal(task: TaskX, actionId: String): Boolean {
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

    /**
     * Внутренний метод для получения всех обычных действий
     */
    private fun getRegularActionsInternal(task: TaskX): List<PlannedAction> {
        val cacheKey = createCacheKey(task, "regular_actions")

        return getCachedValue(cacheKey) {
            task.plannedActions.filter { !it.isFinalAction && !it.isInitialAction }
        }
    }

    /**
     * Внутренний метод для получения всех финальных действий
     */
    private fun getFinalActionsInternal(task: TaskX): List<PlannedAction> {
        val cacheKey = createCacheKey(task, "final_actions")

        return getCachedValue(cacheKey) {
            task.plannedActions.filter { it.isFinalAction }
                .sortedBy { it.order }
        }
    }
}