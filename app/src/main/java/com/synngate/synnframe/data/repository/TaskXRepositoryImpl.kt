package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.TaskXApi
import com.synngate.synnframe.data.remote.dto.CommonResponseDto
import com.synngate.synnframe.data.remote.dto.PlannedActionStatusRequestDto
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.presentation.navigation.TaskXDataHolderSingleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.time.LocalDateTime

class TaskXRepositoryImpl(
    private val taskXApi: TaskXApi,
) : TaskXRepository {

    override fun getTasks(): Flow<List<TaskX>> {
        return flow {
//            val task = taskContextManager.lastStartedTaskX.value
//            if (task != null) {
//                emit(listOf(task))
//            } else {
//                emit(emptyList())
//            }
        }
    }

    override suspend fun getTaskById(id: String): TaskX? {
        return null
    }

    override suspend fun updateTask(task: TaskX) {

    }

    override suspend fun startTask(id: String, executorId: String, endpoint: String): Result<TaskX> {
        try {
            val result = taskXApi.startTask(id, endpoint)
            return when (result) {
                is ApiResult.Success -> {
                    val task = TaskXDataHolderSingleton.currentTask.value
                    if (task != null && task.id == id) {
                        val updatedTask = task.copy(
                            status = TaskXStatus.IN_PROGRESS,
                            executorId = executorId,
                            startedAt = LocalDateTime.now(),
                            lastModifiedAt = LocalDateTime.now()
                        )
                        TaskXDataHolderSingleton.updateTask(updatedTask)
                        Result.success(updatedTask)
                    } else {
                        Result.failure(Exception("Task not found in context"))
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
                    val task = TaskXDataHolderSingleton.currentTask.value
                    if (task != null && task.id == id) {
                        val updatedTask = task.copy(
                            status = TaskXStatus.PAUSED,
                            lastModifiedAt = LocalDateTime.now()
                        )
                        TaskXDataHolderSingleton.updateTask(updatedTask)
                        Result.success(updatedTask)
                    } else {
                        Result.failure(Exception("Task not found in context"))
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
                    val task = TaskXDataHolderSingleton.currentTask.value
                    if (task != null && task.id == id) {
                        val updatedTask = task.copy(
                            status = TaskXStatus.COMPLETED,
                            completedAt = LocalDateTime.now(),
                            lastModifiedAt = LocalDateTime.now()
                        )
                        TaskXDataHolderSingleton.updateTask(updatedTask)
                        Result.success(updatedTask)
                    } else {
                        Timber.e("Task not found in TaskXDataHolderSingleton for id: $id")
                        Result.failure(Exception("Task not found in context"))
                    }
                }
                is ApiResult.Error -> {
                    Timber.e("Error finishing task $id: ${result.message}")
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
                    val task = TaskXDataHolderSingleton.currentTask.value

                    if (task != null && task.id == factAction.taskId) {
                        val updatedFactActions = task.factActions.toMutableList()
                        updatedFactActions.add(factAction)

                        val updatedPlannedActions = task.plannedActions.map {
                            if (it.id == factAction.plannedActionId) {
                                it.copy(isCompleted = true)
                            } else if (factAction.plannedActionId == null &&
                                it.storageProduct?.product?.id == factAction.storageProduct?.product?.id) {
                                it.copy(isCompleted = true)
                            } else {
                                it
                            }
                        }

                        val updatedTask = task.copy(
                            plannedActions = updatedPlannedActions,
                            factActions = updatedFactActions,
                            lastModifiedAt = LocalDateTime.now()
                        )

                        Result.success(updatedTask)
                    } else {
                        Timber.e("Task not found in TaskXDataHolderSingleton for factAction: ${factAction.id}")
                        Result.failure(Exception("Task not found in context"))
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

    override suspend fun setPlannedActionStatus(
        taskId: String,
        requestDto: PlannedActionStatusRequestDto,
        endpoint: String
    ): ApiResult<CommonResponseDto> {
        try {
            return taskXApi.setPlannedActionStatus(taskId, requestDto, endpoint)
        } catch (e: Exception) {
            Timber.e(e, "Error setting planned action status for taskId: $taskId, actionId: ${requestDto.plannedActionId}")
            return ApiResult.Error(
                code = 500,
                message = "Error setting planned action status: ${e.message}"
            )
        }
    }
}