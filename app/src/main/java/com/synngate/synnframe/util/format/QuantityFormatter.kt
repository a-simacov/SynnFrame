package com.synngate.synnframe.util.format

import java.util.Locale

object QuantityFormatter {
    fun format(quantity: Float): String {
        return if (quantity == quantity.toInt().toFloat()) {
            quantity.toInt().toString()
        } else {
            "%.3f".format(Locale.getDefault(), quantity)
        }
    }
}