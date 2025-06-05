package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.CommandExecutionRequestDto
import com.synngate.synnframe.data.remote.dto.CommandExecutionResponseDto
import com.synngate.synnframe.data.remote.service.ServerProvider
import io.ktor.client.HttpClient
import timber.log.Timber

class StepCommandApiImpl(
    httpClient: HttpClient,
    serverProvider: ServerProvider
) : BaseApiImpl(httpClient, serverProvider), StepCommandApi {

    override suspend fun executeCommand(
        endpoint: String,
        request: CommandExecutionRequestDto
    ): ApiResult<CommandExecutionResponseDto> {
        try {
            return executeApiRequest<CommandExecutionResponseDto>(
                endpoint = "POST $endpoint",
                body = request
            )
        } catch (e: Exception) {
            Timber.e(e, "Ошибка выполнения команды: ${request.commandId}")
            return createApiError("Ошибка выполнения команды: ${e.message}")
        }
    }
}