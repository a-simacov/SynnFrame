package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskStartRequestDto(
    @SerialName("taskId")
    val taskId: String,

    @SerialName("start")
    val start: Boolean = true
)