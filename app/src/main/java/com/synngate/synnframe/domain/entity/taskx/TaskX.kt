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
    val taskTypeId: String,
    val executorId: String? = null,
    val isVerified: Boolean = false,
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
    val factActions: List<FactAction> = emptyList(),
    val allowCompletionWithoutFactActions: Boolean = false
) {

    fun getNextAction(): PlannedAction? {
        return plannedActions.firstOrNull { !it.isCompleted && !it.isSkipped }
    }
}