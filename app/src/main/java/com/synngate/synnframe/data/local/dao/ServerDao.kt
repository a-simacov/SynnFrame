package com.synngate.synnframe.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.synngate.synnframe.data.local.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с серверами в базе данных
 */
@Dao
interface ServerDao {
    /**
     * Получение всех серверов
     */
    @Query("SELECT * FROM servers ORDER BY name ASC")
    fun getAllServers(): Flow<List<ServerEntity>>

    /**
     * Получение сервера по идентификатору
     */
    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getServerById(id: Int): ServerEntity?

    /**
     * Получение активного сервера
     */
    @Query("SELECT * FROM servers WHERE isActive = 1 LIMIT 1")
    fun getActiveServer(): Flow<ServerEntity?>

    /**
     * Вставка нового сервера
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerEntity): Long

    /**
     * Обновление существующего сервера
     */
    @Update
    suspend fun updateServer(server: ServerEntity)

    /**
     * Удаление сервера
     */
    @Delete
    suspend fun deleteServer(server: ServerEntity)

    /**
     * Удаление сервера по идентификатору
     */
    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteServerById(id: Int)

    /**
     * Сброс активного статуса для всех серверов
     */
    @Query("UPDATE servers SET isActive = 0")
    suspend fun clearActiveStatus()

    /**
     * Установка сервера в качестве активного
     */
    @Query("UPDATE servers SET isActive = 1 WHERE id = :id")
    suspend fun setActiveServer(id: Int)
}