package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.CommandExecutionRequestDto
import com.synngate.synnframe.data.remote.dto.CommandExecutionResponseDto

interface StepCommandApi {
    suspend fun executeCommand(
        endpoint: String,
        request: CommandExecutionRequestDto
    ): ApiResult<CommandExecutionResponseDto>
}