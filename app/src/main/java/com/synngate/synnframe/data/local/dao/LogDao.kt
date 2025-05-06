package com.synngate.synnframe.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.synngate.synnframe.data.local.entity.LogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface LogDao {

    @Query("SELECT id, SUBSTR(message, 1, 500) as message, type, createdAt FROM logs ORDER BY createdAt DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Query("SELECT id, SUBSTR(message, 1, 500) as message, type, createdAt FROM logs WHERE type IN (:types) ORDER BY createdAt DESC")
    fun getLogsByTypes(types: List<String>): Flow<List<LogEntity>>

    @Query("SELECT id, SUBSTR(message, 1, 500) as message, type, createdAt FROM logs WHERE message LIKE '%' || :messageFilter || '%' ORDER BY createdAt DESC")
    fun getLogsByMessageFilter(messageFilter: String): Flow<List<LogEntity>>

    @Query("""
    SELECT id, SUBSTR(message, 1, 500) as message, type, createdAt FROM logs

    WHERE type IN (:types)
    AND message LIKE '%' || :messageFilter || '%'
    
    ORDER BY createdAt DESC
    """)
    fun getLogsByTypesAndMessageFilter(types: List<String>, messageFilter: String): Flow<List<LogEntity>>

    @Query("SELECT id, SUBSTR(message, 1, 500) as message, type, createdAt FROM logs WHERE createdAt BETWEEN :dateFrom AND :dateTo ORDER BY createdAt DESC")
    fun getLogsByDateRange(dateFrom: LocalDateTime, dateTo: LocalDateTime): Flow<List<LogEntity>>

    @Query("""
        SELECT id, SUBSTR(message, 1, 500) as message, type, createdAt FROM logs
    
        WHERE type IN (:types) 
        AND message LIKE '%' || :messageFilter || '%' 
        AND createdAt BETWEEN :dateFrom AND :dateTo
    
        ORDER BY createdAt DESC
    """)
    fun getFilteredLogs(
        types: List<String>,
        messageFilter: String,
        dateFrom: LocalDateTime,
        dateTo: LocalDateTime
    ): Flow<List<LogEntity>>

    @Query("SELECT * FROM logs WHERE id = :id")
    suspend fun getLogById(id: Int): LogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity): Long

    @Delete
    suspend fun deleteLog(log: LogEntity)

    @Query("DELETE FROM logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    @Query("DELETE FROM logs")
    suspend fun deleteAllLogs()

    @Query("DELETE FROM logs WHERE createdAt < :date")
    suspend fun deleteLogsOlderThan(date: LocalDateTime): Int
}