package com.synngate.synnframe.domain.repository

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.dto.CommonResponseDto
import com.synngate.synnframe.data.remote.dto.PlannedActionStatusRequestDto
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import kotlinx.coroutines.flow.Flow

interface TaskXRepository {
    // Получение списка заданий
    fun getTasks(): Flow<List<TaskX>>

    // Получение задания по ID
    suspend fun getTaskById(id: String): TaskX?

    // Обновление задания
    suspend fun updateTask(task: TaskX)

    // Начало выполнения задания
    suspend fun startTask(id: String, executorId: String, endpoint: String): Result<TaskX>

    // Приостановка выполнения задания
    suspend fun pauseTask(id: String, endpoint: String): Result<TaskX>

    // Завершение выполнения задания
    suspend fun finishTask(id: String, endpoint: String): Result<TaskX>

    // Добавление фактического действия
    suspend fun addFactAction(factAction: FactAction, endpoint: String): Result<TaskX>

    // Обновление статуса ручного выполнения запланированного действия
    suspend fun setPlannedActionStatus(
        taskId: String,
        requestDto: PlannedActionStatusRequestDto,
        endpoint: String
    ): ApiResult<CommonResponseDto>
}