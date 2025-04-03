package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.local.dao.FactLineActionDao
import com.synngate.synnframe.data.local.dao.TaskTypeDao
import com.synngate.synnframe.data.local.entity.FactLineActionEntity
import com.synngate.synnframe.data.local.entity.TaskTypeEntity
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.TaskTypeApi
import com.synngate.synnframe.domain.entity.TaskType
import com.synngate.synnframe.domain.repository.TaskTypeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class TaskTypeRepositoryImpl(
    private val taskTypeDao: TaskTypeDao,
    private val factLineActionDao: FactLineActionDao,
    private val taskTypeApi: TaskTypeApi
) : TaskTypeRepository {
    override fun getTaskTypes(): Flow<List<TaskType>> {
        return taskTypeDao.getAllTaskTypes().map { taskTypeEntities ->
            taskTypeEntities.map { entity ->
                val factActions = factLineActionDao.getActionsForTaskTypeSync(entity.id)
                entity.toDomainModel(factActions)
            }
        }
    }

    override suspend fun getTaskTypeById(id: String): TaskType? {
        val taskTypeEntity = taskTypeDao.getTaskTypeById(id) ?: return null
        val factActions = factLineActionDao.getActionsForTaskTypeSync(taskTypeEntity.id)
        return taskTypeEntity.toDomainModel(factActions)
    }

    override suspend fun syncTaskTypes(): Result<Int> {
        return try {
            val result = taskTypeApi.getTaskTypes()

            when (result) {
                is ApiResult.Success -> {
                    val taskTypes = result.data ?: emptyList()

                    // Сохраняем типы заданий в БД
                    val taskTypeEntities = taskTypes.map { TaskTypeEntity.fromDomainModel(it) }
                    taskTypeDao.insertTaskTypes(taskTypeEntities)

                    // Сохраняем действия для строки факта
                    taskTypes.forEach { taskType ->
                        factLineActionDao.deleteActionsForTaskType(taskType.id)

                        val factActionEntities = taskType.factLineActions.map { action ->
                            FactLineActionEntity.fromDomainModel(taskType.id, action)
                        }

                        factLineActionDao.insertFactLineActions(factActionEntities)
                    }

                    Result.success(taskTypes.size)
                }
                is ApiResult.Error -> {
                    Timber.e("Failed to sync task types: ${result.message}")
                    Result.failure(Exception("Failed to sync task types: ${result.message}"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during task types sync")
            Result.failure(e)
        }
    }

    override suspend fun deleteAllTaskTypes() {
        taskTypeDao.deleteAllTaskTypes()
    }

    override suspend fun insertTaskTypes(taskTypes: List<TaskTypeEntity>) {
        taskTypeDao.insertTaskTypes(taskTypes)
    }

    override suspend fun deleteActionsForTaskType(taskTypeId: String) {
        factLineActionDao.deleteActionsForTaskType(taskTypeId)
    }

    override suspend fun insertFactLineActions(actions: List<FactLineActionEntity>) {
        factLineActionDao.insertFactLineActions(actions)
    }
}