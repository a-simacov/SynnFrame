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
                "RECEIPT" -> RECEIPT
                "PUTAWAY" -> PUTAWAY
                "REPLENISHMENT" -> REPLENISHMENT
                "PICKING" -> PICKING
                "MOVEMENT" -> MOVEMENT
                "RECALCULATION" -> RECALCULATION
                "PACKING" -> PACKING
                "VERIFICATION" -> VERIFICATION
                "SHIPMENT" -> SHIPMENT
                else -> RECEIPT
            }
        }
    }
}