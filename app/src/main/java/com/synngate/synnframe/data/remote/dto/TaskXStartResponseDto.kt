package com.synngate.synnframe.data.remote.dto

import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskXStartResponseDto(
    @SerialName("task")
    val task: TaskX,

    @SerialName("taskType")
    val taskType: TaskTypeX
)