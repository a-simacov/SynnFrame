package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequestDto(
    @SerialName("deviceIp")
    val deviceIp: String,

    @SerialName("deviceId")
    val deviceId: String,

    @SerialName("deviceName")
    val deviceName: String
)

@Serializable
data class AuthResponseDto(
    @SerialName("name")
    val name: String,

    @SerialName("id")
    val id: String,

    @SerialName("userGroupId")
    val userGroupId: String
)