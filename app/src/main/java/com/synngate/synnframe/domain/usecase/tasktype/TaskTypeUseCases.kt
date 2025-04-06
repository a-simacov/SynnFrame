package com.synngate.synnframe.domain.usecase.tasktype

import com.synngate.synnframe.domain.entity.TaskType
import com.synngate.synnframe.domain.repository.TaskTypeRepository
import com.synngate.synnframe.domain.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class TaskTypeUseCases(
    private val taskTypeRepository: TaskTypeRepository
) : BaseUseCase {
    fun getTaskTypes(): Flow<List<TaskType>> {
        return taskTypeRepository.getTaskTypes()
    }

    suspend fun getTaskTypeById(id: String): TaskType? {
        return taskTypeRepository.getTaskTypeById(id)
    }

    suspend fun syncTaskTypes(): Result<Int> {
        return try {
            val result = taskTypeRepository.syncTaskTypes()

            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                Timber.i("Synced $count task types")
            } else {
                val error = result.exceptionOrNull()
                Timber.e("Sync task types error: ${error?.message}")
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "Error syncing task types")
            Result.failure(e)
        }
    }
}