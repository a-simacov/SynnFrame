// File: com.synngate.synnframe.domain.service.WebServerController.kt

package com.synngate.synnframe.domain.service

import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс для управления локальным веб-сервером
 */
interface WebServerController : ServiceController {
    /**
     * Текущий хост сервера (например, "localhost" или IP-адрес устройства)
     */
    val serverHost: Flow<String>

    /**
     * Текущий порт сервера
     */
    val serverPort: Flow<Int>

    /**
     * Получение списка последних запросов к серверу
     */
    val lastRequests: Flow<List<RequestInfo>>

    /**
     * Очистка списка запросов
     */
    suspend fun clearRequestsLog()

    /**
     * Информация о запросе к серверу
     */
    data class RequestInfo(
        val timestamp: Long,
        val method: String,
        val path: String,
        val statusCode: Int,
        val responseTime: Long, // в миллисекундах
        val clientIp: String
    )
}