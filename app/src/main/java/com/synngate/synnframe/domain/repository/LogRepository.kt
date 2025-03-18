package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.Log
import com.synngate.synnframe.domain.entity.LogType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Интерфейс репозитория для работы с логами
 */
interface LogRepository {
    /**
     * Получение списка всех логов
     */
    fun getLogs(): Flow<List<Log>>

    /**
     * Получение списка логов с фильтрацией
     */
    fun getFilteredLogs(
        messageFilter: String? = null,
        typeFilter: List<LogType>? = null,
        dateFromFilter: LocalDateTime? = null,
        dateToFilter: LocalDateTime? = null
    ): Flow<List<Log>>

    /**
     * Получение лога по идентификатору
     */
    suspend fun getLogById(id: Int): Log?

    /**
     * Добавление нового лога
     */
    suspend fun addLog(log: Log): Long

    /**
     * Удаление лога
     */
    suspend fun deleteLog(id: Int)

    /**
     * Удаление всех логов
     */
    suspend fun deleteAllLogs()

    /**
     * Удаление логов старше указанной даты
     */
    suspend fun deleteLogsOlderThan(date: LocalDateTime): Int
}