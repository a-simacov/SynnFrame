package com.synngate.synnframe.domain.usecase.taskx

import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.service.TaskContextManager
import com.synngate.synnframe.domain.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.time.LocalDateTime

class TaskXUseCases(
    private val taskXRepository: TaskXRepository,
    private val taskContextManager: TaskContextManager
) : BaseUseCase {

    fun getFilteredTasks(
        nameFilter: String? = null,
        statusFilter: List<TaskXStatus>? = null,
        typeFilter: List<String>? = null,
        dateFromFilter: LocalDateTime? = null,
        dateToFilter: LocalDateTime? = null,
        executorIdFilter: String? = null
    ): Flow<List<TaskX>> {
        return flow {
            val task = taskContextManager.lastStartedTaskX.value ?: return@flow emit(emptyList())

            val matchesName = nameFilter == null || task.name.contains(nameFilter, ignoreCase = true)
            val matchesStatus = statusFilter == null || task.status in statusFilter
            val matchesType = typeFilter == null || task.taskTypeId in typeFilter
            val matchesDateFrom = dateFromFilter == null || task.createdAt.isAfter(dateFromFilter)
            val matchesDateTo = dateToFilter == null || task.createdAt.isBefore(dateToFilter)
            val matchesExecutor = executorIdFilter == null || task.executorId == executorIdFilter

            if (matchesName && matchesStatus && matchesType && matchesDateFrom && matchesDateTo && matchesExecutor) {
                emit(listOf(task))
            } else {
                emit(emptyList())
            }
        }
    }

    fun getTaskById(id: String): TaskX? {
        val task = taskContextManager.lastStartedTaskX.value
        return if (task?.id == id) task else null
    }

    fun verifyTask(id: String, barcode: String): Result<Boolean> {
        val task = taskContextManager.lastStartedTaskX.value
        if (task == null || task.id != id) {
            return Result.failure(IllegalArgumentException("Задание не найдено: $id"))
        }

        val isVerified = task.barcode == barcode

        if (isVerified) {
            val updatedTask = task.copy(isVerified = true, lastModifiedAt = LocalDateTime.now())
            taskContextManager.updateTask(updatedTask)
        }

        return Result.success(isVerified)
    }

    suspend fun startTask(id: String, executorId: String): Result<TaskX> {
        val endpoint = taskContextManager.currentEndpoint.value
            ?: return Result.failure(IllegalStateException("Не найден endpoint для задания"))

        return taskXRepository.startTask(id, executorId, endpoint)
    }

    suspend fun completeTask(id: String): Result<TaskX> {
        val endpoint = taskContextManager.currentEndpoint.value
            ?: return Result.failure(IllegalStateException("Не найден endpoint для задания"))

        return taskXRepository.finishTask(id, endpoint)
    }

    suspend fun pauseTask(id: String): Result<TaskX> {
        val endpoint = taskContextManager.currentEndpoint.value
            ?: return Result.failure(IllegalStateException("Не найден endpoint для задания"))

        return taskXRepository.pauseTask(id, endpoint)
    }

    fun getTaskType(): TaskTypeX? {
        return taskContextManager.lastTaskTypeX.value
    }
}