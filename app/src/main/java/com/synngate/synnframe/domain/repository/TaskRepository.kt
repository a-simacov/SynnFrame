package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface TaskRepository {

    fun getTasks(): Flow<List<Task>>

    fun getFilteredTasks(
        nameFilter: String? = null,
        statusFilter: List<TaskStatus>? = null,
        typeFilter: List<String>? = null,
        dateFromFilter: LocalDateTime? = null,
        dateToFilter: LocalDateTime? = null,
        executorIdFilter: String? = null
    ): Flow<List<Task>>

    suspend fun getTaskById(id: String): Task?

    suspend fun getTaskByBarcode(barcode: String): Task?

    fun getTasksCountForCurrentUser(): Flow<Int>

    suspend fun addTask(task: Task)

    suspend fun addTasks(tasks: List<Task>)

    suspend fun updateTask(task: Task)

    suspend fun deleteTask(id: String)

    suspend fun setTaskStatus(id: String, status: TaskStatus)

    suspend fun updateTaskFactLine(factLine: TaskFactLine)

    suspend fun checkTaskAvailability(id: String): Result<Boolean>

    suspend fun uploadTaskToServer(id: String): Result<Boolean>

    suspend fun getCompletedNotUploadedTasks(): List<Task>

    suspend fun getTasksFromServer(): Result<List<Task>>
}