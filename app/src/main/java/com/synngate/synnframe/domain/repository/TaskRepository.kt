package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.entity.TaskType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Интерфейс репозитория для работы с заданиями
 * Определяет операции с данными, без бизнес-логики
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
     * Обновление строки факта задания
     * Только операция с данными без бизнес-логики
     */
    suspend fun updateTaskFactLine(factLine: TaskFactLine)

    /**
     * Проверка доступности задания на сервере
     */
    suspend fun checkTaskAvailability(id: String): Result<Boolean>

    /**
     * Выгрузка задания на сервер
     * Только операция с данными без бизнес-логики валидации
     */
    suspend fun uploadTaskToServer(id: String): Result<Boolean>

    /**
     * Получение всех завершенных, но не выгруженных заданий
     */
    suspend fun getCompletedNotUploadedTasks(): List<Task>

    /**
     * Получение списка заданий с сервера
     */
    suspend fun getTasksFromServer(): Result<List<Task>>
}