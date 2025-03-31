package com.synngate.synnframe.domain.entity

enum class AccountingModel {
    BATCH,
    QTY;

    companion object {
        fun fromString(value: String): AccountingModel {
            return when (value.uppercase()) {
                "BATCH", "ПО ПАРТИЯМ" -> BATCH
                "QTY", "QUANTITY", "ПО КОЛИЧЕСТВУ" -> QTY
                else -> QTY
            }
        }
    }
}