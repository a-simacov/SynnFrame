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
import com.synngate.synnframe.domain.entity.TaskType
import com.synngate.synnframe.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/**
 * Имплементация репозитория заданий
 * Отвечает только за операции с данными заданий, без бизнес-логики
 */
class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val taskApi: TaskApi
) : TaskRepository {

    override fun getTasks(): Flow<List<Task>> {
        return taskDao.getAllTasksWithDetails().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getFilteredTasks(
        nameFilter: String?,
        statusFilter: List<TaskStatus>?,
        typeFilter: TaskType?,
        dateFromFilter: LocalDateTime?,
        dateToFilter: LocalDateTime?,
        executorIdFilter: String?
    ): Flow<List<Task>> {
        // Преобразуем фильтры в необходимый формат для DAO
        val hasNameFilter = !nameFilter.isNullOrEmpty()
        val hasStatusFilter = !statusFilter.isNullOrEmpty()
        val hasTypeFilter = typeFilter != null
        val hasDateFilter = dateFromFilter != null && dateToFilter != null
        val hasExecutorFilter = !executorIdFilter.isNullOrEmpty()

        // Преобразуем статусы в строки
        val statusStrings = statusFilter?.map { it.name } ?: emptyList()

        // Получаем отфильтрованные задания
        return taskDao.getFilteredTasks(
            nameFilter = nameFilter ?: "",
            hasStatusFilter = hasStatusFilter,
            statuses = statusStrings,
            hasTypeFilter = hasTypeFilter,
            type = typeFilter?.name ?: "",
            hasDateFilter = hasDateFilter,
            dateFrom = dateFromFilter ?: LocalDateTime.MIN,
            dateTo = dateToFilter ?: LocalDateTime.MAX,
            hasExecutorFilter = hasExecutorFilter,
            executorId = executorIdFilter ?: ""
        ).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getTaskById(id: String): Task? {
        return taskDao.getTaskWithDetailsById(id)?.toDomainModel()
    }

    override suspend fun getTaskByBarcode(barcode: String): Task? {
        return taskDao.getTaskByBarcode(barcode)?.toDomainModel()
    }

    override fun getTasksCountForCurrentUser(): Flow<Int> {
        // Учитываем только задания, назначенные текущему пользователю
        return taskDao.getTasksCountByStatuses(
            statuses = listOf(TaskStatus.TO_DO.name, TaskStatus.IN_PROGRESS.name)
        )
    }

    override suspend fun addTask(task: Task) {
        // Вставляем основную информацию о задании
        val taskEntity = TaskEntity.fromDomainModel(task)
        taskDao.insertTask(taskEntity)

        // Вставляем строки плана задания
        for (planLine in task.planLines) {
            val planLineEntity = TaskPlanLineEntity.fromDomainModel(planLine)
            taskDao.insertTaskPlanLine(planLineEntity)
        }

        // Вставляем строки факта задания (если есть)
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
        // Обновляем основную информацию о задании
        val taskEntity = TaskEntity.fromDomainModel(task)
        taskDao.updateTask(taskEntity)

        // Обновляем строки плана задания
        for (planLine in task.planLines) {
            val planLineEntity = TaskPlanLineEntity.fromDomainModel(planLine)
            taskDao.insertTaskPlanLine(planLineEntity)
        }

        // Обновляем строки факта задания
        for (factLine in task.factLines) {
            val factLineEntity = TaskFactLineEntity.fromDomainModel(factLine)
            taskDao.insertTaskFactLine(factLineEntity)
        }
    }

    override suspend fun deleteTask(id: String) {
        taskDao.deleteTaskById(id)
    }

    override suspend fun setTaskStatus(id: String, status: TaskStatus) {
        val task = taskDao.getTaskWithDetailsById(id)
        if (task != null) {
            // Обновляем статус задания
            val updatedTask = task.task.copy(
                status = status.name,
                // Обновляем соответствующие даты в зависимости от статуса
                startedAt = if (status == TaskStatus.IN_PROGRESS && task.task.startedAt == null) {
                    LocalDateTime.now()
                } else {
                    task.task.startedAt
                },
                completedAt = if (status == TaskStatus.COMPLETED && task.task.completedAt == null) {
                    LocalDateTime.now()
                } else {
                    task.task.completedAt
                }
            )

            taskDao.updateTask(updatedTask)
        }
    }

    override suspend fun updateTaskFactLine(factLine: TaskFactLine) {
        // Проверяем, существует ли строка факта для данного товара
        val existingFactLine =
            taskDao.getTaskFactLineByTaskAndProduct(factLine.taskId, factLine.productId)

        if (existingFactLine != null) {
            // Обновляем существующую строку факта
            val updatedFactLine = existingFactLine.copy(
                quantity = factLine.quantity
            )
            taskDao.updateTaskFactLine(updatedFactLine)
        } else {
            // Создаем новую строку факта
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
            val task = taskDao.getTaskWithDetailsById(id)?.toDomainModel()
                ?: return Result.failure(Exception("Task not found"))

            // Отправляем задание на сервер
            val response = taskApi.uploadTask(id, task)

            when (response) {
                is ApiResult.Success -> {
                    // Обновляем статус выгрузки задания в БД
                    val updatedTask = TaskEntity.fromDomainModel(
                        task.copy(
                            uploaded = true,
                            uploadedAt = LocalDateTime.now()
                        )
                    )
                    taskDao.updateTask(updatedTask)
                    return Result.success(true)
                }

                is ApiResult.Error ->
                    return Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun getCompletedNotUploadedTasks(): List<Task> {
        return taskDao.getCompletedNotUploadedTasks().map { it.toDomainModel() }
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
}