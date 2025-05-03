package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX

/**
 * Сервис для проверки доступности финальных действий
 */
class FinalActionsValidator {

    /**
     * Проверяет, можно ли выполнить финальные действия.
     * Финальные действия можно выполнить только после всех обычных действий.
     *
     * @param task Задание, для которого проверяется доступность финальных действий
     * @return true, если финальные действия доступны, иначе false
     */
    fun canExecuteFinalActions(task: TaskX): Boolean {
        // Проверяем, что все обычные действия завершены или пропущены
        return task.plannedActions
            .filter { !it.isFinalAction }
            .all { it.isCompleted || it.isSkipped }
    }

    /**
     * Получает список доступных действий с учетом финальных действий
     *
     * @param task Задание
     * @param includeCompleted Включать ли завершенные действия
     * @param includeSkipped Включать ли пропущенные действия
     * @return Список доступных действий
     */
    fun getAvailableActions(
        task: TaskX,
        includeCompleted: Boolean = false,
        includeSkipped: Boolean = false
    ): List<String> {
        val canExecuteFinals = canExecuteFinalActions(task)

        return task.plannedActions
            .filter { action ->
                // Фильтруем по состоянию
                (action.isCompleted && includeCompleted) ||
                        (action.isSkipped && includeSkipped) ||
                        (!action.isCompleted && !action.isSkipped && (!action.isFinalAction || canExecuteFinals))
            }
            .map { it.id }
    }

    /**
     * Определяет следующее действие для выполнения в строгом порядке
     *
     * @param task Задание
     * @return ID следующего действия или null, если нет доступных действий
     */
    fun getNextActionIdInStrictOrder(task: TaskX): String? {
        val canExecuteFinals = canExecuteFinalActions(task)

        // Сначала ищем обычные действия
        val nextRegularAction = task.plannedActions
            .filter { !it.isFinalAction }
            .sortedBy { it.order }
            .firstOrNull { !it.isCompleted && !it.isSkipped }

        if (nextRegularAction != null) {
            return nextRegularAction.id
        }

        // Если все обычные действия выполнены и можно выполнять финальные,
        // ищем первое невыполненное финальное действие
        if (canExecuteFinals) {
            return task.plannedActions
                .filter { it.isFinalAction }
                .sortedBy { it.order }
                .firstOrNull { !it.isCompleted && !it.isSkipped }
                ?.id
        }

        return null
    }
}