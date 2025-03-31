package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.Server
import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    fun getServers(): Flow<List<Server>>
    suspend fun getServerById(id: Int): Server?
    fun getActiveServer(): Flow<Server?>
    suspend fun addServer(server: Server): Long
    suspend fun updateServer(server: Server)
    suspend fun deleteServer(id: Int)
    suspend fun setActiveServer(id: Int)
    suspend fun clearActiveStatus()
    // Возвращает непосредственный результат API, без преобразования в Result
    suspend fun testConnection(server: Server): ApiResult<Unit>
}