package com.synngate.synnframe.domain.entity

enum class LogType {
    INFO,
    WARNING,
    ERROR;

    companion object {
        fun fromString(value: String): LogType {
            return when (value.uppercase()) {
                "INFO" -> INFO
                "WARNING" -> WARNING
                "ERROR" -> ERROR
                else -> INFO
            }
        }
    }
}