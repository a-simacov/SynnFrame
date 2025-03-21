package com.synngate.synnframe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.synngate.synnframe.data.sync.SyncHistoryRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * DAO для работы с историей синхронизаций
 */
@Dao
interface SyncHistoryDao {
    /**
     * Получение всей истории синхронизаций в порядке убывания времени начала
     */
    @Query("SELECT * FROM sync_history ORDER BY startTime DESC")
    fun getAllHistory(): Flow<List<SyncHistoryRecord>>

    /**
     * Получение успешных синхронизаций
     */
    @Query("SELECT * FROM sync_history WHERE successful = 1 ORDER BY startTime DESC")
    fun getSuccessfulSyncs(): Flow<List<SyncHistoryRecord>>

    /**
     * Получение неудачных синхронизаций
     */
    @Query("SELECT * FROM sync_history WHERE successful = 0 ORDER BY startTime DESC")
    fun getFailedSyncs(): Flow<List<SyncHistoryRecord>>

    /**
     * Получение синхронизаций за последнее время
     */
    @Query("SELECT * FROM sync_history WHERE startTime >= :since ORDER BY startTime DESC")
    fun getRecentSyncs(since: LocalDateTime): Flow<List<SyncHistoryRecord>>

    /**
     * Добавление записи истории
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(record: SyncHistoryRecord)

    /**
     * Удаление старых записей
     */
    @Query("DELETE FROM sync_history WHERE startTime < :cutoffDate")
    suspend fun deleteOldRecords(cutoffDate: LocalDateTime)

    /**
     * Подсчет количества синхронизаций
     */
    @Query("SELECT COUNT(*) FROM sync_history")
    suspend fun getCount(): Int
}