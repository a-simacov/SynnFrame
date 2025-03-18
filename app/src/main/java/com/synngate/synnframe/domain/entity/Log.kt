package com.synngate.synnframe.domain.entity

import java.time.LocalDateTime

/**
 * Доменная модель лога
 */
data class Log(
    /**
     * Идентификатор лога (автогенерируемый)
     */
    val id: Int = 0,

    /**
     * Сообщение лога
     */
    val message: String,

    /**
     * Тип лога (INFO, WARNING, ERROR)
     */
    val type: LogType,

    /**
     * Дата и время создания
     */
    val createdAt: LocalDateTime
) {
    /**
     * Получение короткого сообщения (первые 100 символов)
     */
    fun getShortMessage(maxLength: Int = 100): String {
        return if (message.length <= maxLength) {
            message
        } else {
            message.substring(0, maxLength) + "..."
        }
    }
}