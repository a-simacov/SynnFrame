package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.TaskType
import kotlinx.coroutines.flow.Flow

interface TaskTypeRepository {
    fun getTaskTypes(): Flow<List<TaskType>>
    suspend fun getTaskTypeById(id: String): TaskType?
    suspend fun syncTaskTypes(): Result<Int>
}