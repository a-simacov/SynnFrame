package com.synngate.synnframe.domain.entity.taskx

import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.util.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class TaskX(
    val id: String,
    val barcode: String,                      // Штрихкод задания
    val name: String,                         // Имя задания
    val taskTypeId: String,                   // ID типа задания
    val executorId: String? = null,           // ID исполнителя
    val isVerified: Boolean = false,          // Верифицировано ли задание
    val status: TaskXStatus = TaskXStatus.TO_DO, // Статус задания
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,             // Дата и время создания
    @Serializable(with = LocalDateTimeSerializer::class)
    val startedAt: LocalDateTime? = null,     // Дата и время начала выполнения
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastModifiedAt: LocalDateTime? = null, // Дата и время последнего изменения
    @Serializable(with = LocalDateTimeSerializer::class)
    val completedAt: LocalDateTime? = null,   // Дата и время завершения
    val plannedActions: List<PlannedAction> = emptyList(), // Запланированные действия
    val factActions: List<FactAction> = emptyList(),       // Фактические действия
    val allowCompletionWithoutFactActions: Boolean = false // Разрешить завершение без факт. действий
) {
    // Можно ли начать выполнение задания
    fun canStart(): Boolean = status == TaskXStatus.TO_DO

    // Можно ли завершить задание
    fun canComplete(): Boolean {
        return status == TaskXStatus.IN_PROGRESS &&
                (factActions.isNotEmpty() || allowCompletionWithoutFactActions)
    }

    // Можно ли приостановить задание
    fun canPause(): Boolean = status == TaskXStatus.IN_PROGRESS

    // Можно ли возобновить задание
    fun canResume(): Boolean = status == TaskXStatus.PAUSED

    // Можно ли добавлять факт. действия
    fun canAddFactActions(): Boolean = status == TaskXStatus.IN_PROGRESS

    // Получить следующее запланированное действие
    fun getNextAction(): PlannedAction? {
        return plannedActions.firstOrNull { !it.isCompleted && !it.isSkipped }
    }

    /**
     * Получает факт. действия для конкретного планового действия
     * @param plannedActionId ID планового действия
     * @return список фактических действий
     */
    fun getFactActionsForPlannedAction(plannedActionId: String): List<FactAction> {
        return factActions.filter { it.plannedActionId == plannedActionId }
    }

    /**
     * Проверяет, частично ли выполнено действие
     * @param plannedActionId ID планового действия
     * @return true, если действие выполнено частично
     */
    fun isPlannedActionPartiallyCompleted(plannedActionId: String): Boolean {
        val plannedAction = plannedActions.find { it.id == plannedActionId } ?: return false
        return plannedAction.isPartiallyCompleted(factActions)
    }

    /**
     * Получает частично выполненные действия
     * @return список частично выполненных действий
     */
    fun getPartiallyCompletedActions(): List<PlannedAction> {
        return plannedActions.filter {
            !it.isCompleted && !it.isSkipped && it.isPartiallyCompleted(factActions)
        }
    }

    /**
     * Проверяет, можно ли продолжить выполнение частично выполненного действия
     * @param plannedActionId ID планового действия
     * @return true, если можно продолжить
     */
    fun canContinueAction(plannedActionId: String): Boolean {
        val plannedAction = plannedActions.find { it.id == plannedActionId } ?: return false

        // Проверяем, что действие не завершено, не пропущено и имеет связанные факт. действия
        return status == TaskXStatus.IN_PROGRESS &&
                !plannedAction.isCompleted &&
                !plannedAction.isSkipped &&
                getFactActionsForPlannedAction(plannedActionId).isNotEmpty()
    }

    /**
     * Рассчитывает общий прогресс выполнения задания
     * @return процент выполнения от 0.0 до 1.0
     */
    fun calculateTotalProgress(): Float {
        if (plannedActions.isEmpty()) return 0f

        val totalPlanned = plannedActions.sumOf {
            (if (it.isSkipped) 0f else it.plannedQuantity).toDouble()
        }.toFloat()

        if (totalPlanned <= 0) {
            // Если нет данных о плановом количестве, считаем по числу действий
            val total = plannedActions.size
            val completed = plannedActions.count { it.isCompleted || it.isSkipped }
            return if (total > 0) completed.toFloat() / total else 0f
        }

        // Суммируем фактически выполненные количества
        var completedQuantity = 0f
        plannedActions.forEach { action ->
            if (action.isCompleted) {
                // Для завершенных действий учитываем плановое количество
                completedQuantity += action.plannedQuantity
            } else if (!action.isSkipped) {
                // Для незавершенных действий учитываем фактически выполненное
                completedQuantity += action.getCompletedQuantity(factActions)
            }
        }

        return (completedQuantity / totalPlanned).coerceIn(0f, 1f)
    }
}