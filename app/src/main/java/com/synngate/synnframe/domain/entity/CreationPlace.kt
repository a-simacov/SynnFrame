package com.synngate.synnframe.domain.entity

enum class CreationPlace {
    APP,
    SERVER;

    companion object {
        fun fromString(value: String): CreationPlace {
            return when (value.uppercase()) {
                "APP", "ПРИЛОЖЕНИЕ" -> APP
                "SERVER", "СЕРВЕР" -> SERVER
                else -> APP
            }
        }
    }
}