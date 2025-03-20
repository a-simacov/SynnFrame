package com.synngate.synnframe.presentation.util

/**
 * Форматирует число с плавающей точкой для отображения
 */
fun formatQuantity(quantity: Float): String {
    return if (quantity == quantity.toInt().toFloat()) {
        quantity.toInt().toString()
    } else {
        "%.3f".format(quantity)
    }
}