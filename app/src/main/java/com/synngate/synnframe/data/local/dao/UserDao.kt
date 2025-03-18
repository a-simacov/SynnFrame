package com.synngate.synnframe.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.synngate.synnframe.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с пользователями в базе данных
 */
@Dao
interface UserDao {
    /**
     * Получение всех пользователей
     */
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    /**
     * Получение пользователя по идентификатору
     */
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    /**
     * Получение пользователя по паролю
     */
    @Query("SELECT * FROM users WHERE password = :password LIMIT 1")
    suspend fun getUserByPassword(password: String): UserEntity?

    /**
     * Получение текущего пользователя
     */
    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    /**
     * Вставка нового пользователя
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * Обновление существующего пользователя
     */
    @Update
    suspend fun updateUser(user: UserEntity)

    /**
     * Удаление пользователя
     */
    @Delete
    suspend fun deleteUser(user: UserEntity)

    /**
     * Удаление пользователя по идентификатору
     */
    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: String)

    /**
     * Сброс статуса текущего пользователя для всех пользователей
     */
    @Query("UPDATE users SET isCurrentUser = 0")
    suspend fun clearCurrentUserStatus()

    /**
     * Установка пользователя в качестве текущего
     */
    @Query("UPDATE users SET isCurrentUser = 1 WHERE id = :id")
    suspend fun setCurrentUser(id: String)
}