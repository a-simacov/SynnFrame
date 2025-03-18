package com.synngate.synnframe.domain.entity

/**
 * Перечисление для моделей учета товара
 */
enum class AccountingModel {
    /**
     * По партиям и количеству
     */
    BATCH,

    /**
     * Только по количеству
     */
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