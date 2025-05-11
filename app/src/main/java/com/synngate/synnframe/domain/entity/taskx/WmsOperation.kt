package com.synngate.synnframe.domain.entity.taskx

enum class WmsOperation {
    RECEIPT,
    PUTAWAY,
    REPLENISHMENT,
    PICKING,
    MOVEMENT,
    PACKING,
    VERIFICATION,
    SHIPMENT,
    RECALCULATION;

    companion object {
        fun fromString(value: String): WmsOperation {
            return when (value.uppercase()) {
                "RECEIPT", "ПРИЕМКА" -> RECEIPT
                "PUTAWAY", "РАЗМЕЩЕНИЕ" -> PUTAWAY
                "REPLENISHMENT", "ПОПОЛНЕНИЕ" -> REPLENISHMENT
                "PICKING", "ОТБОР" -> PICKING
                "MOVEMENT", "ПЕРЕМЕЩЕНИЕ" -> MOVEMENT
                "RECALCULATION", "ИНВЕНТАРИЗАЦИЯ" -> RECALCULATION
                "PACKING" -> PACKING
                "VERIFICATION" -> VERIFICATION
                "SHIPMENT" -> SHIPMENT
                else -> RECEIPT
            }
        }
    }
}