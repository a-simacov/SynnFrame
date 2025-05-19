package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.ActionSearchResponseDto
import com.synngate.synnframe.data.remote.service.ServerProvider
import io.ktor.client.HttpClient
import timber.log.Timber

class ActionSearchApiImpl(
    httpClient: HttpClient,
    serverProvider: ServerProvider
) : BaseApiImpl(httpClient, serverProvider), ActionSearchApi {

    override suspend fun searchAction(
        endpoint: String,
        searchValue: String
    ): ApiResult<ActionSearchResponseDto> {
        return try {
            executeApiRequest<ActionSearchResponseDto>(
                endpoint = endpoint,
                params = mapOf("searchValue" to searchValue)
            )
        } catch (e: Exception) {
            Timber.e(e, "Error searching action: $searchValue at endpoint: $endpoint")
            createApiError("Error searching action: ${e.message}")
        }
    }
}