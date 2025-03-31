package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.Log
import com.synngate.synnframe.domain.entity.LogType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface LogRepository {

    fun getLogs(): Flow<List<Log>>

    fun getFilteredLogs(
        messageFilter: String? = null,
        typeFilter: List<LogType>? = null,
        dateFromFilter: LocalDateTime? = null,
        dateToFilter: LocalDateTime? = null
    ): Flow<List<Log>>

    suspend fun getLogById(id: Int): Log?

    suspend fun addLog(log: Log): Long

    suspend fun deleteLog(id: Int)

    suspend fun deleteAllLogs()

    suspend fun deleteLogsOlderThan(date: LocalDateTime): Int
}