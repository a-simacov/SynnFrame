package com.synngate.synnframe.domain.usecase.taskx

import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.usecase.BaseUseCase

class TaskXUseCases(
    private val taskXRepository: TaskXRepository,
) : BaseUseCase {

    suspend fun startTask(id: String, executorId: String): Result<TaskX> {
        val endpoint = ""
            ?: return Result.failure(IllegalStateException("Не найден endpoint для задания"))

        return taskXRepository.startTask(id, executorId, endpoint)
    }

    suspend fun completeTask(id: String): Result<TaskX> {
        val endpoint = ""
            ?: return Result.failure(IllegalStateException("Не найден endpoint для задания"))

        return taskXRepository.finishTask(id, endpoint)
    }

    suspend fun pauseTask(id: String): Result<TaskX> {
        val endpoint = ""
            ?: return Result.failure(IllegalStateException("Не найден endpoint для задания"))

        return taskXRepository.pauseTask(id, endpoint)
    }

    fun getTaskType(): TaskTypeX? {
        return null
    }
}