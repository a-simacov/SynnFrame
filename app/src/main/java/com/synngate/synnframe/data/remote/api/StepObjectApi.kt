package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.StepObjectResponseDto
import com.synngate.synnframe.domain.entity.taskx.action.FactAction

interface StepObjectApi {
    suspend fun getStepObject(
        endpoint: String,
        factAction: FactAction
    ): ApiResult<StepObjectResponseDto>
}