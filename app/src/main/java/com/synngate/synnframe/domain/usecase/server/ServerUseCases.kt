// Файл: com.synngate.synnframe.domain.usecase.server.ServerUseCases.kt

package com.synngate.synnframe.domain.usecase.server

import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.domain.repository.ServerRepository
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.ServerCoordinator
import com.synngate.synnframe.domain.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Use Case класс для операций с серверами
 */
class ServerUseCases(
    private val serverRepository: ServerRepository,
    private val serverCoordinator: ServerCoordinator,
    private val loggingService: LoggingService
) : BaseUseCase {
    // Базовые операции без изменений
    fun getServers(): Flow<List<Server>> =
        serverRepository.getServers()

    suspend fun getServerById(id: Int): Server? =
        serverRepository.getServerById(id)

    fun getActiveServer(): Flow<Server?> =
        serverRepository.getActiveServer()

    // Операции с бизнес-логикой, валидация остается здесь
    suspend fun addServer(server: Server): Result<Long> {
        return try {
            // Валидация сервера
            validateServer(server)

            // Добавление сервера
            val id = serverRepository.addServer(server)
            loggingService.logInfo("Добавлен сервер: ${server.name} (${server.host}:${server.port})")

            Result.success(id)
        } catch (e: Exception) {
            Timber.e(e, "Exception during server addition")
            loggingService.logError("Ошибка при добавлении сервера: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateServer(server: Server): Result<Unit> {
        return try {
            // Валидация сервера
            validateServer(server)

            // Обновление сервера
            serverRepository.updateServer(server)
            loggingService.logInfo("Обновлен сервер: ${server.name} (${server.host}:${server.port})")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during server update")
            loggingService.logError("Ошибка при обновлении сервера: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteServer(id: Int): Result<Unit> {
        return try {
            val server = serverRepository.getServerById(id)
            if (server == null) {
                return Result.failure(IllegalArgumentException("Server not found: $id"))
            }

            serverRepository.deleteServer(id)
            loggingService.logInfo("Удален сервер: ${server.name} (${server.host}:${server.port})")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during server deletion")
            loggingService.logError("Ошибка при удалении сервера: ${e.message}")
            Result.failure(e)
        }
    }

    // Делегируем операции координатору серверов
    suspend fun setActiveServer(id: Int): Result<Unit> =
        serverCoordinator.switchActiveServer(id)

    suspend fun clearActiveServer(): Result<Unit> =
        serverCoordinator.clearActiveServer()

    suspend fun testConnection(server: Server): Result<Boolean> {
        // Валидируем сервер перед тестированием
        try {
            validateServer(server)
        } catch (e: Exception) {
            return Result.failure(e)
        }

        return serverCoordinator.testServerConnection(server)
    }

    // Вспомогательные методы валидации остаются без изменений
    private fun validateServer(server: Server) {
        if (server.name.isBlank()) {
            throw IllegalArgumentException("Server name cannot be empty")
        }

        if (server.host.isBlank()) {
            throw IllegalArgumentException("Server host cannot be empty")
        }

        if (server.port <= 0 || server.port > 65535) {
            throw IllegalArgumentException("Invalid server port: ${server.port}")
        }

        if (server.apiEndpoint.isBlank()) {
            throw IllegalArgumentException("API endpoint cannot be empty")
        }

        if (server.login.isBlank()) {
            throw IllegalArgumentException("Server login cannot be empty")
        }

        // Пароль может быть пустым в некоторых случаях
    }
}