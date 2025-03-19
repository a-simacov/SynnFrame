package com.synngate.synnframe.domain.service

import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс для управления локальным веб-сервером
 */
interface WebServerManager {
    /**
     * Поток с текущим состоянием сервера
     */
    val isRunning: Flow<Boolean>

    /**
     * Запуск веб-сервера
     * @return Result с успехом операции или ошибкой
     */
    suspend fun startServer(): Result<Unit>

    /**
     * Остановка веб-сервера
     * @return Result с успехом операции или ошибкой
     */
    suspend fun stopServer(): Result<Unit>

    /**
     * Переключение состояния сервера (запуск или остановка)
     * @return Result с новым состоянием (true - запущен, false - остановлен)
     */
    suspend fun toggleServer(): Result<Boolean>
}