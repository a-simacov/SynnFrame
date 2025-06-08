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
                "PUT_INTO" -> PUT_INTO
                "TAKE_FROM" -> TAKE_FROM
                "RECEIPT" -> RECEIPT
                "EXPENSE" -> EXPENSE
                "RECOUNT" -> RECOUNT
                "USE" -> USE
                "ASSERT" -> ASSERT
                else -> PUT_INTO
            }
        }
    }
}