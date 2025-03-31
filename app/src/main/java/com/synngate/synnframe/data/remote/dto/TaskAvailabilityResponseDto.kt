package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskAvailabilityResponseDto(
    @SerialName("available")
    val available: Boolean
)