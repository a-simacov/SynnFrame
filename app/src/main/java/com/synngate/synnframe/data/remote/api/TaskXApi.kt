package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.CommonResponseDto
import com.synngate.synnframe.data.remote.dto.PlannedActionStatusRequestDto
import com.synngate.synnframe.domain.entity.taskx.action.FactAction

interface TaskXApi {

    suspend fun startTask(taskId: String, endpoint: String): ApiResult<CommonResponseDto>

    suspend fun pauseTask(taskId: String, endpoint: String): ApiResult<CommonResponseDto>

    suspend fun finishTask(taskId: String, endpoint: String): ApiResult<CommonResponseDto>

    suspend fun addFactAction(
        taskId: String,
        factAction: FactAction,
        endpoint: String
    ): ApiResult<CommonResponseDto>

    /**
     * Устанавливает статус ручного выполнения для запланированного действия
     *
     * @param taskId ID задания
     * @param requestDto DTO с информацией о статусе выполнения действия
     * @param endpoint Эндпоинт API
     * @return Результат операции
     */
    suspend fun setPlannedActionStatus(
        taskId: String,
        requestDto: PlannedActionStatusRequestDto,
        endpoint: String
    ): ApiResult<CommonResponseDto>
}