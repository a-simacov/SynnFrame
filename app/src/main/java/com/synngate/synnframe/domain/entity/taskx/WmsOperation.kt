package com.synngate.synnframe.domain.entity.taskx

enum class WmsOperation {
    RECEIPT,      // Приемка
    PLACEMENT,    // Размещение
    REPLENISHMENT,// Пополнение
    PICKING,      // Отбор
    MOVEMENT,     // Перемещение
    INVENTORY;    // Инвентаризация

    companion object {
        fun fromString(value: String): WmsOperation {
            return when (value.uppercase()) {
                "RECEIPT", "ПРИЕМКА" -> RECEIPT
                "PLACEMENT", "РАЗМЕЩЕНИЕ" -> PLACEMENT
                "REPLENISHMENT", "ПОПОЛНЕНИЕ" -> REPLENISHMENT
                "PICKING", "ОТБОР" -> PICKING
                "MOVEMENT", "ПЕРЕМЕЩЕНИЕ" -> MOVEMENT
                "INVENTORY", "ИНВЕНТАРИЗАЦИЯ" -> INVENTORY
                else -> RECEIPT
            }
        }
    }
}