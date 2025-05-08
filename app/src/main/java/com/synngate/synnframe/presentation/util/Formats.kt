package com.synngate.synnframe.presentation.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun formatQuantity(quantity: Float): String {
    return if (quantity == quantity.toInt().toFloat()) {
        quantity.toInt().toString()
    } else {
        "%.3f".format(quantity)
    }
}

fun formatDate(date: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    return date.format(formatter)
}