package com.synngate.synnframe.domain.entity

/**
 * Перечисление для типов логов
 */
enum class LogType {
    /**
     * Информационный лог
     */
    INFO,

    /**
     * Предупреждение
     */
    WARNING,

    /**
     * Ошибка
     */
    ERROR;

    companion object {
        fun fromString(value: String): LogType {
            return when (value.uppercase()) {
                "INFO", "ИНФОРМАЦИЯ" -> INFO
                "WARNING", "WARN", "ПРЕДУПРЕЖДЕНИЕ" -> WARNING
                "ERROR", "ERR", "ОШИБКА" -> ERROR
                else -> INFO
            }
        }
    }
}