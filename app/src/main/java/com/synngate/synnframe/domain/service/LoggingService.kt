// Файл: com.synngate.synnframe.domain.service.LoggingService.kt

package com.synngate.synnframe.domain.service

/**
 * Сервис для логирования в приложении
 */
interface LoggingService {
    /**
     * Логирование информационного сообщения
     */
    suspend fun logInfo(message: String): Long

    /**
     * Логирование предупреждения
     */
    suspend fun logWarning(message: String): Long

    /**
     * Логирование ошибки
     */
    suspend fun logError(message: String): Long
}