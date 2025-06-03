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
            // Преобразуем factAction в DTO для запроса
            val requestDto = FactActionRequestDto.fromDomain(factAction)

            // Выполняем POST-запрос с передачей factAction в теле
            return executeApiRequest<StepObjectResponseDto>(
                endpoint = "POST $endpoint",
                body = requestDto
            )
        } catch (e: Exception) {
            Timber.e(e, "Ошибка получения объекта шага с сервера: $endpoint")
            return createApiError("Ошибка получения объекта: ${e.message}")
        }
    }
}