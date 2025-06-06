package com.synngate.synnframe.domain.service

import kotlinx.coroutines.flow.Flow

interface WebServerController : ServiceController {

    val serverHost: Flow<String>

    val serverPort: Flow<Int>

    val lastRequests: Flow<List<RequestInfo>>

    suspend fun clearRequestsLog()

    fun updateRunningState(isRunning: Boolean)

    data class RequestInfo(
        val timestamp: Long,
        val method: String,
        val path: String,
        val statusCode: Int,
        val responseTime: Long,
        val clientIp: String
    )
}