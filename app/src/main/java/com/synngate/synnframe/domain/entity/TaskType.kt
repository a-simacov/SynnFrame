package com.synngate.synnframe.domain.entity

enum class TaskType {
    RECEIPT,
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