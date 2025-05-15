package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.TaskX

class FinalActionsValidator {

    fun canExecuteFinalActions(task: TaskX): Boolean {
        // Проверяем, что все обычные действия завершены или пропущены
        return task.plannedActions
            .filter { !it.isFinalAction }
            .all { it.isCompleted || it.isSkipped }
    }

    fun getNextActionIdInStrictOrder(task: TaskX): String? {
        val canExecuteFinals = canExecuteFinalActions(task)

        val nextRegularAction = task.plannedActions
            .filter { !it.isFinalAction }
            .sortedBy { it.order }
            .firstOrNull { !it.isCompleted && !it.isSkipped }

        if (nextRegularAction != null) {
            return nextRegularAction.id
        }

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