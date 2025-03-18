package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.data.local.dao.ServerDao
import com.synngate.synnframe.data.local.entity.ServerEntity
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.service.ApiService
import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException

/**
 * Имплементация репозитория серверов
 */
class ServerRepositoryImpl(
    private val serverDao: ServerDao,
    private val apiService: ApiService,
    private val appSettingsDataStore: AppSettingsDataStore,
    private val logRepository: LogRepository
) : ServerRepository {

    override fun getServers(): Flow<List<Server>> {
        return serverDao.getAllServers().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getServerById(id: Int): Server? {
        return serverDao.getServerById(id)?.toDomainModel()
    }

    override fun getActiveServer(): Flow<Server?> {
        return serverDao.getActiveServer().map { entity ->
            entity?.toDomainModel()
        }
    }

    override suspend fun addServer(server: Server): Long {
        val entity = ServerEntity.fromDomainModel(server)

        // Если сервер помечен как активный, сбрасываем активный статус других серверов
        if (server.isActive) {
            serverDao.clearActiveStatus()
            // Обновляем информацию о текущем активном сервере в DataStore
            appSettingsDataStore.setActiveServer(server.id, server.name)
        }

        val id = serverDao.insertServer(entity)
        logRepository.logInfo("Добавлен сервер: ${server.name} (${server.host}:${server.port})")
        return id
    }

    override suspend fun updateServer(server: Server) {
        val entity = ServerEntity.fromDomainModel(server)

        // Если сервер помечен как активный, сбрасываем активный статус других серверов
        if (server.isActive) {
            serverDao.clearActiveStatus()
            // Обновляем информацию о текущем активном сервере в DataStore
            appSettingsDataStore.setActiveServer(server.id, server.name)
        }

        serverDao.updateServer(entity)
        logRepository.logInfo("Обновлен сервер: ${server.name} (${server.host}:${server.port})")
    }

    override suspend fun deleteServer(id: Int) {
        val server = serverDao.getServerById(id)
        if (server != null) {
            // Если удаляемый сервер был активным, очищаем информацию об активном сервере
            if (server.isActive) {
                appSettingsDataStore.clearActiveServer()
            }

            serverDao.deleteServerById(id)
            logRepository.logInfo("Удален сервер: ${server.name} (${server.host}:${server.port})")
        }
    }

    override suspend fun setActiveServer(id: Int) {
        // Сбрасываем активный статус всех серверов
        serverDao.clearActiveStatus()

        // Устанавливаем активный статус для указанного сервера
        serverDao.setActiveServer(id)

        // Получаем информацию о новом активном сервере
        val server = serverDao.getServerById(id)
        if (server != null) {
            // Обновляем информацию о текущем активном сервере в DataStore
            appSettingsDataStore.setActiveServer(server.id, server.name)
            logRepository.logInfo("Установлен активный сервер: ${server.name} (${server.host}:${server.port})")
        }
    }

    override suspend fun clearActiveServer() {
        // Сбрасываем активный статус всех серверов
        serverDao.clearActiveStatus()

        // Очищаем информацию об активном сервере в DataStore
        appSettingsDataStore.clearActiveServer()
        logRepository.logInfo("Сброшен активный сервер")
    }

    override suspend fun testConnection(server: Server): Result<Boolean> {
        return try {
            val response = apiService.testConnection(server)
            when (response) {
                is ApiResult.Success -> {
                    logRepository.logInfo("Успешное подключение к серверу: ${server.name} (${server.host}:${server.port})")
                    Result.success(true)
                }

                is ApiResult.Error -> {
                    logRepository.logWarning("Ошибка подключения к серверу: ${server.name} (${server.host}:${server.port}), код: ${response.code}")
                    Result.failure(IOException("Connection test failed with code: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during server connection test")
            logRepository.logError("Исключение при тестировании подключения: ${e.message}")
            Result.failure(e)
        }
    }
}