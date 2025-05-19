package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction

/**
 * Базовый класс для валидаторов действий с общей логикой
 */
abstract class ActionValidator {

    // Кэш для хранения вычисленных результатов
    protected val cache = HashMap<String, Any?>()

    /**
     * Очищает все кэши валидатора
     */
    fun clearCache() {
        cache.clear()
        onCacheCleared()
    }

    /**
     * Хук для дополнительных действий при очистке кэша в наследниках
     */
    protected open fun onCacheCleared() {}

    /**
     * Получает значение из кэша или вычисляет его с помощью переданной функции
     */
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

    /**
     * Генерирует ключ для кэша на основе задания и опциональных дополнительных данных
     */
    protected fun createCacheKey(task: TaskX, prefix: String, vararg additionalData: Any?): String {
        val suffix = if (additionalData.isEmpty()) ""
        else "_" + additionalData.joinToString("_")
        return "${prefix}_${task.id}_${task.lastModifiedAt}$suffix"
    }

    /**
     * Проверяет, все ли действия выполнены или пропущены
     */
    protected fun areAllActionsCompleted(actions: List<PlannedAction>): Boolean {
        return actions.all { it.isCompleted || it.isSkipped }
    }

    /**
     * Получает следующее невыполненное действие в порядке сортировки
     */
    protected fun getNextIncompleteAction(actions: List<PlannedAction>): PlannedAction? {
        return actions
            .filter { !it.isCompleted && !it.isSkipped }
            .minByOrNull { it.order }
    }

    /**
     * Проверяет, нарушает ли указанное действие порядок выполнения
     */
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