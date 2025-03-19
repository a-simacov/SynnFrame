package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.Server

interface ServerCoordinator {
    suspend fun switchActiveServer(id: Int): Result<Unit>
    suspend fun clearActiveServer(): Result<Unit>
    suspend fun testServerConnection(server: Server): Result<Boolean>
}