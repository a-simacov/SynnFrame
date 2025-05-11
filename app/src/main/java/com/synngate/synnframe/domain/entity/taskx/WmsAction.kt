package com.synngate.synnframe.domain.entity.taskx

enum class WmsAction {
    PUT_INTO,
    TAKE_FROM,
    RECEIPT,
    EXPENSE,
    RECOUNT,
    USE,
    ASSERT;

    companion object {
        fun fromString(value: String): WmsAction {
            return when (value.uppercase()) {
                "PUT_INTO", "ПОЛОЖИТЬ" -> PUT_INTO
                "TAKE_FROM", "ВЗЯТЬ" -> TAKE_FROM
                "RECEIPT", "ОПРИХОДОВАТЬ" -> RECEIPT
                "EXPENSE", "СПИСАТЬ" -> EXPENSE
                "RECOUNT", "ПЕРЕСЧИТАТЬ" -> RECOUNT
                "USE", "ИСПОЛЬЗОВАТЬ" -> USE
                "ASSERT", "ПОДТВЕРЖДЕНИЕ" -> ASSERT
                else -> PUT_INTO
            }
        }
    }
}