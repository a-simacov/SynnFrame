package com.synngate.synnframe.data.service

import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.domain.repository.ServerRepository
import com.synngate.synnframe.domain.service.ServerCoordinator
import timber.log.Timber
import java.io.IOException

class ServerCoordinatorImpl(
    private val serverRepository: ServerRepository,
    private val appSettingsDataStore: AppSettingsDataStore
) : ServerCoordinator {

    override suspend fun switchActiveServer(id: Int): Result<Unit> {
        return try {
            val server = serverRepository.getServerById(id)
            if (server == null) {
                Timber.w("Attempt to activate unexistent server: $id")
                return Result.failure(IllegalArgumentException("Server not found: $id"))
            }

            serverRepository.setActiveServer(id)

            appSettingsDataStore.setActiveServer(id, server.name)

            Timber.i("Active server was set: ${server.name} (${server.host}:${server.port})")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e("Error on setting active server: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun clearActiveServer(): Result<Unit> {
        return try {
            serverRepository.clearActiveStatus()

            appSettingsDataStore.clearActiveServer()

            Timber.i("Active server was reset")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e("Error on reset the active server: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun testServerConnection(server: Server): Result<Boolean> {
        return try {
            val response = serverRepository.testConnection(server)

            when (response) {
                is ApiResult.Success -> {
                    Timber.i("Success connection to the server: ${server.name} (${server.host}:${server.port})")
                    Result.success(true)
                }
                is ApiResult.Error -> {
                    Timber.w("Error conection to the server: ${server.name} (${server.host}:${server.port}), причина: ${response.message}")
                    Result.failure(IOException("Connection test failed with code: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Timber.e("Exception while testing connection: ${e.message}")
            Result.failure(e)
        }
    }
}