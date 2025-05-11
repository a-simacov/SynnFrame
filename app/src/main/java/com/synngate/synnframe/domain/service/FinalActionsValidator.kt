package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX

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