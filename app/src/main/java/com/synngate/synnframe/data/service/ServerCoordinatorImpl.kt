package com.synngate.synnframe.data.service

import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.domain.repository.ServerRepository
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.ServerCoordinator
import java.io.IOException

class ServerCoordinatorImpl(
    private val serverRepository: ServerRepository,
    private val appSettingsDataStore: AppSettingsDataStore,
    private val loggingService: LoggingService
) : ServerCoordinator {

    override suspend fun switchActiveServer(id: Int): Result<Unit> {
        return try {
            // Получаем информацию о сервере
            val server = serverRepository.getServerById(id)
            if (server == null) {
                loggingService.logWarning("Попытка активации несуществующего сервера: $id")
                return Result.failure(IllegalArgumentException("Server not found: $id"))
            }

            // Обновляем статус в базе данных
            serverRepository.setActiveServer(id)

            // Обновляем настройки
            appSettingsDataStore.setActiveServer(id, server.name)

            // Логируем изменение
            loggingService.logInfo("Установлен активный сервер: ${server.name} (${server.host}:${server.port})")

            Result.success(Unit)
        } catch (e: Exception) {
            loggingService.logError("Ошибка при установке активного сервера: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun clearActiveServer(): Result<Unit> {
        return try {
            // Очищаем статус в базе данных
            serverRepository.clearActiveStatus()

            // Очищаем настройки
            appSettingsDataStore.clearActiveServer()

            // Логируем изменение
            loggingService.logInfo("Сброшен активный сервер")

            Result.success(Unit)
        } catch (e: Exception) {
            loggingService.logError("Ошибка при сбросе активного сервера: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun testServerConnection(server: Server): Result<Boolean> {
        return try {
            val response = serverRepository.testConnection(server)

            when (response) {
                is ApiResult.Success -> {
                    loggingService.logInfo("Успешное подключение к серверу: ${server.name} (${server.host}:${server.port})")
                    Result.success(true)
                }
                is ApiResult.Error -> {
                    loggingService.logWarning("Ошибка подключения к серверу: ${server.name} (${server.host}:${server.port}), причина: ${response.message}")
                    Result.failure(IOException("Connection test failed with code: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            loggingService.logError("Исключение при тестировании подключения: ${e.message}")
            Result.failure(e)
        }
    }
}