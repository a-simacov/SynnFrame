package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO для запроса аутентификации
 */
@Serializable
data class AuthRequestDto(
    /**
     * IP-адрес устройства в сети
     */
    @SerialName("deviceIp")
    val deviceIp: String,

    /**
     * Идентификатор устройства
     */
    @SerialName("deviceId")
    val deviceId: String,

    /**
     * Имя устройства
     */
    @SerialName("deviceName")
    val deviceName: String
)

/**
 * DTO для ответа на запрос аутентификации
 */
@Serializable
data class AuthResponseDto(
    /**
     * Имя пользователя
     */
    @SerialName("name")
    val name: String,

    /**
     * Идентификатор пользователя
     */
    @SerialName("id")
    val id: String,

    /**
     * Идентификатор группы пользователя
     */
    @SerialName("userGroupId")
    val userGroupId: String
)

/**
 * DTO для ошибки аутентификации
 */
@Serializable
data class AuthErrorDto(
    /**
     * Код ошибки
     */
    @SerialName("code")
    val code: String,

    /**
     * Сообщение об ошибке
     */
    @SerialName("message")
    val message: String
)