package com.synngate.synnframe.domain.entity

/**
 * Перечисление для места создания сущности
 */
enum class CreationPlace {
    /**
     * Создано в приложении
     */
    APP,

    /**
     * Получено с сервера
     */
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