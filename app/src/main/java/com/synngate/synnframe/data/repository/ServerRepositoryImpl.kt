package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.local.dao.ServerDao
import com.synngate.synnframe.data.local.entity.ServerEntity
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.service.ApiService
import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

class ServerRepositoryImpl(
    private val serverDao: ServerDao,
    private val apiService: ApiService
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
        return serverDao.insertServer(entity)
    }

    override suspend fun updateServer(server: Server) {
        val entity = ServerEntity.fromDomainModel(server)
        serverDao.updateServer(entity)
    }

    override suspend fun deleteServer(id: Int) {
        serverDao.deleteServerById(id)
    }

    override suspend fun setActiveServer(id: Int) {
        serverDao.clearActiveStatus()
        serverDao.setActiveServer(id)
    }

    override suspend fun clearActiveStatus() {
        serverDao.clearActiveStatus()
    }

    override suspend fun testConnection(server: Server): ApiResult<Unit> {
        return apiService.testConnection(server)
    }
}