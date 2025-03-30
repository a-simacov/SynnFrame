package com.synngate.synnframe.util.format

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// com.synngate.synnframe.util.format.DateFormatter.kt
object DateFormatter {
    private val defaultDateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    fun formatDateTime(dateTime: LocalDateTime): String {
        return dateTime.format(defaultDateTimeFormatter)
    }
}