package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.TaskXApi
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.service.TaskContextManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class TaskXRepositoryImpl(
    private val taskXApi: TaskXApi,
    private val taskContextManager: TaskContextManager
) : TaskXRepository {

    override fun getTasks(): Flow<List<TaskX>> {
        return flow {
            val task = taskContextManager.lastStartedTaskX.value
            if (task != null) {
                emit(listOf(task))
            } else {
                emit(emptyList())
            }
        }
    }

    override suspend fun getTaskById(id: String): TaskX? {
        return taskContextManager.lastStartedTaskX.value?.takeIf { it.id == id }
    }

    override suspend fun startTask(id: String, executorId: String, endpoint: String): Result<TaskX> {
        try {
            val result = taskXApi.startTask(id, endpoint)
            return when (result) {
                is ApiResult.Success -> {
                    val responseDto = result.data
                    if (responseDto.success && responseDto.task != null) {
                        // Обновляем задание в контекстном менеджере
                        taskContextManager.updateTask(responseDto.task)
                        Result.success(responseDto.task)
                    } else {
                        Result.failure(Exception(responseDto.message ?: "Unknown error"))
                    }
                }
                is ApiResult.Error -> {
                    Result.failure(Exception(result.message))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error starting task: $id")
            return Result.failure(e)
        }
    }

    override suspend fun pauseTask(id: String, endpoint: String): Result<TaskX> {
        try {
            val result = taskXApi.pauseTask(id, endpoint)
            return when (result) {
                is ApiResult.Success -> {
                    val responseDto = result.data
                    if (responseDto.success && responseDto.task != null) {
                        // Обновляем задание в контекстном менеджере
                        taskContextManager.updateTask(responseDto.task)
                        Result.success(responseDto.task)
                    } else {
                        Result.failure(Exception(responseDto.message ?: "Unknown error"))
                    }
                }
                is ApiResult.Error -> {
                    Result.failure(Exception(result.message))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error pausing task: $id")
            return Result.failure(e)
        }
    }

    override suspend fun finishTask(id: String, endpoint: String): Result<TaskX> {
        try {
            val result = taskXApi.finishTask(id, endpoint)
            return when (result) {
                is ApiResult.Success -> {
                    val responseDto = result.data
                    if (responseDto.success && responseDto.task != null) {
                        // Обновляем задание в контекстном менеджере
                        taskContextManager.updateTask(responseDto.task)
                        Result.success(responseDto.task)
                    } else {
                        Result.failure(Exception(responseDto.message ?: "Unknown error"))
                    }
                }
                is ApiResult.Error -> {
                    Result.failure(Exception(result.message))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error finishing task: $id")
            return Result.failure(e)
        }
    }

    override suspend fun addFactAction(
        factAction: FactAction,
        endpoint: String
    ): Result<TaskX> {
        try {
            val result = taskXApi.addFactAction(factAction.taskId, factAction, endpoint)
            return when (result) {
                is ApiResult.Success -> {
                    val responseDto = result.data
                    if (responseDto.success && responseDto.task != null) {
                        // Обновляем задание в контекстном менеджере
                        taskContextManager.updateTask(responseDto.task)
                        Result.success(responseDto.task)
                    } else {
                        Result.failure(Exception(responseDto.message ?: "Unknown error"))
                    }
                }
                is ApiResult.Error -> {
                    Result.failure(Exception(result.message))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error adding fact action: ${factAction.id}")
            return Result.failure(e)
        }
    }
}