package com.synngate.synnframe.domain.entity.taskx

enum class TaskXStatus {
    TO_DO,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    CANCELLED;

    companion object {
        fun fromString(value: String): TaskXStatus {
            return when (value.uppercase()) {
                "TO_DO", "К ВЫПОЛНЕНИЮ" -> TO_DO
                "IN_PROGRESS", "ВЫПОЛНЯЕТСЯ" -> IN_PROGRESS
                "PAUSED", "ПРИОСТАНОВЛЕНО" -> PAUSED
                "COMPLETED", "ЗАВЕРШЕНО" -> COMPLETED
                "CANCELLED", "ОТМЕНЕНО" -> CANCELLED
                else -> TO_DO
            }
        }
    }
}