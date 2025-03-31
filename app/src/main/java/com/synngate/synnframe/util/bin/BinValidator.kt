package com.synngate.synnframe.util.bin

import timber.log.Timber

class BinValidator(private val pattern: String) {
    private val regex: Regex

    init {
        regex = try {
            Regex(pattern)
        } catch (e: Exception) {
            Timber.e(e, "Invalid bin pattern: $pattern")
            Regex(".*") // По умолчанию пропускаем любой код
        }
    }

    fun isValidBin(code: String): Boolean {
        return regex.matches(code)
    }
}