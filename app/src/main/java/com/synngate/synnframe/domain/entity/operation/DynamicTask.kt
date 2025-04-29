package com.synngate.synnframe.domain.entity.operation

import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import kotlinx.serialization.Serializable

@Serializable
data class DynamicTask(
    val id: String,
    val name: String,
    val status: String = "",
    val executorId: String? = null
) {
    fun getTaskStatus(): TaskXStatus {
        return when (status.uppercase()) {
            "TO_DO", "К ВЫПОЛНЕНИЮ" -> TaskXStatus.TO_DO
            "IN_PROGRESS", "ВЫПОЛНЯЕТСЯ" -> TaskXStatus.IN_PROGRESS
            "PAUSED", "ПРИОСТАНОВЛЕНО" -> TaskXStatus.PAUSED
            "COMPLETED", "ЗАВЕРШЕНО" -> TaskXStatus.COMPLETED
            "CANCELLED", "ОТМЕНЕНО" -> TaskXStatus.CANCELLED
            else -> TaskXStatus.TO_DO
        }
    }
}