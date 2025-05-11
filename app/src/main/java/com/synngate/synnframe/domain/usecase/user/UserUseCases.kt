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
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error setting current user")
            Result.failure(e)
        }
    }

    private suspend fun clearCurrentUser(): Result<Unit> {
        return try {
            userRepository.clearCurrentUser()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error clearing current user")
            Result.failure(e)
        }
    }

    suspend fun loginUser(password: String, deviceInfo: Map<String, String>): Result<User> {
        return try {
            val localUser = userRepository.getUserByPassword(password)
            if (localUser != null) {
                val setCurrentUserResult = setCurrentUser(localUser.id)
                if (setCurrentUserResult.isFailure) {
                    return setCurrentUserResult.map { localUser }
                }

                return Result.success(localUser)
            }

            val response = userRepository.authenticateWithServer(password, deviceInfo)

            return when (response) {
                is ApiResult.Success -> {
                    val userDto = response.data
                    val user = User(
                        id = userDto.id,
                        name = userDto.name,
                        password = password,
                        userGroupId = userDto.userGroupId
                    )

                    val addUserResult = addUser(user)
                    if (addUserResult.isFailure) {
                        return addUserResult
                    }

                    val setCurrentUserResult = setCurrentUser(user.id)
                    if (setCurrentUserResult.isFailure) {
                        return setCurrentUserResult.map { user }
                    }

                    Result.success(user)
                }
                is ApiResult.Error -> {
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