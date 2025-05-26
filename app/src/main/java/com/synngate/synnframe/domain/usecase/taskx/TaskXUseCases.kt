package com.synngate.synnframe.domain.usecase.taskx

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.usecase.BaseUseCase

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
}