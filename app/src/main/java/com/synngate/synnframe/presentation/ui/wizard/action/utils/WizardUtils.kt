package com.synngate.synnframe.presentation.ui.wizard.action.utils

import java.util.Locale
import kotlin.math.round

object WizardUtils {

    fun formatQuantity(value: Float): String {
        return if (value % 1f == 0f) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
        }
    }

    fun roundToThreeDecimals(value: Float): Float {
        return (round(value * 1000) / 1000).toFloat()
    }

    fun parseQuantityInput(input: String): Float {
        return try {
            input.replace(",", ".").toFloatOrNull() ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

    fun processQuantityInput(input: String): String {
        return when {
            input.isEmpty() -> "0"
            input == "." -> "0."
            input.startsWith(".") -> "0$input"
            else -> input
        }
    }
}