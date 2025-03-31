package com.synngate.synnframe.domain.service

import kotlinx.coroutines.flow.Flow

interface ServiceController {

    val isRunning: Flow<Boolean>

    suspend fun startService(): Result<Unit>

    suspend fun stopService(): Result<Unit>

    suspend fun toggleService(): Result<Boolean>
}