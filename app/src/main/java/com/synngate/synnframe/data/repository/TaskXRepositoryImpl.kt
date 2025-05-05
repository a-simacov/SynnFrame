package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.TaskXApi
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.service.TaskContextManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.time.LocalDateTime

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

    override suspend fun updateTask(task: TaskX) {
        taskContextManager.updateTask(task)
    }

    override suspend fun startTask(id: String, executorId: String, endpoint: String): Result<TaskX> {
        try {
            val result = taskXApi.startTask(id, endpoint)
            return when (result) {
                is ApiResult.Success -> {
                    val task = taskContextManager.lastStartedTaskX.value
                    if (task != null && task.id == id) {
                        val updatedTask = task.copy(
                            status = TaskXStatus.IN_PROGRESS,
                            executorId = executorId,
                            startedAt = LocalDateTime.now(),
                            lastModifiedAt = LocalDateTime.now()
                        )
                        taskContextManager.updateTask(updatedTask)
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
                    val task = taskContextManager.lastStartedTaskX.value
                    if (task != null && task.id == id) {
                        val updatedTask = task.copy(
                            status = TaskXStatus.PAUSED,
                            lastModifiedAt = LocalDateTime.now()
                        )
                        taskContextManager.updateTask(updatedTask)
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
                    val task = taskContextManager.lastStartedTaskX.value
                    if (task != null && task.id == id) {
                        val updatedTask = task.copy(
                            status = TaskXStatus.COMPLETED,
                            completedAt = LocalDateTime.now(),
                            lastModifiedAt = LocalDateTime.now()
                        )
                        taskContextManager.updateTask(updatedTask)
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
            Timber.e(e, "Error finishing task: $id")
            return Result.failure(e)
        }
    }

    /**
     * Добавляет фактическое действие к заданию
     * @param factAction Фактическое действие
     * @param endpoint Endpoint API
     * @param finalizePlannedAction Завершить плановое действие (true) или только создать факт (false)
     * @return Результат операции - обновленное задание
     */
    override suspend fun addFactAction(
        factAction: FactAction,
        endpoint: String,
        finalizePlannedAction: Boolean
    ): Result<TaskX> {
        try {
            val result = taskXApi.addFactAction(factAction.taskId, factAction, endpoint, finalizePlannedAction)
            return when (result) {
                is ApiResult.Success -> {
                    val task = taskContextManager.lastStartedTaskX.value
                    if (task != null && task.id == factAction.taskId) {
                        val updatedFactActions = task.factActions.toMutableList()
                        updatedFactActions.add(factAction)

                        // Обновляем запланированные действия в зависимости от флага finalizePlannedAction
                        val updatedPlannedActions = task.plannedActions.map {
                            if (it.id == factAction.plannedActionId && finalizePlannedAction) {
                                // Если нужно завершить действие - помечаем его как выполненное
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

                        taskContextManager.updateTask(updatedTask)
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
            Timber.e(e, "Error adding fact action: ${factAction.id}")
            return Result.failure(e)
        }
    }
}