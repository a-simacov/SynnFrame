package com.synngate.synnframe.domain.entity

import java.time.LocalDateTime

data class Log(
    val id: Int = 0,
    val message: String,
    val type: LogType,
    val createdAt: LocalDateTime
) {
    fun getShortMessage(maxLength: Int = 100): String {
        return if (message.length <= maxLength) {
            message
        } else {
            message.substring(0, maxLength) + "..."
        }
    }
}