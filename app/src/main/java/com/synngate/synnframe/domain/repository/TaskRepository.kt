package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.entity.TaskType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Интерфейс репозитория для работы с заданиями
 */
interface TaskRepository {
    /**
     * Получение списка всех заданий
     */
    fun getTasks(): Flow<List<Task>>

    /**
     * Получение списка заданий с фильтрацией
     */
    fun getFilteredTasks(
        nameFilter: String? = null,
        statusFilter: List<TaskStatus>? = null,
        typeFilter: TaskType? = null,
        dateFromFilter: LocalDateTime? = null,
        dateToFilter: LocalDateTime? = null,
        executorIdFilter: String? = null
    ): Flow<List<Task>>

    /**
     * Получение задания по идентификатору
     */
    suspend fun getTaskById(id: String): Task?

    /**
     * Получение задания по штрихкоду
     */
    suspend fun getTaskByBarcode(barcode: String): Task?

    /**
     * Получение количества заданий для текущего пользователя
     */
    fun getTasksCountForCurrentUser(): Flow<Int>

    /**
     * Добавление нового задания
     */
    suspend fun addTask(task: Task)

    /**
     * Добавление списка заданий
     */
    suspend fun addTasks(tasks: List<Task>)

    /**
     * Обновление существующего задания
     */
    suspend fun updateTask(task: Task)

    /**
     * Удаление задания
     */
    suspend fun deleteTask(id: String)

    /**
     * Установка статуса задания
     */
    suspend fun setTaskStatus(id: String, status: TaskStatus)

    /**
     * Начало выполнения задания
     */
    suspend fun startTask(id: String, executorId: String): Result<Task>

    /**
     * Завершение задания
     */
    suspend fun completeTask(id: String): Result<Task>

    /**
     * Обновление строки факта задания
     */
    suspend fun updateTaskFactLine(factLine: TaskFactLine)

    /**
     * Выгрузка задания на сервер
     */
    suspend fun uploadTaskToServer(id: String): Result<Boolean>

    /**
     * Выгрузка всех завершенных заданий на сервер
     */
    suspend fun uploadCompletedTasksToServer(): Result<Int>

    /**
     * Синхронизация заданий с сервером
     */
    suspend fun syncTasksWithServer(): Result<Int>
}