package com.synngate.synnframe.domain.service

import kotlinx.coroutines.flow.Flow

interface WebServerManager {

    val isRunning: Flow<Boolean>

    suspend fun startServer(): Result<Unit>

    suspend fun stopServer(): Result<Unit>

    suspend fun toggleServer(): Result<Boolean>
}