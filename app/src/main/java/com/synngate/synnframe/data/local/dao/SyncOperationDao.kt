package com.synngate.synnframe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.synngate.synnframe.data.local.entity.OperationType
import com.synngate.synnframe.data.local.entity.SyncOperation
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface SyncOperationDao {

    @Query("SELECT * FROM sync_operations WHERE completed = 0 ORDER BY priority, createdAt")
    fun getPendingOperations(): Flow<List<SyncOperation>>

    @Query("SELECT * FROM sync_operations WHERE operationType = :type")
    fun getOperationsByType(type: OperationType): Flow<List<SyncOperation>>

    @Query("SELECT * FROM sync_operations WHERE completed = 0 " +
            "AND (nextAttemptAt IS NULL OR nextAttemptAt <= :now) " +
            "ORDER BY priority, createdAt")
    suspend fun getReadyOperations(now: LocalDateTime = LocalDateTime.now()): List<SyncOperation>

    @Query("SELECT * FROM sync_operations WHERE targetId = :targetId")
    suspend fun getOperationsForTarget(targetId: String): List<SyncOperation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: SyncOperation): Long

    @Update
    suspend fun updateOperation(operation: SyncOperation)

    @Query("UPDATE sync_operations SET completed = 1 WHERE id = :id")
    suspend fun markOperationAsCompleted(id: Long)

    @Query("UPDATE sync_operations SET attempts = attempts + 1, lastAttemptAt = :attemptTime " +
            "WHERE id = :id")
    suspend fun incrementAttempts(id: Long, attemptTime: LocalDateTime = LocalDateTime.now())

    @Query("UPDATE sync_operations SET nextAttemptAt = :nextAttemptTime WHERE id = :id")
    suspend fun setNextAttemptTime(id: Long, nextAttemptTime: LocalDateTime)

    @Query("UPDATE sync_operations SET lastError = :error WHERE id = :id")
    suspend fun setOperationError(id: Long, error: String)

    @Query("DELETE FROM sync_operations WHERE completed = 1 AND lastAttemptAt < :cutoffTime")
    suspend fun deleteOldCompletedOperations(cutoffTime: LocalDateTime)

    @Query("SELECT * FROM sync_operations WHERE id = :id")
    suspend fun getOperationById(id: Long): SyncOperation?

}