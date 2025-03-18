package com.synngate.synnframe.domain.entity

/**
 * Доменная модель пользователя мобильного приложения
 */
data class User(
    /**
     * Идентификатор пользователя
     */
    val id: String,

    /**
     * Имя пользователя
     */
    val name: String,

    /**
     * Пароль пользователя
     */
    val password: String,

    /**
     * Идентификатор группы пользователя
     */
    val userGroupId: String
)