package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.ValidationResponseDto
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.domain.service.ValidationApiService
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ValidationApiServiceImpl(
    httpClient: HttpClient,
    serverProvider: ServerProvider
) : BaseApiImpl(httpClient, serverProvider), ValidationApiService {

    override suspend fun validate(endpoint: String, value: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val params = mapOf("value" to value)

            val result = executeApiRequest<ValidationResponseDto>(
                endpoint = endpoint,
                params = params
            )

            when (result) {
                is ApiResult.Success -> {
                    Pair(result.data.result, result.data.message)
                }
                is ApiResult.Error -> {
                    Timber.e("Validation error: ${result.message}")
                    Pair(false, result.message)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error validating value: $value at endpoint: $endpoint")
            Pair(false, "Validation error: ${e.message}")
        }
    }
}