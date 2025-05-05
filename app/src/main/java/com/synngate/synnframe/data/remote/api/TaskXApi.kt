package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.CommonResponseDto
import com.synngate.synnframe.domain.entity.taskx.action.FactAction

interface TaskXApi {

    suspend fun startTask(taskId: String, endpoint: String): ApiResult<CommonResponseDto>

    suspend fun pauseTask(taskId: String, endpoint: String): ApiResult<CommonResponseDto>

    suspend fun finishTask(taskId: String, endpoint: String): ApiResult<CommonResponseDto>

    /**
     * Добавление фактического действия
     * @param taskId ID задания
     * @param factAction Фактическое действие
     * @param endpoint Endpoint API
     * @param finalizePlannedAction Завершить плановое действие (true) или только создать факт (false)
     * @return Результат операции
     */
    suspend fun addFactAction(
        taskId: String,
        factAction: FactAction,
        endpoint: String,
        finalizePlannedAction: Boolean = true
    ): ApiResult<CommonResponseDto>
}