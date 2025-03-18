// Файл: com.synngate.synnframe.domain.usecase.user.UserUseCases.kt

package com.synngate.synnframe.domain.usecase.user

import com.synngate.synnframe.domain.entity.User
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.UserRepository
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Use Case класс для операций с пользователями
 */
class UserUseCases(
    private val userRepository: UserRepository,
    private val loggingService: LoggingService
) : BaseUseCase {

    // Базовые операции
    fun getUsers(): Flow<List<User>> =
        userRepository.getUsers()

    suspend fun getUserById(id: String): User? =
        userRepository.getUserById(id)

    suspend fun getUserByPassword(password: String): User? =
        userRepository.getUserByPassword(password)

    fun getCurrentUser(): Flow<User?> =
        userRepository.getCurrentUser()

    // Операции с бизнес-логикой
    suspend fun loginUser(password: String, deviceInfo: Map<String, String>): Result<User> {
        return try {
            // Проверяем наличие пользователя в локальной БД
            val localUser = userRepository.getUserByPassword(password)
            if (localUser != null) {
                // Пользователь найден, устанавливаем как текущего
                userRepository.setCurrentUser(localUser.id)
                loggingService.logInfo("Вход выполнен для пользователя: ${localUser.name}")
                return Result.success(localUser)
            }

            // Пользователь не найден, пробуем аутентификацию на сервере
            val result = userRepository.authenticateUserOnServer(password, deviceInfo)

            if (result.isSuccess) {
                val user = result.getOrNull()!!
                loggingService.logInfo("Успешная аутентификация пользователя: ${user.name}")
                return Result.success(user)
            } else {
                loggingService.logWarning("Ошибка аутентификации: ${result.exceptionOrNull()?.message}")
                return result
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during user login")
            loggingService.logError("Исключение при входе пользователя: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun logoutUser(): Result<Unit> {
        return try {
            userRepository.clearCurrentUser()
            loggingService.logInfo("Выход пользователя выполнен")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during user logout")
            loggingService.logError("Исключение при выходе пользователя: ${e.message}")
            Result.failure(e)
        }
    }
}