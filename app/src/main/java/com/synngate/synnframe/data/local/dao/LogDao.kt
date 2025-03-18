package com.synngate.synnframe.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.synngate.synnframe.data.local.entity.LogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * DAO для работы с логами в базе данных
 */
@Dao
interface LogDao {
    /**
     * Получение всех логов
     */
    @Query("SELECT * FROM logs ORDER BY createdAt DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    /**
     * Получение логов с фильтрацией по типу
     */
    @Query("SELECT * FROM logs WHERE type IN (:types) ORDER BY createdAt DESC")
    fun getLogsByTypes(types: List<String>): Flow<List<LogEntity>>

    /**
     * Получение логов с фильтрацией по сообщению
     */
    @Query("SELECT * FROM logs WHERE message LIKE '%' || :messageFilter || '%' ORDER BY createdAt DESC")
    fun getLogsByMessageFilter(messageFilter: String): Flow<List<LogEntity>>

    /**
     * Получение логов с фильтрацией по типу и сообщению
     */
    @Query("SELECT * FROM logs WHERE type IN (:types) AND message LIKE '%' || :messageFilter || '%' ORDER BY createdAt DESC")
    fun getLogsByTypesAndMessageFilter(types: List<String>, messageFilter: String): Flow<List<LogEntity>>

    /**
     * Получение логов с фильтрацией по дате
     */
    @Query("SELECT * FROM logs WHERE createdAt BETWEEN :dateFrom AND :dateTo ORDER BY createdAt DESC")
    fun getLogsByDateRange(dateFrom: LocalDateTime, dateTo: LocalDateTime): Flow<List<LogEntity>>

    /**
     * Получение логов с комплексной фильтрацией
     */
    @Query("SELECT * FROM logs WHERE type IN (:types) AND message LIKE '%' || :messageFilter || '%' AND createdAt BETWEEN :dateFrom AND :dateTo ORDER BY createdAt DESC")
    fun getFilteredLogs(types: List<String>, messageFilter: String, dateFrom: LocalDateTime, dateTo: LocalDateTime): Flow<List<LogEntity>>

    /**
     * Получение лога по идентификатору
     */
    @Query("SELECT * FROM logs WHERE id = :id")
    suspend fun getLogById(id: Int): LogEntity?

    /**
     * Вставка нового лога
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity): Long

    /**
     * Удаление лога
     */
    @Delete
    suspend fun deleteLog(log: LogEntity)

    /**
     * Удаление лога по идентификатору
     */
    @Query("DELETE FROM logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    /**
     * Удаление всех логов
     */
    @Query("DELETE FROM logs")
    suspend fun deleteAllLogs()

    /**
     * Удаление логов старше указанной даты
     */
    @Query("DELETE FROM logs WHERE createdAt < :date")
    suspend fun deleteLogsOlderThan(date: LocalDateTime): Int
}