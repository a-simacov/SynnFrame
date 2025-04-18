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
    val finalActions: List<FactAction> = emptyList(),      // Финальные действия
    val allowCompletionWithoutFactActions: Boolean = false // Разрешить завершение без факт. действий
) {
    // Можно ли начать выполнение задания
    fun canStart(): Boolean = status == TaskXStatus.TO_DO

    // Можно ли завершить задание (изменена логика)
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
}