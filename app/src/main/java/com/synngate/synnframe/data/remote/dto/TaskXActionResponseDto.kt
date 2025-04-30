package com.synngate.synnframe.data.remote.dto

import com.synngate.synnframe.domain.entity.taskx.TaskX
import kotlinx.serialization.Serializable

@Serializable
data class TaskXActionResponseDto(
    val success: Boolean,
    val message: String? = null,
    val task: TaskX? = null
)