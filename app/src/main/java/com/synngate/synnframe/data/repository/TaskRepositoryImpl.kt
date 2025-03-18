package com.synngate.synnframe.data.repository

import com.synngate.synnframe.data.local.dao.TaskDao
import com.synngate.synnframe.data.local.entity.TaskEntity
import com.synngate.synnframe.data.local.entity.TaskFactLineEntity
import com.synngate.synnframe.data.local.entity.TaskPlanLineEntity
import com.synngate.synnframe.data.remote.api.TaskApi
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.entity.TaskType
import com.synngate.synnframe.domain.repository.LogRepository
import com.synngate.synnframe.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

/**
 * Имплементация репозитория заданий
 */
class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val taskApi: TaskApi,
    private val logRepository: LogRepository
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

        logRepository.logInfo("Добавлено задание: ${task.name}")
    }

    override suspend fun addTasks(tasks: List<Task>) {
        for (task in tasks) {
            addTask(task)
        }
        logRepository.logInfo("Добавлено заданий: ${tasks.size}")
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

        logRepository.logInfo("Обновлено задание: ${task.name}")
    }

    override suspend fun deleteTask(id: String) {
        val task = taskDao.getTaskWithDetailsById(id)
        if (task != null) {
            // Удаляем задание (каскадно удалит строки плана и факта)
            taskDao.deleteTaskById(id)
            logRepository.logInfo("Удалено задание: ${task.task.name}")
        }
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
            logRepository.logInfo("Изменен статус задания '${task.task.name}' на ${status.name}")
        }
    }

    override suspend fun startTask(id: String, executorId: String): Result<Task> {
        val task = taskDao.getTaskWithDetailsById(id)
        if (task != null) {
            // Проверяем, что задание находится в статусе "К выполнению"
            if (task.task.status != TaskStatus.TO_DO.name) {
                logRepository.logWarning("Невозможно начать выполнение задания '${task.task.name}', текущий статус: ${task.task.status}")
                return Result.failure(IllegalStateException("Task is not in TO_DO status"))
            }

            // Проверяем, назначено ли задание исполнителю или не имеет исполнителя
            if (task.task.executorId != null && task.task.executorId != executorId) {
                logRepository.logWarning("Задание '${task.task.name}' назначено другому исполнителю: ${task.task.executorId}")
                return Result.failure(IllegalStateException("Task is assigned to another executor"))
            }

            // Если задание не имеет исполнителя, проверяем на сервере, не взял ли его кто-то другой
            if (task.task.executorId == null) {
                try {
                    val response = taskApi.checkTaskAvailability(id)
                    if (!response.isSuccessful || response.body()?.available != true) {
                        logRepository.logWarning("Задание '${task.task.name}' недоступно для выполнения")
                        return Result.failure(IllegalStateException("Task is not available"))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Exception during task availability check")
                    logRepository.logError("Ошибка при проверке доступности задания: ${e.message}")
                    // Если нет подключения к серверу, позволяем взять задание в работу
                    logRepository.logWarning("Нет подключения к серверу, задание взято в работу без проверки")
                }
            }

            // Обновляем задание
            val now = LocalDateTime.now()
            val updatedTask = task.task.copy(
                status = TaskStatus.IN_PROGRESS.name,
                startedAt = now,
                executorId = executorId
            )

            taskDao.updateTask(updatedTask)
            logRepository.logInfo("Начато выполнение задания: ${task.task.name}")

            // Возвращаем обновленное задание
            return Result.success(getTaskById(id)!!)
        } else {
            logRepository.logWarning("Задание с ID $id не найдено")
            return Result.failure(IllegalArgumentException("Task not found"))
        }
    }

    override suspend fun completeTask(id: String): Result<Task> {
        val task = taskDao.getTaskWithDetailsById(id)
        if (task != null) {
            // Проверяем, что задание находится в статусе "Выполняется"
            if (task.task.status != TaskStatus.IN_PROGRESS.name) {
                logRepository.logWarning("Невозможно завершить задание '${task.task.name}', текущий статус: ${task.task.status}")
                return Result.failure(IllegalStateException("Task is not in IN_PROGRESS status"))
            }

            // Обновляем задание
            val now = LocalDateTime.now()
            val updatedTask = task.task.copy(
                status = TaskStatus.COMPLETED.name,
                completedAt = now
            )

            taskDao.updateTask(updatedTask)
            logRepository.logInfo("Завершено выполнение задания: ${task.task.name}")

            // Пытаемся выгрузить задание на сервер
            try {
                uploadTaskToServer(id)
            } catch (e: Exception) {
                // Игнорируем ошибки выгрузки, так как задание можно будет выгрузить позже
                Timber.e(e, "Exception during task upload after completion")
                logRepository.logWarning("Ошибка выгрузки задания после завершения: ${e.message}")
            }

            // Возвращаем обновленное задание
            return Result.success(getTaskById(id)!!)
        } else {
            logRepository.logWarning("Задание с ID $id не найдено")
            return Result.failure(IllegalArgumentException("Task not found"))
        }
    }

    override suspend fun updateTaskFactLine(factLine: TaskFactLine) {
        // Проверяем, существует ли строка факта для данного товара
        val existingFactLine = taskDao.getTaskFactLineByTaskAndProduct(factLine.taskId, factLine.productId)

        if (existingFactLine != null) {
            // Обновляем существующую строку факта
            val updatedFactLine = existingFactLine.copy(
                quantity = factLine.quantity
            )
            taskDao.updateTaskFactLine(updatedFactLine)
        } else {
            // Создаем новую строку факта
            val newFactLine = TaskFactLineEntity(
                id = factLine.id.ifEmpty { UUID.randomUUID().toString() },
                taskId = factLine.taskId,
                productId = factLine.productId,
                quantity = factLine.quantity
            )
            taskDao.insertTaskFactLine(newFactLine)
        }

        logRepository.logInfo("Обновлена строка факта для задания ${factLine.taskId}, товар ${factLine.productId}, количество ${factLine.quantity}")
    }

    override suspend fun uploadTaskToServer(id: String): Result<Boolean> {
        val task = taskDao.getTaskWithDetailsById(id)
        if (task != null) {
            // Проверяем, что задание завершено
            if (task.task.status != TaskStatus.COMPLETED.name) {
                logRepository.logWarning("Невозможно выгрузить незавершенное задание '${task.task.name}'")
                return Result.failure(IllegalStateException("Task is not completed"))
            }

            try {
                // Преобразуем задание в доменную модель для отправки
                val domainTask = task.toDomainModel()

                // Отправляем задание на сервер
                val response = taskApi.uploadTask(id, domainTask)

                if (response.isSuccessful) {
                    // Обновляем статус выгрузки задания
                    val now = LocalDateTime.now()
                    val updatedTask = task.task.copy(
                        uploaded = true,
                        uploadedAt = now
                    )

                    taskDao.updateTask(updatedTask)
                    logRepository.logInfo("Задание успешно выгружено: ${task.task.name}")

                    return Result.success(true)
                } else {
                    // Извлекаем сообщение об ошибке из ответа
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    logRepository.logWarning("Ошибка выгрузки задания '${task.task.name}': $errorBody")

                    return Result.failure(Exception("Failed to upload task: $errorBody"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during task upload")
                logRepository.logError("Исключение при выгрузке задания '${task.task.name}': ${e.message}")

                return Result.failure(e)
            }
        } else {
            logRepository.logWarning("Задание с ID $id не найдено")
            return Result.failure(IllegalArgumentException("Task not found"))
        }
    }

    override suspend fun uploadCompletedTasksToServer(): Result<Int> {
        try {
            // Получаем все завершенные, но не выгруженные задания
            val completedTasks = taskDao.getCompletedNotUploadedTasks()

            if (completedTasks.isEmpty()) {
                logRepository.logInfo("Нет завершенных заданий для выгрузки")
                return Result.success(0)
            }

            var successCount = 0

            // Выгружаем каждое задание
            for (task in completedTasks) {
                val result = uploadTaskToServer(task.task.id)
                if (result.isSuccess) {
                    successCount++
                }
            }

            logRepository.logInfo("Выгружено заданий: $successCount из ${completedTasks.size}")
            return Result.success(successCount)
        } catch (e: Exception) {
            Timber.e(e, "Exception during completed tasks upload")
            logRepository.logError("Исключение при выгрузке завершенных заданий: ${e.message}")

            return Result.failure(e)
        }
    }

    override suspend fun syncTasksWithServer(): Result<Int> {
        try {
            val response = taskApi.getTasks()

            if (response.isSuccessful) {
                val tasks = response.body()

                if (tasks != null) {
                    // Добавляем новые задания и обновляем существующие
                    var addedCount = 0
                    var updatedCount = 0

                    for (task in tasks) {
                        val existingTask = getTaskById(task.id)

                        if (existingTask == null) {
                            // Добавляем новое задание
                            addTask(task)
                            addedCount++
                        } else {
                            // Обновляем существующее задание, если оно не в статусе выполняется или завершено
                            if (existingTask.status == TaskStatus.TO_DO) {
                                updateTask(task)
                                updatedCount++
                            }
                        }
                    }

                    logRepository.logInfo("Синхронизация заданий: добавлено $addedCount, обновлено $updatedCount")
                    return Result.success(addedCount + updatedCount)
                } else {
                    logRepository.logWarning("Пустой ответ при синхронизации заданий")
                    return Result.failure(Exception("Empty task synchronization response"))
                }
            } else {
                // Извлекаем сообщение об ошибке из ответа
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                logRepository.logWarning("Ошибка синхронизации заданий: $errorBody")

                return Result.failure(Exception("Failed to sync tasks: $errorBody"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during tasks synchronization")
            logRepository.logError("Исключение при синхронизации заданий: ${e.message}")

            return Result.failure(e)
        }
    }
}