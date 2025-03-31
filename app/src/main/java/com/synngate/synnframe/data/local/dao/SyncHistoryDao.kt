package com.synngate.synnframe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.synngate.synnframe.data.sync.SyncHistoryRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface SyncHistoryDao {

    @Query("SELECT * FROM sync_history ORDER BY startTime DESC")
    fun getAllHistory(): Flow<List<SyncHistoryRecord>>

    @Query("SELECT * FROM sync_history WHERE successful = 1 ORDER BY startTime DESC")
    fun getSuccessfulSyncs(): Flow<List<SyncHistoryRecord>>

    @Query("SELECT * FROM sync_history WHERE successful = 0 ORDER BY startTime DESC")
    fun getFailedSyncs(): Flow<List<SyncHistoryRecord>>

    @Query("SELECT * FROM sync_history WHERE startTime >= :since ORDER BY startTime DESC")
    fun getRecentSyncs(since: LocalDateTime): Flow<List<SyncHistoryRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(record: SyncHistoryRecord)

    @Query("DELETE FROM sync_history WHERE startTime < :cutoffDate")
    suspend fun deleteOldRecords(cutoffDate: LocalDateTime)

    @Query("SELECT COUNT(*) FROM sync_history")
    suspend fun getCount(): Int
}