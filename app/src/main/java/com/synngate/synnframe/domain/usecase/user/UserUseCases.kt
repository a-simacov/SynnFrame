package com.synngate.synnframe.domain.usecase.user

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.User
import com.synngate.synnframe.domain.repository.UserRepository
import com.synngate.synnframe.domain.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.io.IOException

class UserUseCases(
    private val userRepository: UserRepository
) : BaseUseCase {

    fun getUsers(): Flow<List<User>> =
        userRepository.getUsers()

    suspend fun getUserById(id: String): User? =
        userRepository.getUserById(id)

    suspend fun getUserByPassword(password: String): User? =
        userRepository.getUserByPassword(password)

    fun getCurrentUser(): Flow<User?> =
        userRepository.getCurrentUser()

    private suspend fun addUser(user: User): Result<User> {
        return try {
            userRepository.addUser(user)
            Timber.i("User was added: ${user.name}")
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(e, "Error adding user")
            Result.failure(e)
        }
    }

    private suspend fun setCurrentUser(userId: String): Result<Unit> {
        return try {
            val user = userRepository.getUserById(userId)
            if (user == null) {
                return Result.failure(IllegalArgumentException("Пользователь не найден"))
            }

            userRepository.setCurrentUser(userId)
            Timber.i("Current user was set: ${user.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error setting current user")
            Result.failure(e)
        }
    }

    private suspend fun clearCurrentUser(): Result<Unit> {
        return try {
            userRepository.clearCurrentUser()
            Timber.i("Current user was cleared")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error clearing current user")
            Result.failure(e)
        }
    }

    suspend fun loginUser(password: String, deviceInfo: Map<String, String>): Result<User> {
        return try {
            // Проверяем наличие пользователя в локальной БД
            val localUser = userRepository.getUserByPassword(password)
            if (localUser != null) {
                // Пользователь найден, устанавливаем как текущего
                val setCurrentUserResult = setCurrentUser(localUser.id)
                if (setCurrentUserResult.isFailure) {
                    return setCurrentUserResult.map { localUser }
                }

                Timber.i("Logged in user: ${localUser.name}")
                return Result.success(localUser)
            }

            // Пользователь не найден, пробуем аутентификацию на сервере
            val response = userRepository.authenticateWithServer(password, deviceInfo)

            return when (response) {
                is ApiResult.Success -> {
                    val userDto = response.data
                    if (userDto != null) {
                        // Создаем объект пользователя
                        val user = User(
                            id = userDto.id,
                            name = userDto.name,
                            password = password, // Сохраняем введенный пароль
                            userGroupId = userDto.userGroupId
                        )

                        // Сохраняем пользователя в базе данных
                        val addUserResult = addUser(user)
                        if (addUserResult.isFailure) {
                            return addUserResult
                        }

                        // Устанавливаем его как текущего
                        val setCurrentUserResult = setCurrentUser(user.id)
                        if (setCurrentUserResult.isFailure) {
                            return setCurrentUserResult.map { user }
                        }

                        Timber.i("User auth was successfull: ${user.name}")
                        Result.success(user)
                    } else {
                        Timber.w("Empty answer on auth")
                        Result.failure(IOException("Пустой ответ при аутентификации"))
                    }
                }
                is ApiResult.Error -> {
                    Timber.w("Error in auth: ${response.message}")
                    Result.failure(IOException("Ошибка аутентификации: ${response.code} - ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during user login")
            Result.failure(e)
        }
    }

    suspend fun logoutUser(): Result<Unit> {
        return clearCurrentUser()
    }
}