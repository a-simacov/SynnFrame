package com.synngate.synnframe.domain.entity

enum class TaskStatus {
    TO_DO,
    IN_PROGRESS,
    COMPLETED;

    companion object {
        fun fromString(value: String): TaskStatus {
            return when (value.uppercase()) {
                "TO_DO", "К ВЫПОЛНЕНИЮ" -> TO_DO
                "IN_PROGRESS", "ВЫПОЛНЯЕТСЯ" -> IN_PROGRESS
                "COMPLETED", "ВЫПОЛНЕНО" -> COMPLETED
                else -> TO_DO
            }
        }
    }
}