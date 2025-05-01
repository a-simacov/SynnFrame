package com.synngate.synnframe.domain.usecase.taskx

import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.repository.TaskXRepository
import com.synngate.synnframe.domain.service.TaskContextManager
import com.synngate.synnframe.domain.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Use cases для работы с заданиями TaskX
 * Обновлено для работы с TaskXRepository и endpoint из TaskContextManager
 */
class TaskXUseCases(
    private val taskXRepository: TaskXRepository,
    private val taskContextManager: TaskContextManager
) : BaseUseCase {

    // Получение отфильтрованного списка заданий - просто фильтрует последнее загруженное задание
    fun getFilteredTasks(
        nameFilter: String? = null,
        statusFilter: List<TaskXStatus>? = null,
        typeFilter: List<String>? = null,
        dateFromFilter: LocalDateTime? = null,
        dateToFilter: LocalDateTime? = null,
        executorIdFilter: String? = null
    ): Flow<List<TaskX>> {
        return flow {
            val task = taskContextManager.lastStartedTaskX.value ?: return@flow emit(emptyList())

            val matchesName = nameFilter == null || task.name.contains(nameFilter, ignoreCase = true)
            val matchesStatus = statusFilter == null || task.status in statusFilter
            val matchesType = typeFilter == null || task.taskTypeId in typeFilter
            val matchesDateFrom = dateFromFilter == null || task.createdAt.isAfter(dateFromFilter)
            val matchesDateTo = dateToFilter == null || task.createdAt.isBefore(dateToFilter)
            val matchesExecutor = executorIdFilter == null || task.executorId == executorIdFilter

            if (matchesName && matchesStatus && matchesType && matchesDateFrom && matchesDateTo && matchesExecutor) {
                emit(listOf(task))
            } else {
                emit(emptyList())
            }
        }
    }

    // Получение задания по ID
    suspend fun getTaskById(id: String): TaskX? {
        val task = taskContextManager.lastStartedTaskX.value
        return if (task?.id == id) task else null
    }

    // Верификация задания
    suspend fun verifyTask(id: String, barcode: String): Result<Boolean> {
        val task = taskContextManager.lastStartedTaskX.value
        if (task == null || task.id != id) {
            return Result.failure(IllegalArgumentException("Задание не найдено: $id"))
        }

        // Проверяем, что штрихкод совпадает
        val isVerified = task.barcode == barcode

        if (isVerified) {
            // Обновляем задание в контексте
            val updatedTask = task.copy(isVerified = true, lastModifiedAt = LocalDateTime.now())
            taskContextManager.updateTask(updatedTask)
            Timber.i("Задание $id успешно верифицировано")
        } else {
            Timber.w("Неверный штрихкод при верификации задания $id: $barcode")
        }

        return Result.success(isVerified)
    }

    // Начало выполнения задания
    suspend fun startTask(id: String, executorId: String): Result<TaskX> {
        // Получаем endpoint из контекста
        val endpoint = taskContextManager.currentEndpoint.value
            ?: return Result.failure(IllegalStateException("Не найден endpoint для задания"))

        // Используем репозиторий для отправки запроса на сервер
        return taskXRepository.startTask(id, executorId, endpoint)
    }

    // Завершение выполнения задания
    suspend fun completeTask(id: String): Result<TaskX> {
        // Получаем endpoint из контекста
        val endpoint = taskContextManager.currentEndpoint.value
            ?: return Result.failure(IllegalStateException("Не найден endpoint для задания"))

        // Используем репозиторий для отправки запроса на сервер
        return taskXRepository.finishTask(id, endpoint)
    }

    // Приостановка выполнения задания
    suspend fun pauseTask(id: String): Result<TaskX> {
        // Получаем endpoint из контекста
        val endpoint = taskContextManager.currentEndpoint.value
            ?: return Result.failure(IllegalStateException("Не найден endpoint для задания"))

        // Используем репозиторий для отправки запроса на сервер
        return taskXRepository.pauseTask(id, endpoint)
    }

    // Получение типа задания
    suspend fun getTaskType(taskTypeId: String): TaskTypeX? {
        return taskContextManager.lastTaskTypeX.value
    }
}