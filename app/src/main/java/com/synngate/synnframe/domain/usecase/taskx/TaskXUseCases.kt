package com.synngate.synnframe.domain.usecase.taskx

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.dto.CommonResponseDto
import com.synngate.synnframe.data.remote.dto.PlannedActionStatusRequestDto
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.usecase.BaseUseCase
import java.time.LocalDateTime

class TaskXUseCases(
    private val taskXRepository: TaskXRepository,
) : BaseUseCase {

    suspend fun startTask(id: String, executorId: String, endpoint: String): Result<TaskX> {
        return taskXRepository.startTask(id, executorId, endpoint)
    }

    suspend fun completeTask(id: String, endpoint: String): Result<TaskX> {
        return taskXRepository.finishTask(id, endpoint)
    }

    suspend fun pauseTask(id: String, endpoint: String): Result<TaskX> {
        return taskXRepository.pauseTask(id, endpoint)
    }

    suspend fun setPlannedActionStatus(
        id: String,
        actionId: String,
        completed: Boolean,
        endpoint: String
    ): ApiResult<CommonResponseDto> {
        val requestDto = PlannedActionStatusRequestDto.fromPlannedAction(
            plannedActionId = actionId,
            manuallyCompleted = completed,
            manuallyCompletedAt = if (completed) LocalDateTime.now() else null
        )

        return taskXRepository.setPlannedActionStatus(id, requestDto, endpoint)
    }
}