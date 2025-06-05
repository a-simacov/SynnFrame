package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.FactActionRequestDto
import com.synngate.synnframe.data.remote.dto.StepObjectResponseDto
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import io.ktor.client.HttpClient
import timber.log.Timber

class StepObjectApiImpl(
    httpClient: HttpClient,
    serverProvider: ServerProvider
) : BaseApiImpl(httpClient, serverProvider), StepObjectApi {

    override suspend fun getStepObject(
        endpoint: String,
        factAction: FactAction
    ): ApiResult<StepObjectResponseDto> {
        try {
            val requestDto = FactActionRequestDto.fromDomain(factAction)

            return executeApiRequest<StepObjectResponseDto>(
                endpoint = "POST $endpoint",
                body = requestDto
            )
        } catch (e: Exception) {
            Timber.e(e, "Error getting step object from server: $endpoint")
            return createApiError("Error getting step object from server: ${e.message}")
        }
    }
}