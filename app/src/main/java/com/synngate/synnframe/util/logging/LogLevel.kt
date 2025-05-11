package com.synngate.synnframe.util.logging

enum class LogLevel {
    FULL,
    INFO,
    WARNING,
    ERROR;

    companion object {
        fun fromString(value: String): LogLevel {
            return try {
                valueOf(value)
            } catch (e: Exception) {
                FULL
            }
        }
    }
}