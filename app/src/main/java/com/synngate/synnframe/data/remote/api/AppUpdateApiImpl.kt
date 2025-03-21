package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.AppVersionDto
import com.synngate.synnframe.data.remote.service.ServerProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.util.toByteArray
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyTo
import timber.log.Timber
import java.nio.ByteBuffer

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

    /**
     * Получение строки Basic аутентификации
     */
    private fun getBasicAuth(login: String, password: String): String {
        val credentials = "$login:$password"
        return java.util.Base64.getEncoder().encodeToString(credentials.toByteArray())
    }

    override suspend fun downloadUpdate(
        version: String,
        progressListener: DownloadProgressListener?
    ): ApiResult<ByteArray> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        return try {
            val url = "${server.apiUrl}/app/file?version=$version"

            // Создаем и выполняем запрос
            val response = client.get(url) {
                header("Authorization", "Basic ${getBasicAuth(server.login, server.password)}")
            }

            if (response.status == HttpStatusCode.OK) {
                // Получаем общий размер файла из заголовка Content-Length
                val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: -1L

                // Получаем массив байтов из ответа
                val bytes = response.body<ByteArray>()

                // Сообщаем о завершении загрузки
                progressListener?.onProgressUpdate(bytes.size.toLong(), contentLength)

                ApiResult.Success(bytes)
            } else {
                ApiResult.Error(response.status.value, "Server returned ${response.status}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error downloading app update: $version")
            ApiResult.Error(HttpStatusCode.InternalServerError.value, e.message ?: "Unknown error")
        }
    }

}

interface DownloadProgressListener {
    fun onProgressUpdate(bytesDownloaded: Long, totalBytes: Long)
}