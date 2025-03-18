package com.synngate.synnframe.domain.entity

/**
 * Перечисление для типов заданий
 */
enum class TaskType {
    /**
     * Приёмка товара
     */
    RECEIPT,

    /**
     * Отбор товара
     */
    PICK;

    companion object {
        fun fromString(value: String): TaskType {
            return when (value.uppercase()) {
                "RECEIPT", "ПРИЁМКА", "ПРИЕМКА" -> RECEIPT
                "PICK", "ОТБОР" -> PICK
                else -> RECEIPT
            }
        }
    }
}