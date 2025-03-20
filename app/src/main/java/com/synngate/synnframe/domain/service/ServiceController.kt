// File: com.synngate.synnframe.domain.service.ServiceController.kt

package com.synngate.synnframe.domain.service

import kotlinx.coroutines.flow.Flow

/**
 * Общий интерфейс для контроллеров сервисов
 */
interface ServiceController {
    /**
     * Текущее состояние сервиса (запущен или нет)
     */
    val isRunning: Flow<Boolean>

    /**
     * Запуск сервиса
     * @return Результат операции
     */
    suspend fun startService(): Result<Unit>

    /**
     * Остановка сервиса
     * @return Результат операции
     */
    suspend fun stopService(): Result<Unit>

    /**
     * Переключение состояния сервиса (запуск/остановка)
     * @return Новое состояние сервиса после переключения
     */
    suspend fun toggleService(): Result<Boolean>
}