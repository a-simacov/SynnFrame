package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.TaskAvailabilityResponseDto
import com.synngate.synnframe.domain.entity.Task

interface TaskApi {

    suspend fun getTasks(): ApiResult<List<Task>>

    suspend fun checkTaskAvailability(taskId: String): ApiResult<TaskAvailabilityResponseDto>

    suspend fun uploadTask(taskId: String, task: Task): ApiResult<Unit>
}