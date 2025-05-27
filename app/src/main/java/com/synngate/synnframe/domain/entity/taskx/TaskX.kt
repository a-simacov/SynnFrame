package com.synngate.synnframe.domain.entity.taskx

import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.util.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class TaskX(
    val id: String,
    val barcode: String,
    val name: String,
    val taskType: TaskTypeX? = null, // Заполняется из ответа сервера
    val executorId: String? = null,
    val status: TaskXStatus = TaskXStatus.TO_DO,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val startedAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastModifiedAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val completedAt: LocalDateTime? = null,
    val plannedActions: List<PlannedAction> = emptyList(),
    val factActions: List<FactAction> = emptyList()
) {
    fun getInitialActions(): List<PlannedAction> =
        plannedActions.filter { it.isInitialAction() }

    fun getRegularActions(): List<PlannedAction> =
        plannedActions.filter { it.isRegularAction() }

    fun getFinalActions(): List<PlannedAction> =
        plannedActions.filter { it.isFinalAction() }

    /**
     * Проверяет, завершены ли все начальные действия
     * Использует метод isFullyCompleted для единой логики проверки
     */
    fun areInitialActionsCompleted(): Boolean =
        getInitialActions().all { it.isFullyCompleted(factActions) }

    fun getCompletedInitialActionsCount(): Int =
        getInitialActions().count { it.isFullyCompleted(factActions) }

    fun getTotalInitialActionsCount(): Int =
        getInitialActions().size
}