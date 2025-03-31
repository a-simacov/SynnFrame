package com.synngate.synnframe.domain.usecase.task

import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.entity.TaskType
import com.synngate.synnframe.domain.repository.TaskRepository
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.time.LocalDateTime

class TaskUseCases(
    private val taskRepository: TaskRepository,
    private val loggingService: LoggingService
) : BaseUseCase {

    fun getTasks(): Flow<List<Task>> =
        taskRepository.getTasks()

    fun getFilteredTasks(
        nameFilter: String? = null,
        statusFilter: List<TaskStatus>? = null,
        typeFilter: List<TaskType>? = null,
        dateFromFilter: LocalDateTime? = null,
        dateToFilter: LocalDateTime? = null,
        executorIdFilter: String? = null
    ): Flow<List<Task>> {
        return taskRepository.getFilteredTasks(
            nameFilter, statusFilter, typeFilter,
            dateFromFilter, dateToFilter, executorIdFilter
        )
    }

    suspend fun getTaskById(id: String): Task? =
        taskRepository.getTaskById(id)

    suspend fun getTaskByBarcode(barcode: String): Task? =
        taskRepository.getTaskByBarcode(barcode)

    fun getTasksCountForCurrentUser(): Flow<Int> =
        taskRepository.getTasksCountForCurrentUser()

    suspend fun startTask(id: String, executorId: String): Result<Task> {
        try {
            val task = taskRepository.getTaskById(id) ?:
            return Result.failure(IllegalArgumentException("Task not found"))

            if (task.status != TaskStatus.TO_DO) {
                loggingService.logWarning("Невозможно начать выполнение задания '${task.name}', текущий статус: ${task.status}")
                return Result.failure(IllegalStateException("Task is not in TO_DO status"))
            }

            if (task.executorId != null && task.executorId != executorId) {
                loggingService.logWarning("Задание '${task.name}' назначено другому исполнителю: ${task.executorId}")
                return Result.failure(IllegalStateException("Task is assigned to another executor"))
            }

            if (task.executorId == null) {
                try {
                    val availabilityResult = taskRepository.checkTaskAvailability(id)
                    if (availabilityResult.isFailure || availabilityResult.getOrNull() != true) {
                        loggingService.logWarning("Задание '${task.name}' недоступно для выполнения")
                        return Result.failure(IllegalStateException("Task is not available"))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Exception during task availability check")
                    loggingService.logError("Ошибка при проверке доступности задания: ${e.message}")
                    // Если нет подключения к серверу, позволяем взять задание в работу
                    loggingService.logWarning("Нет подключения к серверу, задание взято в работу без проверки")
                }
            }

            val updatedTask = task.copy(
                status = TaskStatus.IN_PROGRESS,
                startedAt = LocalDateTime.now(),
                executorId = executorId
            )

            taskRepository.updateTask(updatedTask)
            loggingService.logInfo("Начато выполнение задания: ${task.name}")

            return Result.success(taskRepository.getTaskById(id)!!)
        } catch (e: Exception) {
            Timber.e(e, "Exception during starting task")
            loggingService.logError("Исключение при запуске задания: ${e.message}")
            return Result.failure(e)
        }
    }

    suspend fun completeTask(id: String): Result<Task> {
        try {
            val task = taskRepository.getTaskById(id) ?:
            return Result.failure(IllegalArgumentException("Task not found"))

            if (task.status != TaskStatus.IN_PROGRESS) {
                loggingService.logWarning("Невозможно завершить задание '${task.name}', текущий статус: ${task.status}")
                return Result.failure(IllegalStateException("Task is not in IN_PROGRESS status"))
            }

            val updatedTask = task.copy(
                status = TaskStatus.COMPLETED,
                completedAt = LocalDateTime.now()
            )

            taskRepository.updateTask(updatedTask)
            loggingService.logInfo("Завершено выполнение задания: ${task.name}")

            try {
                uploadTask(id)
            } catch (e: Exception) {
                Timber.e(e, "Exception during task upload after completion")
                loggingService.logWarning("Ошибка выгрузки задания после завершения: ${e.message}")
            }

            return Result.success(taskRepository.getTaskById(id)!!)
        } catch (e: Exception) {
            Timber.e(e, "Exception during completing task")
            loggingService.logError("Исключение при завершении задания: ${e.message}")
            return Result.failure(e)
        }
    }

    suspend fun updateTaskFactLine(factLine: TaskFactLine) {
        try {
            val task = taskRepository.getTaskById(factLine.taskId) ?:
            throw IllegalArgumentException("Task not found: ${factLine.taskId}")

            if (task.status != TaskStatus.IN_PROGRESS) {
                loggingService.logWarning("Невозможно обновить строку факта: задание не в статусе выполнения")
                throw IllegalStateException("Cannot update fact line: task is not in progress")
            }

            taskRepository.updateTaskFactLine(factLine)
            loggingService.logInfo("Обновлена строка факта для задания ${factLine.taskId}, товар ${factLine.productId}, количество ${factLine.quantity}")
        } catch (e: Exception) {
            Timber.e(e, "Error updating task fact line")
            loggingService.logError("Ошибка при обновлении строки факта: ${e.message}")
            throw e
        }
    }

    suspend fun uploadTask(id: String): Result<Boolean> {
        return try {
            val task = taskRepository.getTaskById(id) ?:
            return Result.failure(IllegalArgumentException("Task not found"))

            if (task.status != TaskStatus.COMPLETED) {
                loggingService.logWarning("Невозможно выгрузить незавершенное задание: ${task.name}")
                return Result.failure(IllegalStateException("Cannot upload incomplete task"))
            }

            val result = taskRepository.uploadTaskToServer(id)

            if (result.isSuccess) {
                loggingService.logInfo("Задание успешно выгружено: ${task.name}")
            } else {
                val error = result.exceptionOrNull()
                loggingService.logWarning("Ошибка выгрузки задания: ${task.name}, причина: ${error?.message}")

                // Для лучшей диагностики добавим стек трейс
                if (error != null) {
                    val stackTrace = error.stackTraceToString()
                    loggingService.logError("Стек вызовов ошибки: $stackTrace")
                }
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "Exception during task upload")
            loggingService.logError("Исключение при выгрузке задания: ${e.message}, стек: ${e.stackTraceToString()}")
            Result.failure(e)
        }
    }

    suspend fun uploadCompletedTasks(): Result<Int> {
        return try {
            // Получаем все завершенные, но не выгруженные задания
            val completedTasks = taskRepository.getCompletedNotUploadedTasks()

            if (completedTasks.isEmpty()) {
                loggingService.logInfo("Нет завершенных заданий для выгрузки")
                return Result.success(0)
            }

            var successCount = 0

            // Выгружаем каждое задание
            for (task in completedTasks) {
                val result = uploadTask(task.id)
                if (result.isSuccess) {
                    successCount++
                }
            }

            loggingService.logInfo("Выгружено заданий: $successCount из ${completedTasks.size}")
            Result.success(successCount)
        } catch (e: Exception) {
            Timber.e(e, "Exception during completed tasks upload")
            loggingService.logError("Исключение при выгрузке завершенных заданий: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun syncTasks(): Result<Int> {
        return try {
            // Получаем задания с сервера
            val serverTasksResult = taskRepository.getTasksFromServer()

            if (serverTasksResult.isFailure) {
                return Result.failure(serverTasksResult.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val serverTasks = serverTasksResult.getOrNull() ?: emptyList()

            // Добавляем новые задания и обновляем существующие
            var addedCount = 0
            var updatedCount = 0

            for (task in serverTasks) {
                val existingTask = taskRepository.getTaskById(task.id)

                if (existingTask == null) {
                    // Добавляем новое задание
                    taskRepository.addTask(task)
                    addedCount++
                } else {
                    // Обновляем существующее задание, если оно не в статусе выполняется или завершено
                    if (existingTask.status == TaskStatus.TO_DO) {
                        taskRepository.updateTask(task)
                        updatedCount++
                    }
                }
            }

            loggingService.logInfo("Синхронизация заданий: добавлено $addedCount, обновлено $updatedCount")
            Result.success(addedCount + updatedCount)
        } catch (e: Exception) {
            Timber.e(e, "Exception during tasks synchronization")
            loggingService.logError("Исключение при синхронизации заданий: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun addTask(task: Task): Result<Unit> {
        return try {
            // Проверяем, что задание имеет валидные данные
            if (task.id.isBlank()) {
                return Result.failure(IllegalArgumentException("Task ID cannot be empty"))
            }

            if (task.name.isBlank()) {
                return Result.failure(IllegalArgumentException("Task name cannot be empty"))
            }

            // Добавляем задание через репозиторий
            taskRepository.addTask(task)

            // Логируем операцию
            loggingService.logInfo("Добавлено новое задание: ${task.name} (${task.id})")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error adding task")
            loggingService.logError("Ошибка при добавлении задания: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getCompletedNotUploadedTasks(): Result<List<Task>> {
        return try {
            val tasks = taskRepository.getCompletedNotUploadedTasks()
            Result.success(tasks)
        } catch (e: Exception) {
            Timber.e(e, "Error getting completed not uploaded tasks")
            loggingService.logError("Ошибка получения невыгруженных заданий: ${e.message}")
            Result.failure(e)
        }
    }
}