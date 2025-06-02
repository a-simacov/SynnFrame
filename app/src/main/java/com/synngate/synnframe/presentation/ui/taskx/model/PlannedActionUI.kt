package com.synngate.synnframe.presentation.ui.taskx.model

import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction

/**
 * UI-модель для действия, содержащая дополнительную информацию для отображения
 */
data class PlannedActionUI(
    val action: PlannedAction,
    val isCompleted: Boolean,
    val completedQuantity: Float = 0f,
    val isClickable: Boolean = true,
    val manuallyCompleted: Boolean = false,
    val canBeCompletedManually: Boolean = false
) {
    companion object {
        /**
         * Создает UI-модель из доменной модели и рассчитывает статус выполненности
         */
        fun fromDomain(
            action: PlannedAction,
            factActions: List<FactAction>,
            isTaskInProgress: Boolean
        ): PlannedActionUI {
            // Определяем, выполнено ли действие, используя единый метод проверки
            val isCompleted = action.isFullyCompleted(factActions)

            // Рассчитываем количество для действий с множественными фактами
            val completedQuantity = if (action.canHaveMultipleFactActions()) {
                action.getCompletedQuantity(factActions)
            } else {
                0f
            }

            // Определяем, можно ли нажать на действие
            val isClickable = isTaskInProgress && (!isCompleted ||
                    (action.canHaveMultipleFactActions() && action.isRegularAction()))

            // Определяем, может ли действие быть выполнено вручную
            val canBeCompletedManually = isTaskInProgress &&
                    action.actionTemplate?.allowManualActionCompletion == true &&
                    !action.manuallyCompleted

            return PlannedActionUI(
                action = action,
                isCompleted = isCompleted,
                completedQuantity = completedQuantity,
                isClickable = isClickable,
                manuallyCompleted = action.manuallyCompleted,
                canBeCompletedManually = canBeCompletedManually
            )
        }
    }

    // Методы-прокси для удобного доступа к свойствам оригинального действия
    val id get() = action.id
    val name get() = action.actionTemplate?.name ?: "Действие #${action.order}"
    val order get() = action.order
    val isInitialAction get() = action.isInitialAction()
    val isFinalAction get() = action.isFinalAction()
    val canHaveMultipleFactActions get() = action.canHaveMultipleFactActions()
    val quantity get() = action.quantity
}