package com.synngate.synnframe.domain.usecase.taskx

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.service.TaskContextManager
import com.synngate.synnframe.domain.usecase.BaseUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Use cases для работы с заданиями TaskX
 * Упрощенная версия для работы только с TaskContextManager
 */
class TaskXUseCases(
    private val taskContextManager: TaskContextManager
) : BaseUseCase {

    // Получение последнего загруженного задания из контекста
    fun getTasks(): Flow<List<TaskX>> {
        return flow {
            val task = taskContextManager.lastStartedTaskX.value
            if (task != null) {
                emit(listOf(task))
            } else {
                emit(emptyList())
            }
        }
    }

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

    // Получение задания по штрихкоду
    suspend fun getTaskByBarcode(barcode: String): TaskX? {
        val task = taskContextManager.lastStartedTaskX.value
        return if (task?.barcode == barcode) task else null
    }

    // Получение количества заданий для текущего пользователя - всегда возвращает 0 или 1
    fun getTasksCountForCurrentUser(): Flow<Int> {
        return flow {
            val task = taskContextManager.lastStartedTaskX.value
            emit(if (task != null) 1 else 0)
        }
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

    // Изменение статуса задания
    suspend fun setTaskStatus(id: String, status: TaskXStatus): Result<Boolean> {
        val task = taskContextManager.lastStartedTaskX.value
        if (task == null || task.id != id) {
            return Result.failure(IllegalArgumentException("Задание не найдено: $id"))
        }

        // Обновляем задание в контексте
        val updatedTask = task.copy(
            status = status,
            lastModifiedAt = LocalDateTime.now()
        )
        taskContextManager.updateTask(updatedTask)
        return Result.success(true)
    }

    // Начало выполнения задания
    suspend fun startTask(id: String, executorId: String): Result<TaskX> {
        val task = taskContextManager.lastStartedTaskX.value
        if (task == null || task.id != id) {
            return Result.failure(IllegalArgumentException("Задание не найдено: $id"))
        }

        if (task.status != TaskXStatus.TO_DO) {
            return Result.failure(IllegalStateException("Задание не в статусе 'К выполнению'"))
        }

        // Обновляем задание
        val now = LocalDateTime.now()
        val updatedTask = task.copy(
            status = TaskXStatus.IN_PROGRESS,
            startedAt = now,
            lastModifiedAt = now,
            executorId = executorId
        )

        taskContextManager.updateTask(updatedTask)
        return Result.success(updatedTask)
    }

    // Завершение выполнения задания
    suspend fun completeTask(id: String): Result<TaskX> {
        val task = taskContextManager.lastStartedTaskX.value
        if (task == null || task.id != id) {
            return Result.failure(IllegalArgumentException("Задание не найдено: $id"))
        }

        val taskType = taskContextManager.lastTaskTypeX.value
        if (taskType == null) {
            return Result.failure(IllegalArgumentException("Тип задания не найден"))
        }

        if (task.status != TaskXStatus.IN_PROGRESS) {
            return Result.failure(IllegalStateException("Задание не в статусе 'Выполняется'"))
        }

        // Проверяем, есть ли строки факта или разрешено завершение без них
        if (task.factActions.isEmpty() && !taskType.allowCompletionWithoutFactActions) {
            return Result.failure(IllegalStateException("Невозможно завершить задание без строк факта"))
        }

        // Обновляем задание
        val now = LocalDateTime.now()
        val updatedTask = task.copy(
            status = TaskXStatus.COMPLETED,
            completedAt = now,
            lastModifiedAt = now
        )

        taskContextManager.updateTask(updatedTask)
        return Result.success(updatedTask)
    }

    // Приостановка выполнения задания
    suspend fun pauseTask(id: String): Result<TaskX> {
        val task = taskContextManager.lastStartedTaskX.value
        if (task == null || task.id != id) {
            return Result.failure(IllegalArgumentException("Задание не найдено: $id"))
        }

        if (task.status != TaskXStatus.IN_PROGRESS) {
            return Result.failure(IllegalStateException("Задание не в статусе 'Выполняется'"))
        }

        // Обновляем задание
        val now = LocalDateTime.now()
        val updatedTask = task.copy(
            status = TaskXStatus.PAUSED,
            lastModifiedAt = now
        )

        taskContextManager.updateTask(updatedTask)
        return Result.success(updatedTask)
    }

    // Возобновление выполнения задания
    suspend fun resumeTask(id: String): Result<TaskX> {
        val task = taskContextManager.lastStartedTaskX.value
        if (task == null || task.id != id) {
            return Result.failure(IllegalArgumentException("Задание не найдено: $id"))
        }

        if (task.status != TaskXStatus.PAUSED) {
            return Result.failure(IllegalStateException("Задание не в статусе 'Приостановлено'"))
        }

        // Обновляем задание
        val now = LocalDateTime.now()
        val updatedTask = task.copy(
            status = TaskXStatus.IN_PROGRESS,
            lastModifiedAt = now
        )

        taskContextManager.updateTask(updatedTask)
        return Result.success(updatedTask)
    }

    // Добавление строки факта
    suspend fun addFactAction(factAction: FactAction): Result<TaskX> {
        val task = taskContextManager.lastStartedTaskX.value
        if (task == null || task.id != factAction.taskId) {
            return Result.failure(IllegalArgumentException("Задание не найдено: ${factAction.taskId}"))
        }

        val taskType = taskContextManager.lastTaskTypeX.value
        if (taskType == null) {
            return Result.failure(IllegalArgumentException("Тип задания не найден"))
        }

        if (task.status != TaskXStatus.IN_PROGRESS) {
            return Result.failure(IllegalStateException("Задание не в статусе 'Выполняется'"))
        }

        // Обновляем задание
        val updatedFactActions = task.factActions.toMutableList()
        updatedFactActions.add(factAction)

        val updatedTask = task.copy(
            factActions = updatedFactActions,
            lastModifiedAt = LocalDateTime.now()
        )

        taskContextManager.updateTask(updatedTask)
        return Result.success(updatedTask)
    }

    // Получение следующего запланированного действия
    suspend fun getNextPlannedAction(taskId: String): PlannedAction? {
        val task = taskContextManager.lastStartedTaskX.value
        if (task?.id != taskId) return null

        return task.plannedActions.firstOrNull { !it.isCompleted && !it.isSkipped }
    }

    // Получение типа задания - временная заглушка, так как типы заданий не хранятся в TaskContextManager
    suspend fun getTaskType(taskTypeId: String): TaskTypeX? {
        return taskContextManager.lastTaskTypeX.value
    }

    // Создание паллеты - всегда возвращает новый объект с генерированным кодом
    suspend fun createPallet(): Result<Pallet> {
        try {
            val palletCode = "IN${System.currentTimeMillis()}"
            val newPallet = Pallet(
                code = palletCode,
                isClosed = false
            )
            return Result.success(newPallet)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при создании паллеты")
            return Result.failure(e)
        }
    }

    // Печать этикетки паллеты - имитация успешной операции
    suspend fun printPalletLabel(code: String): Result<Boolean> {
        return Result.success(true)
    }

    // Закрытие паллеты - имитация успешной операции
    suspend fun closePallet(code: String): Result<Boolean> {
        return Result.success(true)
    }

    // Получение ячейки по коду - создает новый объект с указанным кодом
    suspend fun getBinByCode(code: String): BinX? {
        return BinX(
            code = code,
            zone = "Неизвестная зона",
            line = "",
            rack = "",
            tier = "",
            position = ""
        )
    }

    // Получение отфильтрованных ячеек - всегда возвращает пустой список
    fun getFilteredBins(codeFilter: String? = null, zoneFilter: String? = null): Flow<List<BinX>> {
        return flowOf(emptyList())
    }

    // Проверка существования ячейки - всегда возвращает true
    suspend fun binExists(code: String): Boolean {
        return true
    }

    // Получение типа задания по ID
    suspend fun getTaskTypeById(taskTypeId: String): TaskTypeX? {
        val taskType = taskContextManager.lastTaskTypeX.value
        return if (taskType?.id == taskTypeId) taskType else null
    }

    // Получение всех типов заданий - возвращает список, содержащий только текущий тип задания
    fun getTaskTypes(): Flow<List<TaskTypeX>> {
        return flow {
            val taskType = taskContextManager.lastTaskTypeX.value
            if (taskType != null) {
                emit(listOf(taskType))
            } else {
                emit(emptyList())
            }
        }
    }
}