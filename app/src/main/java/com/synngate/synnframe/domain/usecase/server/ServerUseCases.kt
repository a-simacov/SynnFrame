// Файл: com.synngate.synnframe.domain.usecase.server.ServerUseCases.kt

package com.synngate.synnframe.domain.usecase.server

import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.ServerRepository
import com.synngate.synnframe.domain.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Use Case класс для операций с серверами
 */
class ServerUseCases(
    private val serverRepository: ServerRepository,
    private val logRepository: LogRepository
) : BaseUseCase {

    // Базовые операции
    fun getServers(): Flow<List<Server>> =
        serverRepository.getServers()

    suspend fun getServerById(id: Int): Server? =
        serverRepository.getServerById(id)

    fun getActiveServer(): Flow<Server?> =
        serverRepository.getActiveServer()

    // Операции с бизнес-логикой
    suspend fun addServer(server: Server): Result<Long> {
        return try {
            // Валидация сервера
            validateServer(server)

            // Добавление сервера
            val id = serverRepository.addServer(server)
            logRepository.logInfo("Добавлен сервер: ${server.name} (${server.host}:${server.port})")

            Result.success(id)
        } catch (e: Exception) {
            Timber.e(e, "Exception during server addition")
            logRepository.logError("Ошибка при добавлении сервера: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateServer(server: Server): Result<Unit> {
        return try {
            // Валидация сервера
            validateServer(server)

            // Обновление сервера
            serverRepository.updateServer(server)
            logRepository.logInfo("Обновлен сервер: ${server.name} (${server.host}:${server.port})")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during server update")
            logRepository.logError("Ошибка при обновлении сервера: ${e.message}")
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
            logRepository.logInfo("Удален сервер: ${server.name} (${server.host}:${server.port})")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during server deletion")
            logRepository.logError("Ошибка при удалении сервера: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun setActiveServer(id: Int): Result<Unit> {
        return try {
            val server = serverRepository.getServerById(id)
            if (server == null) {
                return Result.failure(IllegalArgumentException("Server not found: $id"))
            }

            serverRepository.setActiveServer(id)
            logRepository.logInfo("Установлен активный сервер: ${server.name} (${server.host}:${server.port})")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during setting active server")
            logRepository.logError("Ошибка при установке активного сервера: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun clearActiveServer(): Result<Unit> {
        return try {
            serverRepository.clearActiveServer()
            logRepository.logInfo("Сброшен активный сервер")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during clearing active server")
            logRepository.logError("Ошибка при сбросе активного сервера: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun testConnection(server: Server): Result<Boolean> {
        return try {
            val result = serverRepository.testConnection(server)

            if (result.isSuccess) {
                logRepository.logInfo("Успешное подключение к серверу: ${server.name} (${server.host}:${server.port})")
            } else {
                logRepository.logWarning("Ошибка подключения к серверу: ${server.name} (${server.host}:${server.port}), причина: ${result.exceptionOrNull()?.message}")
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "Exception during server connection test")
            logRepository.logError("Исключение при тестировании подключения: ${e.message}")
            Result.failure(e)
        }
    }

    // Вспомогательные методы
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