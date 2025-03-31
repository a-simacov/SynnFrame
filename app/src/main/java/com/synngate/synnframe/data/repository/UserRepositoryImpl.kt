package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import com.synngate.synnframe.data.local.dao.UserDao
import com.synngate.synnframe.data.local.entity.UserEntity
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.AuthApi
import com.synngate.synnframe.data.remote.dto.AuthResponseDto
import com.synngate.synnframe.domain.entity.User
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val authApi: AuthApi,
    private val appSettingsDataStore: AppSettingsDataStore
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
    }

    override suspend fun updateUser(user: User) {
        val entity = UserEntity.fromDomainModel(user)
        userDao.updateUser(entity)
    }

    override suspend fun deleteUser(id: String) {
        userDao.deleteUserById(id)
    }

    override suspend fun setCurrentUser(userId: String) {
        userDao.clearCurrentUserStatus()
        userDao.setCurrentUser(userId)
        appSettingsDataStore.setCurrentUser(userId)
    }

    override suspend fun clearCurrentUser() {
        userDao.clearCurrentUserStatus()
        appSettingsDataStore.setCurrentUser(null)
    }

    override suspend fun authenticateWithServer(password: String, deviceInfo: Map<String, String>): ApiResult<AuthResponseDto> {
        return authApi.authenticate(password, deviceInfo)
    }
}