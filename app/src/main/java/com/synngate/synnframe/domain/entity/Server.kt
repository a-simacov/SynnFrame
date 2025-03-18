package com.synngate.synnframe.domain.entity

/**
 * Доменная модель внешнего сервера
 */
data class Server(
    /**
     * Идентификатор (автогенерируемый)
     */
    val id: Int = 0,

    /**
     * Имя сервера
     */
    val name: String,

    /**
     * Хост сервера (IP адрес или доменное имя)
     */
    val host: String,

    /**
     * Порт сервера
     */
    val port: Int,

    /**
     * Точка подключения к API (например, /api)
     */
    val apiEndpoint: String,

    /**
     * Логин для доступа к API
     */
    val login: String,

    /**
     * Пароль для доступа к API
     */
    val password: String,

    /**
     * Признак активного сервера
     */
    val isActive: Boolean = false
) {
    /**
     * Полный URL-адрес API сервера
     */
    val apiUrl: String
        get() = "https://$host:$port$apiEndpoint"

    /**
     * URL для проверки доступности сервера
     */
    val echoUrl: String
        get() = "$apiUrl/echo"

    /**
     * Краткое представление сервера для отображения
     */
    fun getDisplayName(): String = "$name ($host:$port)"
}