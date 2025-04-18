package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface TaskXRepository {
    // Получение списка заданий
    fun getTasks(): Flow<List<TaskX>>

    // Получение отфильтрованного списка заданий
    fun getFilteredTasks(
        nameFilter: String? = null,
        statusFilter: List<TaskXStatus>? = null,
        typeFilter: List<String>? = null,
        dateFromFilter: LocalDateTime? = null,
        dateToFilter: LocalDateTime? = null,
        executorIdFilter: String? = null
    ): Flow<List<TaskX>>

    // Получение задания по ID
    suspend fun getTaskById(id: String): TaskX?

    // Получение задания по штрихкоду
    suspend fun getTaskByBarcode(barcode: String): TaskX?

    // Количество заданий для текущего пользователя
    fun getTasksCountForCurrentUser(): Flow<Int>

    // Добавление задания
    suspend fun addTask(task: TaskX)

    // Обновление задания
    suspend fun updateTask(task: TaskX)

    // Удаление задания
    suspend fun deleteTask(id: String)

    // Изменение статуса задания
    suspend fun setTaskStatus(id: String, status: TaskXStatus)

    // Назначение исполнителя
    suspend fun assignExecutor(id: String, executorId: String)

    // Установка времени начала выполнения
    suspend fun setStartTime(id: String, startTime: LocalDateTime)

    // Установка времени завершения
    suspend fun setCompletionTime(id: String, completionTime: LocalDateTime)

    // Проверка доступности задания (запрос к серверу)
    suspend fun checkTaskAvailability(id: String): Result<Boolean>

    // Верификация задания
    suspend fun verifyTask(id: String, barcode: String): Result<Boolean>

    // Новые методы для работы с запланированными и фактическими действиями

    // Добавление фактического действия
    suspend fun addFactAction(factAction: FactAction)

    // Получение запланированного действия по ID
    suspend fun getPlannedActionById(taskId: String, actionId: String): PlannedAction?

    // Обновление запланированного действия
    suspend fun updatePlannedAction(taskId: String, action: PlannedAction)

    // Отметка запланированного действия как выполненного
    suspend fun markPlannedActionCompleted(taskId: String, actionId: String, isCompleted: Boolean)

    // Отметка запланированного действия как пропущенного
    suspend fun markPlannedActionSkipped(taskId: String, actionId: String, isSkipped: Boolean)

    // Получение следующего запланированного действия
    suspend fun getNextPlannedAction(taskId: String): PlannedAction?
}