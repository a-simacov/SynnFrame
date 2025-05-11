package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.dto.CommonResponseDto
import com.synngate.synnframe.data.remote.dto.PlannedActionStatusRequestDto
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import kotlinx.coroutines.flow.Flow

interface TaskXRepository {

    fun getTasks(): Flow<List<TaskX>>

    suspend fun getTaskById(id: String): TaskX?

    suspend fun updateTask(task: TaskX)

    suspend fun startTask(id: String, executorId: String, endpoint: String): Result<TaskX>

    suspend fun pauseTask(id: String, endpoint: String): Result<TaskX>

    suspend fun finishTask(id: String, endpoint: String): Result<TaskX>

    suspend fun addFactAction(factAction: FactAction, endpoint: String): Result<TaskX>

    suspend fun setPlannedActionStatus(
        taskId: String,
        requestDto: PlannedActionStatusRequestDto,
        endpoint: String
    ): ApiResult<CommonResponseDto>
}