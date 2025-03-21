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

/**
 * DAO для работы с очередью операций синхронизации
 */
@Dao
interface SyncOperationDao {
    /**
     * Получение всех незавершенных операций
     */
    @Query("SELECT * FROM sync_operations WHERE completed = 0 ORDER BY priority, createdAt")
    fun getPendingOperations(): Flow<List<SyncOperation>>

    /**
     * Получение всех операций определенного типа
     */
    @Query("SELECT * FROM sync_operations WHERE operationType = :type")
    fun getOperationsByType(type: OperationType): Flow<List<SyncOperation>>

    /**
     * Получение операций, готовых для выполнения
     */
    @Query("SELECT * FROM sync_operations WHERE completed = 0 " +
            "AND (nextAttemptAt IS NULL OR nextAttemptAt <= :now) " +
            "ORDER BY priority, createdAt")
    suspend fun getReadyOperations(now: LocalDateTime = LocalDateTime.now()): List<SyncOperation>

    /**
     * Получение операций для конкретной цели
     */
    @Query("SELECT * FROM sync_operations WHERE targetId = :targetId")
    suspend fun getOperationsForTarget(targetId: String): List<SyncOperation>

    /**
     * Добавление новой операции
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: SyncOperation): Long

    /**
     * Обновление операции
     */
    @Update
    suspend fun updateOperation(operation: SyncOperation)

    /**
     * Отметка операции как завершенной
     */
    @Query("UPDATE sync_operations SET completed = 1 WHERE id = :id")
    suspend fun markOperationAsCompleted(id: Long)

    /**
     * Увеличение счетчика попыток и установка времени последней попытки
     */
    @Query("UPDATE sync_operations SET attempts = attempts + 1, lastAttemptAt = :attemptTime " +
            "WHERE id = :id")
    suspend fun incrementAttempts(id: Long, attemptTime: LocalDateTime = LocalDateTime.now())

    /**
     * Установка времени следующей попытки
     */
    @Query("UPDATE sync_operations SET nextAttemptAt = :nextAttemptTime WHERE id = :id")
    suspend fun setNextAttemptTime(id: Long, nextAttemptTime: LocalDateTime)

    /**
     * Установка ошибки для операции
     */
    @Query("UPDATE sync_operations SET lastError = :error WHERE id = :id")
    suspend fun setOperationError(id: Long, error: String)

    /**
     * Удаление завершенных операций старше определенного времени
     */
    @Query("DELETE FROM sync_operations WHERE completed = 1 AND lastAttemptAt < :cutoffTime")
    suspend fun deleteOldCompletedOperations(cutoffTime: LocalDateTime)

    /**
     * Получение операции по идентификатору
     */
    @Query("SELECT * FROM sync_operations WHERE id = :id")
    suspend fun getOperationById(id: Long): SyncOperation?

}