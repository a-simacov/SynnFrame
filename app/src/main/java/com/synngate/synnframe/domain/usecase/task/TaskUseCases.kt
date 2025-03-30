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

/**
 * Use Case класс для операций с заданиями
 * Содержит всю бизнес-логику, связанную с заданиями
 */
class TaskUseCases(
    private val taskRepository: TaskRepository,
    private val loggingService: LoggingService
) : BaseUseCase {

    // Базовые операции CRUD, делегируемые репозиторию
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

    // Операции с бизнес-логикой
    suspend fun startTask(id: String, executorId: String): Result<Task> {
        try {
            // Получаем задание
            val task = taskRepository.getTaskById(id) ?:
            return Result.failure(IllegalArgumentException("Task not found"))

            // Проверка бизнес-правила: задание должно быть в статусе "К выполнению"
            if (task.status != TaskStatus.TO_DO) {
                loggingService.logWarning("Невозможно начать выполнение задания '${task.name}', текущий статус: ${task.status}")
                return Result.failure(IllegalStateException("Task is not in TO_DO status"))
            }

            // Проверка бизнес-правила: если у задания есть исполнитель, то это должен быть текущий пользователь
            if (task.executorId != null && task.executorId != executorId) {
                loggingService.logWarning("Задание '${task.name}' назначено другому исполнителю: ${task.executorId}")
                return Result.failure(IllegalStateException("Task is assigned to another executor"))
            }

            // Если задание без исполнителя, проверяем на сервере, не взял ли его кто-то другой
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

            // Обновляем задание
            val updatedTask = task.copy(
                status = TaskStatus.IN_PROGRESS,
                startedAt = LocalDateTime.now(),
                executorId = executorId
            )

            // Сохраняем обновленное задание
            taskRepository.updateTask(updatedTask)
            loggingService.logInfo("Начато выполнение задания: ${task.name}")

            // Возвращаем обновленное задание
            return Result.success(taskRepository.getTaskById(id)!!)
        } catch (e: Exception) {
            Timber.e(e, "Exception during starting task")
            loggingService.logError("Исключение при запуске задания: ${e.message}")
            return Result.failure(e)
        }
    }

    suspend fun completeTask(id: String): Result<Task> {
        try {
            // Получаем задание
            val task = taskRepository.getTaskById(id) ?:
            return Result.failure(IllegalArgumentException("Task not found"))

            // Проверка бизнес-правила: задание должно быть в статусе "Выполняется"
            if (task.status != TaskStatus.IN_PROGRESS) {
                loggingService.logWarning("Невозможно завершить задание '${task.name}', текущий статус: ${task.status}")
                return Result.failure(IllegalStateException("Task is not in IN_PROGRESS status"))
            }

            // Обновляем задание
            val updatedTask = task.copy(
                status = TaskStatus.COMPLETED,
                completedAt = LocalDateTime.now()
            )

            // Сохраняем обновленное задание
            taskRepository.updateTask(updatedTask)
            loggingService.logInfo("Завершено выполнение задания: ${task.name}")

            // Пытаемся выгрузить задание на сервер
            try {
                uploadTask(id)
            } catch (e: Exception) {
                // Игнорируем ошибки выгрузки, так как задание можно будет выгрузить позже
                Timber.e(e, "Exception during task upload after completion")
                loggingService.logWarning("Ошибка выгрузки задания после завершения: ${e.message}")
            }

            // Возвращаем обновленное задание
            return Result.success(taskRepository.getTaskById(id)!!)
        } catch (e: Exception) {
            Timber.e(e, "Exception during completing task")
            loggingService.logError("Исключение при завершении задания: ${e.message}")
            return Result.failure(e)
        }
    }

    suspend fun updateTaskFactLine(factLine: TaskFactLine) {
        try {
            // Проверяем существование задания
            val task = taskRepository.getTaskById(factLine.taskId) ?:
            throw IllegalArgumentException("Task not found: ${factLine.taskId}")

            // Проверяем, что задание в статусе "Выполняется"
            if (task.status != TaskStatus.IN_PROGRESS) {
                loggingService.logWarning("Невозможно обновить строку факта: задание не в статусе выполнения")
                throw IllegalStateException("Cannot update fact line: task is not in progress")
            }

            // Передаем обновление в репозиторий
            taskRepository.updateTaskFactLine(factLine)
            loggingService.logInfo("Обновлена строка факта для задания ${factLine.taskId}, товар ${factLine.productId}, количество ${factLine.quantity}")
        } catch (e: Exception) {
            Timber.e(e, "Error updating task fact line")
            loggingService.logError("Ошибка при обновлении строки факта: ${e.message}")
            throw e
        }
    }

    // в com.synngate.synnframe.domain.usecase.task.TaskUseCases
    suspend fun uploadTask(id: String): Result<Boolean> {
        return try {
            // Получаем задание
            val task = taskRepository.getTaskById(id) ?:
            return Result.failure(IllegalArgumentException("Task not found"))

            // Проверяем, что задание завершено
            if (task.status != TaskStatus.COMPLETED) {
                loggingService.logWarning("Невозможно выгрузить незавершенное задание: ${task.name}")
                return Result.failure(IllegalStateException("Cannot upload incomplete task"))
            }

            // Вызываем выгрузку в репозитории
            val result = taskRepository.uploadTaskToServer(id)

            // Логируем результат
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

    /**
     * Добавляет новое задание
     * @param task Задание для добавления
     * @return Результат операции
     */
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

    /**
     * Получение завершенных, но не выгруженных заданий
     */
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