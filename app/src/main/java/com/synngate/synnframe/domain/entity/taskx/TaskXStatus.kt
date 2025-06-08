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
                "TO_DO" -> TO_DO
                "IN_PROGRESS" -> IN_PROGRESS
                "PAUSED" -> PAUSED
                "COMPLETED" -> COMPLETED
                "CANCELLED" -> CANCELLED
                else -> TO_DO
            }
        }
    }
}