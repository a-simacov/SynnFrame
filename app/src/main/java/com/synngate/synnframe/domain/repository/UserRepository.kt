package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.dto.AuthResponseDto
import com.synngate.synnframe.domain.entity.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {

    fun getUsers(): Flow<List<User>>

    suspend fun getUserById(id: String): User?

    suspend fun getUserByPassword(password: String): User?

    fun getCurrentUser(): Flow<User?>

    suspend fun addUser(user: User)

    suspend fun updateUser(user: User)

    suspend fun deleteUser(id: String)

    suspend fun setCurrentUser(userId: String)

    suspend fun clearCurrentUser()

    suspend fun authenticateWithServer(password: String, deviceInfo: Map<String, String>): ApiResult<AuthResponseDto>
}