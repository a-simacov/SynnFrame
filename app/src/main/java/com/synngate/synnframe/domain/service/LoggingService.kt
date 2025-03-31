package com.synngate.synnframe.domain.service

interface LoggingService {

    suspend fun logInfo(message: String): Long

    suspend fun logWarning(message: String): Long

    suspend fun logError(message: String): Long
}