package com.synngate.synnframe.domain.entity

enum class LogType {
    INFO,
    WARNING,
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