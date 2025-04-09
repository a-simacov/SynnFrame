// Репозиторий для типов заданий X
package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import kotlinx.coroutines.flow.Flow

interface TaskTypeXRepository {
    // Получение всех типов заданий
    fun getTaskTypes(): Flow<List<TaskTypeX>>

    // Получение типа задания по ID
    suspend fun getTaskTypeById(id: String): TaskTypeX?

    // Добавление типа задания
    suspend fun addTaskType(taskType: TaskTypeX)

    // Обновление типа задания
    suspend fun updateTaskType(taskType: TaskTypeX)

    // Удаление типа задания
    suspend fun deleteTaskType(id: String)
}