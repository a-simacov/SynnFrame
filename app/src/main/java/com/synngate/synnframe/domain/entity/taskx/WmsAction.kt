package com.synngate.synnframe.domain.entity.taskx

enum class WmsAction {
    PUT_INTO,     // Положить
    TAKE_FROM,    // Взять
    RECEIPT,      // Оприходовать
    EXPENSE,      // Списать
    RECOUNT,      // Пересчитать
    USE;          // Использовать

    companion object {
        fun fromString(value: String): WmsAction {
            return when (value.uppercase()) {
                "PUT_INTO", "ПОЛОЖИТЬ" -> PUT_INTO
                "TAKE_FROM", "ВЗЯТЬ" -> TAKE_FROM
                "RECEIPT", "ОПРИХОДОВАТЬ" -> RECEIPT
                "EXPENSE", "СПИСАТЬ" -> EXPENSE
                "RECOUNT", "ПЕРЕСЧИТАТЬ" -> RECOUNT
                "USE", "ИСПОЛЬЗОВАТЬ" -> USE
                else -> PUT_INTO
            }
        }
    }
}