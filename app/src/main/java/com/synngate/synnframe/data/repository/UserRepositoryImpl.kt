package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.data.local.dao.UserDao
import com.synngate.synnframe.data.local.entity.UserEntity
import com.synngate.synnframe.data.remote.api.AuthApi
import com.synngate.synnframe.domain.entity.User
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException

/**
 * Имплементация репозитория пользователей
 */
class UserRepositoryImpl(
    private val userDao: UserDao,
    private val authApi: AuthApi,
    private val appSettingsDataStore: AppSettingsDataStore,
    private val logRepository: LogRepository
) : UserRepository {

    override fun getUsers(): Flow<List<User>> {
        return userDao.getAllUsers().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getUserById(id: String): User? {
        return userDao.getUserById(id)?.toDomainModel()
    }

    override suspend fun getUserByPassword(password: String): User? {
        return userDao.getUserByPassword(password)?.toDomainModel()
    }

    override fun getCurrentUser(): Flow<User?> {
        return userDao.getCurrentUser().map { entity ->
            entity?.toDomainModel()
        }
    }

    override suspend fun addUser(user: User) {
        val entity = UserEntity.fromDomainModel(user)
        userDao.insertUser(entity)
        logRepository.logInfo("Добавлен пользователь: ${user.name}")
    }

    override suspend fun updateUser(user: User) {
        val entity = UserEntity.fromDomainModel(user)
        userDao.updateUser(entity)
        logRepository.logInfo("Обновлен пользователь: ${user.name}")
    }

    override suspend fun deleteUser(id: String) {
        val user = userDao.getUserById(id)
        if (user != null) {
            // Если удаляемый пользователь был текущим, очищаем информацию о текущем пользователе
            if (user.isCurrentUser) {
                appSettingsDataStore.setCurrentUser(null)
            }

            userDao.deleteUserById(id)
            logRepository.logInfo("Удален пользователь: ${user.name}")
        }
    }

    override suspend fun setCurrentUser(userId: String) {
        // Сбрасываем статус текущего пользователя для всех пользователей
        userDao.clearCurrentUserStatus()

        // Устанавливаем статус текущего пользователя для указанного пользователя
        userDao.setCurrentUser(userId)

        // Обновляем информацию о текущем пользователе в DataStore
        appSettingsDataStore.setCurrentUser(userId)

        // Логируем изменение
        val user = userDao.getUserById(userId)
        if (user != null) {
            logRepository.logInfo("Установлен текущий пользователь: ${user.name}")
        }
    }

    override suspend fun clearCurrentUser() {
        // Сбрасываем статус текущего пользователя для всех пользователей
        userDao.clearCurrentUserStatus()

        // Очищаем информацию о текущем пользователе в DataStore
        appSettingsDataStore.setCurrentUser(null)

        // Логируем изменение
        logRepository.logInfo("Сброшен текущий пользователь")
    }

    override suspend fun authenticateUserOnServer(password: String, deviceInfo: Map<String, String>): Result<User> {
        return try {
            val response = authApi.authenticate(password, deviceInfo)
            if (response.isSuccessful) {
                val userDto = response.body()
                if (userDto != null) {
                    // Создаем объект пользователя
                    val user = User(
                        id = userDto.id,
                        name = userDto.name,
                        password = password, // Сохраняем введенный пароль
                        userGroupId = userDto.userGroupId
                    )

                    // Сохраняем пользователя в базе данных
                    addUser(user)

                    // Устанавливаем его как текущего
                    setCurrentUser(user.id)

                    // Логируем успешную аутентификацию
                    logRepository.logInfo("Успешная аутентификация пользователя: ${user.name}")

                    Result.success(user)
                } else {
                    logRepository.logWarning("Пустой ответ при аутентификации")
                    Result.failure(IOException("Empty authentication response"))
                }
            } else {
                // Извлекаем сообщение об ошибке из ответа
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                logRepository.logWarning("Ошибка аутентификации: $errorBody")
                Result.failure(IOException("Authentication failed: $errorBody"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during user authentication")
            logRepository.logError("Исключение при аутентификации: ${e.message}")
            Result.failure(e)
        }
    }
}