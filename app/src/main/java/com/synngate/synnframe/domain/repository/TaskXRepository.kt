package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.taskx.FactLineX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
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

    // Добавление строки факта
    suspend fun addFactLine(factLine: FactLineX)

    // Установка времени начала выполнения
    suspend fun setStartTime(id: String, startTime: LocalDateTime)

    // Установка времени завершения
    suspend fun setCompletionTime(id: String, completionTime: LocalDateTime)

    // Проверка доступности задания (запрос к серверу)
    suspend fun checkTaskAvailability(id: String): Result<Boolean>

    // Верификация задания
    suspend fun verifyTask(id: String, barcode: String): Result<Boolean>
}