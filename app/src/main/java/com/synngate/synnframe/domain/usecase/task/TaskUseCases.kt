package com.synngate.synnframe.domain.usecase.task

import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.repository.TaskRepository
import com.synngate.synnframe.domain.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.time.LocalDateTime

class TaskUseCases(
    private val taskRepository: TaskRepository
) : BaseUseCase {

    fun getTasks(): Flow<List<Task>> =
        taskRepository.getTasks()

    fun getFilteredTasks(
        nameFilter: String? = null,
        statusFilter: List<TaskStatus>? = null,
        typeFilter: List<String>? = null,
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
                Timber.w("Impossible to start task completion '${task.name}', current status: ${task.status}")
                return Result.failure(IllegalStateException("Task is not in TO_DO status"))
            }

            if (task.executorId != null && task.executorId != executorId) {
                Timber.w("Task '${task.name}' is assigned to another executor: ${task.executorId}")
                return Result.failure(IllegalStateException("Task is assigned to another executor"))
            }

            if (task.executorId == null) {
                try {
                    val availabilityResult = taskRepository.checkTaskAvailability(id)
                    if (availabilityResult.isFailure || availabilityResult.getOrNull() != true) {
                        Timber.w("Task '${task.name}' is not available")
                        return Result.failure(IllegalStateException("Task is not available"))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Exception during task availability check")
                    // Если нет подключения к серверу, позволяем взять задание в работу
                    Timber.w("Нет подключения к серверу, задание взято в работу без проверки")
                }
            }

            val updatedTask = task.copy(
                status = TaskStatus.IN_PROGRESS,
                startedAt = LocalDateTime.now(),
                executorId = executorId
            )

            taskRepository.updateTask(updatedTask)
            Timber.i("Task execution started: ${task.name}")

            return Result.success(taskRepository.getTaskById(id)!!)
        } catch (e: Exception) {
            Timber.e(e, "Exception during starting task")
            return Result.failure(e)
        }
    }

    suspend fun completeTask(id: String): Result<Task> {
        try {
            val task = taskRepository.getTaskById(id) ?:
            return Result.failure(IllegalArgumentException("Task not found"))

            if (task.status != TaskStatus.IN_PROGRESS) {
                Timber.w("Impossible to complete the task '${task.name}', current status: ${task.status}")
                return Result.failure(IllegalStateException("Task is not in IN_PROGRESS status"))
            }

            val updatedTask = task.copy(
                status = TaskStatus.COMPLETED,
                completedAt = LocalDateTime.now()
            )

            taskRepository.updateTask(updatedTask)
            Timber.i("Task completion finished: ${task.name}")

            try {
                uploadTask(id)
            } catch (e: Exception) {
                Timber.e(e, "Exception during task upload after completion")
            }

            return Result.success(taskRepository.getTaskById(id)!!)
        } catch (e: Exception) {
            Timber.e(e, "Exception during completing task")
            return Result.failure(e)
        }
    }

    suspend fun updateTaskFactLine(factLine: TaskFactLine) {
        try {
            val task = taskRepository.getTaskById(factLine.taskId) ?:
            throw IllegalArgumentException("Task not found: ${factLine.taskId}")

            if (task.status != TaskStatus.IN_PROGRESS) {
                Timber.w("Cannot update fact line: task is not in progress")
                throw IllegalStateException("Cannot update fact line: task is not in progress")
            }

            taskRepository.updateTaskFactLine(factLine)
            Timber.i("Updated fact line ${factLine.taskId}, product ${factLine.productId}, qty ${factLine.quantity}")
        } catch (e: Exception) {
            Timber.e(e, "Error updating task fact line")
            throw e
        }
    }

    suspend fun uploadTask(id: String): Result<Boolean> {
        return try {
            val task = taskRepository.getTaskById(id) ?:
            return Result.failure(IllegalArgumentException("Task not found"))

            if (task.status != TaskStatus.COMPLETED) {
                Timber.w("Cannot upload incomplete task: ${task.name}")
                return Result.failure(IllegalStateException("Cannot upload incomplete task"))
            }

            val result = taskRepository.uploadTaskToServer(id)

            if (result.isSuccess) {
                Timber.i("Task was uploaded successfully: ${task.name}")
            } else {
                val error = result.exceptionOrNull()
                Timber.w("Error uploading task: ${task.name}, error: ${error?.message}")

                // Для лучшей диагностики добавим стек трейс
                if (error != null) {
                    val stackTrace = error.stackTraceToString()
                    Timber.e("Stack trace error: $stackTrace")
                }
            }

            result
        } catch (e: Exception) {
            Timber.e(e, "Exception during task upload")
            Result.failure(e)
        }
    }

    suspend fun uploadCompletedTasks(): Result<Int> {
        return try {
            // Получаем все завершенные, но не выгруженные задания
            val completedTasks = taskRepository.getCompletedNotUploadedTasks()

            if (completedTasks.isEmpty()) {
                Timber.i("No completed tasks to upload")
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

            Timber.i("Uploaded tasks: $successCount from ${completedTasks.size}")
            Result.success(successCount)
        } catch (e: Exception) {
            Timber.e(e, "Exception during completed tasks upload")
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

            Timber.i("Tasks sync: added $addedCount, updated $updatedCount")
            Result.success(addedCount + updatedCount)
        } catch (e: Exception) {
            Timber.e(e, "Exception during tasks synchronization")
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
            Timber.i("New task was added: ${task.name} (${task.id})")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error adding task")
            Result.failure(e)
        }
    }

    suspend fun getCompletedNotUploadedTasks(): Result<List<Task>> {
        return try {
            val tasks = taskRepository.getCompletedNotUploadedTasks()
            Result.success(tasks)
        } catch (e: Exception) {
            Timber.e(e, "Error getting completed not uploaded tasks")
            Result.failure(e)
        }
    }

    suspend fun deleteTask(id: String): Result<Unit> {
        return try {
            // Получаем задание
            val task = taskRepository.getTaskById(id) ?:
            return Result.failure(IllegalArgumentException("Task not found"))

            // Проверяем, что задание выгружено
            if (!task.uploaded) {
                Timber.w("Cannot delete non-uploaded task: ${task.name}")
                return Result.failure(IllegalStateException("Cannot delete non-uploaded task"))
            }

            // Удаляем задание через репозиторий
            taskRepository.deleteTask(id)
            Timber.i("Task was deleted: ${task.name}")

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception during task deletion")
            Result.failure(e)
        }
    }
}