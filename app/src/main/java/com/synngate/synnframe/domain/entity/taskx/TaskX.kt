// Задание X (TaskX)
package com.synngate.synnframe.domain.entity.taskx

import com.synngate.synnframe.domain.entity.User
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
    val planLines: List<PlanLineX> = emptyList(), // Строки плана
    val factLines: List<FactLineX> = emptyList(), // Строки факта
    val finalFactLine: FactLineX? = null       // Финальная строка факта
) {
    fun getTaskType(): TaskTypeX? = null // Будет реализовано в репозитории

    fun getExecutor(): User? = null // Будет реализовано в репозитории

    // Можно ли начать выполнение задания
    fun canStart(): Boolean = status == TaskXStatus.TO_DO

    // Можно ли завершить задание
    fun canComplete(): Boolean {
        return status == TaskXStatus.IN_PROGRESS &&
                (factLines.isNotEmpty() || getTaskType()?.allowCompletionWithoutFactLines == true)
    }

    // Можно ли приостановить задание
    fun canPause(): Boolean = status == TaskXStatus.IN_PROGRESS

    // Можно ли возобновить задание
    fun canResume(): Boolean = status == TaskXStatus.PAUSED

    // Можно ли добавлять строки факта
    fun canAddFactLines(): Boolean = status == TaskXStatus.IN_PROGRESS
}