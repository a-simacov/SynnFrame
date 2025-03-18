package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.User
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с пользователями
 */
interface UserRepository {
    /**
     * Получение списка всех пользователей
     */
    fun getUsers(): Flow<List<User>>

    /**
     * Получение пользователя по идентификатору
     */
    suspend fun getUserById(id: String): User?

    /**
     * Получение пользователя по паролю
     */
    suspend fun getUserByPassword(password: String): User?

    /**
     * Получение текущего пользователя
     */
    fun getCurrentUser(): Flow<User?>

    /**
     * Добавление нового пользователя
     */
    suspend fun addUser(user: User)

    /**
     * Обновление существующего пользователя
     */
    suspend fun updateUser(user: User)

    /**
     * Удаление пользователя
     */
    suspend fun deleteUser(id: String)

    /**
     * Установка текущего пользователя
     */
    suspend fun setCurrentUser(userId: String)

    /**
     * Очистка текущего пользователя (выход)
     */
    suspend fun clearCurrentUser()

    /**
     * Аутентификация пользователя на сервере
     */
    suspend fun authenticateUserOnServer(password: String, deviceInfo: Map<String, String>): Result<User>
}