package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.local.dao.TaskDao
import com.synngate.synnframe.data.local.entity.TaskEntity
import com.synngate.synnframe.data.local.entity.TaskFactLineEntity
import com.synngate.synnframe.data.local.entity.TaskPlanLineEntity
import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.api.TaskApi
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDateTime

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val taskApi: TaskApi
) : TaskRepository {

    override fun getTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { taskEntities ->
            buildTasksWithDetails(taskEntities)
        }
    }

    override fun getFilteredTasks(
        nameFilter: String?,
        statusFilter: List<TaskStatus>?,
        typeFilter: List<String>?,
        dateFromFilter: LocalDateTime?,
        dateToFilter: LocalDateTime?,
        executorIdFilter: String?
    ): Flow<List<Task>> {
        val hasStatusFilter = !statusFilter.isNullOrEmpty()
        val hasTypeFilter = !typeFilter.isNullOrEmpty()
        val hasDateFilter = dateFromFilter != null && dateToFilter != null
        val hasExecutorFilter = !executorIdFilter.isNullOrEmpty()

        val statusStrings = statusFilter?.map { it.name } ?: emptyList()
        val typesStrings = typeFilter ?: emptyList()

        return taskDao.getFilteredTasks(
            nameFilter = nameFilter ?: "",
            hasStatusFilter = hasStatusFilter,
            statuses = statusStrings,
            hasTypeFilter = hasTypeFilter,
            types = typesStrings,
            hasDateFilter = hasDateFilter,
            dateFrom = dateFromFilter ?: LocalDateTime.MIN,
            dateTo = dateToFilter ?: LocalDateTime.MAX,
            hasExecutorFilter = hasExecutorFilter,
            executorId = executorIdFilter ?: ""
        ).map { taskEntities ->
            buildTasksWithDetails(taskEntities)
        }
    }

    override suspend fun getTaskById(id: String): Task? {
        val taskEntity = taskDao.getTaskById(id) ?: return null
        return buildTaskWithDetails(taskEntity)
    }

    override suspend fun getTaskByBarcode(barcode: String): Task? {
        val taskEntity = taskDao.getTaskByBarcode(barcode) ?: return null
        return buildTaskWithDetails(taskEntity)
    }

    override fun getTasksCountForCurrentUser(): Flow<Int> {
        return taskDao.getTasksCountByStatuses(
            statuses = listOf(TaskStatus.TO_DO.name, TaskStatus.IN_PROGRESS.name)
        )
    }

    override suspend fun addTask(task: Task) {
        val taskEntity = TaskEntity.fromDomainModel(task)
        taskDao.insertTask(taskEntity)

        for (planLine in task.planLines) {
            val planLineEntity = TaskPlanLineEntity.fromDomainModel(planLine)
            taskDao.insertTaskPlanLine(planLineEntity)
        }

        for (factLine in task.factLines) {
            val factLineEntity = TaskFactLineEntity.fromDomainModel(factLine)
            taskDao.insertTaskFactLine(factLineEntity)
        }
    }

    override suspend fun addTasks(tasks: List<Task>) {
        for (task in tasks) {
            addTask(task)
        }
    }

    override suspend fun updateTask(task: Task) {
        val taskEntity = TaskEntity.fromDomainModel(task)
        taskDao.updateTask(taskEntity)

        for (planLine in task.planLines) {
            val planLineEntity = TaskPlanLineEntity.fromDomainModel(planLine)
            taskDao.insertTaskPlanLine(planLineEntity)
        }

        for (factLine in task.factLines) {
            val factLineEntity = TaskFactLineEntity.fromDomainModel(factLine)
            taskDao.insertTaskFactLine(factLineEntity)
        }
    }

    override suspend fun deleteTask(id: String) {
        taskDao.deleteTaskById(id)
    }

    override suspend fun setTaskStatus(id: String, status: TaskStatus) {
        val task = taskDao.getTaskById(id)
        if (task != null) {
            val updatedTask = task.copy(
                status = status.name,
                startedAt = if (status == TaskStatus.IN_PROGRESS && task.startedAt == null) {
                    LocalDateTime.now()
                } else {
                    task.startedAt
                },
                completedAt = if (status == TaskStatus.COMPLETED && task.completedAt == null) {
                    LocalDateTime.now()
                } else {
                    task.completedAt
                }
            )

            taskDao.updateTask(updatedTask)
        }
    }

    override suspend fun updateTaskFactLine(factLine: TaskFactLine) {
        val existingFactLine =
            taskDao.getTaskFactLineByTaskAndProduct(factLine.taskId, factLine.productId)

        if (existingFactLine != null) {
            val updatedFactLine = existingFactLine.copy(
                quantity = factLine.quantity
            )
            taskDao.updateTaskFactLine(updatedFactLine)
        } else {
            val newFactLine = TaskFactLineEntity.fromDomainModel(factLine)
            taskDao.insertTaskFactLine(newFactLine)
        }
    }

    override suspend fun checkTaskAvailability(id: String): Result<Boolean> {
        return try {
            val response = taskApi.checkTaskAvailability(id)
            when (response) {
                is ApiResult.Success -> Result.success(response.getOrNull()?.available ?: false)
                is ApiResult.Error -> Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadTaskToServer(id: String): Result<Boolean> {
        try {
            val taskEntity = taskDao.getTaskById(id) ?:
            return Result.failure(Exception("Task not found"))

            val task = buildTaskWithDetails(taskEntity)

            val response = taskApi.uploadTask(id, task)

            when (response) {
                is ApiResult.Success -> {
                    val updatedTask = TaskEntity.fromDomainModel(
                        task.copy(
                            uploaded = true,
                            uploadedAt = LocalDateTime.now()
                        )
                    )
                    taskDao.updateTask(updatedTask)
                    return Result.success(true)
                }

                is ApiResult.Error -> {
                    Timber.e("Failed to upload task: Code=${response.code}, Message=${response.message}")
                    return Result.failure(Exception(response.message))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during task upload: $id")
            return Result.failure(e)
        }
    }

    override suspend fun getCompletedNotUploadedTasks(): List<Task> {
        val taskEntities = taskDao.getCompletedNotUploadedTasks()
        return buildTasksWithDetails(taskEntities)
    }

    override suspend fun getTasksFromServer(): Result<List<Task>> {
        return try {
            val response = taskApi.getTasks()
            when (response) {
                is ApiResult.Success -> Result.success(response.getOrNull() ?: emptyList())
                is ApiResult.Error -> Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun buildTaskWithDetails(taskEntity: TaskEntity): Task {
        val planLines = taskDao.getPlanLinesForTask(taskEntity.id).map { it.toDomainModel() }
        val factLines = taskDao.getFactLinesForTask(taskEntity.id).map { it.toDomainModel() }

        return taskEntity.toDomainModel(planLines, factLines)
    }

    private suspend fun buildTasksWithDetails(taskEntities: List<TaskEntity>): List<Task> {
        if (taskEntities.isEmpty()) return emptyList()

        val taskIds = taskEntities.map { it.id }

        val allPlanLines = taskDao.getPlanLinesForTasks(taskIds)
        val allFactLines = taskDao.getFactLinesForTasks(taskIds)

        val planLinesMap = allPlanLines.groupBy { it.taskId }
        val factLinesMap = allFactLines.groupBy { it.taskId }

        return taskEntities.map { taskEntity ->
            val planLines = planLinesMap[taskEntity.id]?.map { it.toDomainModel() } ?: emptyList()
            val factLines = factLinesMap[taskEntity.id]?.map { it.toDomainModel() } ?: emptyList()

            taskEntity.toDomainModel(planLines, factLines)
        }
    }
}