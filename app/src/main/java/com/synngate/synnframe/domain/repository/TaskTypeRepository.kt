package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.data.local.entity.FactLineActionEntity
import com.synngate.synnframe.data.local.entity.TaskTypeEntity
import com.synngate.synnframe.domain.entity.TaskType
import kotlinx.coroutines.flow.Flow

interface TaskTypeRepository {
    fun getTaskTypes(): Flow<List<TaskType>>
    suspend fun getTaskTypeById(id: String): TaskType?
    suspend fun syncTaskTypes(): Result<Int>

    suspend fun deleteAllTaskTypes()
    suspend fun insertTaskTypes(taskTypes: List<TaskTypeEntity>)
    suspend fun deleteActionsForTaskType(taskTypeId: String)
    suspend fun insertFactLineActions(actions: List<FactLineActionEntity>)
}