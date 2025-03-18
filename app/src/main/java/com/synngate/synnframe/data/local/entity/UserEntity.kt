package com.synngate.synnframe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.synngate.synnframe.domain.entity.User

/**
 * Entity класс для хранения информации о пользователях в Room
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val password: String,
    val userGroupId: String,
    val isCurrentUser: Boolean = false
) {
    /**
     * Преобразование в доменную модель
     */
    fun toDomainModel(): User {
        return User(
            id = id,
            name = name,
            password = password,
            userGroupId = userGroupId
        )
    }

    companion object {
        /**
         * Создание Entity из доменной модели
         */
        fun fromDomainModel(user: User, isCurrentUser: Boolean = false): UserEntity {
            return UserEntity(
                id = user.id,
                name = user.name,
                password = user.password,
                userGroupId = user.userGroupId,
                isCurrentUser = isCurrentUser
            )
        }
    }
}