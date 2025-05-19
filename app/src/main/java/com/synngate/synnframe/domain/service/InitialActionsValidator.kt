package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction

/**
 * Валидатор начальных действий с улучшенным кэшированием и производительностью
 */
class InitialActionsValidator : ActionValidator() {

    /**
     * Проверяет, выполнены ли все начальные действия
     */
    fun areInitialActionsCompleted(task: TaskX): Boolean {
        val cacheKey = createCacheKey(task, "initial_completed")

        return getCachedValue(cacheKey) {
            // Получаем все начальные действия и проверяем их статус
            val initialActions = getInitialActionsInternal(task)
            areAllActionsCompleted(initialActions)
        }
    }

    /**
     * Получает ID следующего невыполненного начального действия
     */
    fun getNextInitialActionId(task: TaskX): String? {
        val cacheKey = createCacheKey(task, "next_initial")

        return getCachedValue(cacheKey) {
            val initialActions = getInitialActionsInternal(task)
            getNextIncompleteAction(initialActions)?.id
        }
    }

    /**
     * Проверяет, содержит ли задание начальные действия
     */
    fun hasInitialActions(task: TaskX): Boolean {
        val cacheKey = createCacheKey(task, "has_initial")

        return getCachedValue(cacheKey) {
            getInitialActionsInternal(task).isNotEmpty()
        }
    }

    /**
     * Получает список ID всех начальных действий в порядке их выполнения
     */
    fun getInitialActions(task: TaskX): List<String> {
        val cacheKey = createCacheKey(task, "initial_ids")

        return getCachedValue(cacheKey) {
            getInitialActionsInternal(task)
                .sortedBy { it.order }
                .map { it.id }
        }
    }

    /**
     * Проверяет, можно ли выполнить обычное действие
     */
    fun canExecuteRegularAction(task: TaskX, actionId: String): Boolean {
        // Начальные действия всегда можно выполнять
        val action = task.plannedActions.find { it.id == actionId }
        if (action?.isInitialAction == true) {
            return true
        }

        // Если есть начальные действия, все они должны быть выполнены
        if (hasInitialActions(task)) {
            return areInitialActionsCompleted(task)
        }

        return true
    }

    /**
     * Проверяет, нарушает ли начальное действие порядок выполнения
     */
    fun isInitialActionOutOfOrder(task: TaskX, actionId: String): Boolean {
        val cacheKey = createCacheKey(task, "out_of_order", actionId)

        return getCachedValue(cacheKey) {
            isActionOutOfOrder(task, actionId) { it.isInitialAction }
        }
    }

    /**
     * Получает количество выполненных начальных действий
     */
    fun getCompletedInitialActionsCount(task: TaskX): Int {
        val cacheKey = createCacheKey(task, "completed_count")

        return getCachedValue(cacheKey) {
            getInitialActionsInternal(task)
                .count { it.isCompleted }
        }
    }

    /**
     * Получает общее количество начальных действий
     */
    fun getTotalInitialActionsCount(task: TaskX): Int {
        val cacheKey = createCacheKey(task, "total_count")

        return getCachedValue(cacheKey) {
            getInitialActionsInternal(task).size
        }
    }

    /**
     * Внутренний метод для получения всех начальных действий
     * с кэшированием списка, чтобы не фильтровать его многократно
     */
    private fun getInitialActionsInternal(task: TaskX): List<PlannedAction> {
        val cacheKey = createCacheKey(task, "initial_actions")

        return getCachedValue(cacheKey) {
            task.plannedActions.filter { it.isInitialAction }
        }
    }
}