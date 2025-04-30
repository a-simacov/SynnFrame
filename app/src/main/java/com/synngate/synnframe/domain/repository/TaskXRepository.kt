package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.taskx.TaskX
import kotlinx.coroutines.flow.Flow

interface TaskXRepository {
    // Получение списка заданий
    fun getTasks(): Flow<List<TaskX>>

    // Получение задания по ID
    suspend fun getTaskById(id: String): TaskX?

    // Обновление задания
    suspend fun updateTask(task: TaskX)
}