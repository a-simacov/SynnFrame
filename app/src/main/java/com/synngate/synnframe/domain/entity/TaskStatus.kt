package com.synngate.synnframe.domain.entity

/**
 * Перечисление для статусов выполнения задания
 */
enum class TaskStatus {
    /**
     * К выполнению - задание создано, но еще не начато
     */
    TO_DO,

    /**
     * Выполняется - задание в процессе выполнения
     */
    IN_PROGRESS,

    /**
     * Выполнено - задание завершено
     */
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