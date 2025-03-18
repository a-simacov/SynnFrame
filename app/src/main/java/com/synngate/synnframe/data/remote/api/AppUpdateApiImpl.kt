package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.AppVersionDto
import com.synngate.synnframe.data.remote.service.ServerProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import timber.log.Timber

/**
 * Реализация интерфейса AppUpdateApi
 */
class AppUpdateApiImpl(
    private val client: HttpClient,
    private val serverProvider: ServerProvider
) : AppUpdateApi {

    override suspend fun getLastVersion(): ApiResult<AppVersionDto> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        return try {
            val url = "${server.apiUrl}/app/lastversion"
            val response = client.get(url) {
                // Basic аутентификация
                header("Authorization", "Basic ${getBasicAuth(server.login, server.password)}")
            }

            if (response.status == HttpStatusCode.OK) {
                val appVersion = response.body<AppVersionDto>()
                ApiResult.Success(appVersion)
            } else {
                ApiResult.Error(response.status.value, "Server returned ${response.status}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting last app version")
            ApiResult.Error(HttpStatusCode.InternalServerError.value, e.message ?: "Unknown error")
        }
    }

    override suspend fun downloadUpdate(version: String): ApiResult<ByteArray> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        return try {
            val url = "${server.apiUrl}/app/file?version=$version"
            val response = client.get(url) {
                // Basic аутентификация
                header("Authorization", "Basic ${getBasicAuth(server.login, server.password)}")
            }

            if (response.status == HttpStatusCode.OK) {
                val bytes = response.body<ByteArray>()
                ApiResult.Success(bytes)
            } else {
                ApiResult.Error(response.status.value, "Server returned ${response.status}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error downloading app update: $version")
            ApiResult.Error(HttpStatusCode.InternalServerError.value, e.message ?: "Unknown error")
        }
    }

    /**
     * Получение строки Basic аутентификации
     */
    private fun getBasicAuth(login: String, password: String): String {
        val credentials = "$login:$password"
        return java.util.Base64.getEncoder().encodeToString(credentials.toByteArray())
    }
}