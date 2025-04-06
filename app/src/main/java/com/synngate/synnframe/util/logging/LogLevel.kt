package com.synngate.synnframe.util.logging

/**
 * Уровни логирования в приложении
 */
enum class LogLevel {
    FULL,      // Все логи (включая Debug)
    INFO,      // Информация, предупреждения и ошибки
    WARNING,   // Предупреждения и ошибки
    ERROR;     // Только ошибки

    companion object {
        fun fromString(value: String): LogLevel {
            return try {
                valueOf(value)
            } catch (e: Exception) {
                FULL // По умолчанию полное логирование
            }
        }
    }
}