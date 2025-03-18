package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.TaskAvailabilityResponseDto
import com.synngate.synnframe.domain.entity.Task

/**
 * Интерфейс TaskApi для работы с заданиями
 */
interface TaskApi {
    /**
     * Получение списка заданий с сервера
     */
    suspend fun getTasks(): ApiResult<List<Task>>

    /**
     * Проверка доступности задания
     */
    suspend fun checkTaskAvailability(taskId: String): ApiResult<TaskAvailabilityResponseDto>

    /**
     * Выгрузка задания на сервер
     */
    suspend fun uploadTask(taskId: String, task: Task): ApiResult<Unit>
}