package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.Server
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с серверами
 */
interface ServerRepository {
    /**
     * Получение списка всех серверов
     */
    fun getServers(): Flow<List<Server>>

    /**
     * Получение сервера по идентификатору
     */
    suspend fun getServerById(id: Int): Server?

    /**
     * Получение активного сервера
     */
    fun getActiveServer(): Flow<Server?>

    /**
     * Добавление нового сервера
     */
    suspend fun addServer(server: Server): Long

    /**
     * Обновление существующего сервера
     */
    suspend fun updateServer(server: Server)

    /**
     * Удаление сервера
     */
    suspend fun deleteServer(id: Int)

    /**
     * Установка сервера в качестве активного
     */
    suspend fun setActiveServer(id: Int)

    /**
     * Очистка активного сервера
     */
    //suspend fun clearActiveServer()

    suspend fun clearActiveStatus()

    /**
     * Проверка соединения с сервером
     */
    suspend fun testConnection(server: Server): Result<Boolean>
}