package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.AppVersionDto
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.util.network.ApiUtils
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
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.nio.ByteBuffer

/**
 * Интерфейс для отслеживания прогресса загрузки
 */
interface DownloadProgressListener {
    /**
     * Вызывается при обновлении прогресса загрузки
     *
     * @param bytesDownloaded Количество загруженных байт
     * @param totalBytes Общее количество байт для загрузки
     */
    fun onProgressUpdate(bytesDownloaded: Long, totalBytes: Long)
}

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
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
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

            // Используем prepareGet для возможности работы с потоком данных
            val call = client.prepareGet(url) {
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
            }

            val response = call.execute()

            if (response.status == HttpStatusCode.OK) {
                // Получаем общий размер файла из заголовка Content-Length
                val contentLength = response.contentLength() ?: -1L

                // Получаем канал для чтения данных
                val channel = response.bodyAsChannel()
                val result = withContext(Dispatchers.IO) {
                    // Создаем буфер для данных
                    val byteArrayOutputStream = java.io.ByteArrayOutputStream()
                    var downloadedBytes = 0L
                    var lastReportedProgress = 0L

                    // Буфер для чтения данных блоками
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

                    // Читаем данные блоками
                    while (!channel.isClosedForRead) {
                        val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                        if (bytesRead < 0) break

                        // Записываем прочитанные данные в выходной поток
                        byteArrayOutputStream.write(buffer, 0, bytesRead)

                        // Обновляем счетчик загруженных байт
                        downloadedBytes += bytesRead

                        // Отправляем обновление прогресса раз в PROGRESS_UPDATE_THRESHOLD байт
                        // или если это первое или последнее обновление
                        if (progressListener != null && (
                                    downloadedBytes - lastReportedProgress >= PROGRESS_UPDATE_THRESHOLD ||
                                            downloadedBytes == bytesRead.toLong() ||
                                            channel.isClosedForRead
                                    )) {
                            lastReportedProgress = downloadedBytes
                            progressListener.onProgressUpdate(downloadedBytes, contentLength)
                        }
                    }

                    // Отправляем финальное обновление прогресса
                    progressListener?.onProgressUpdate(downloadedBytes, contentLength)

                    // Возвращаем полученные данные как ByteArray
                    byteArrayOutputStream.toByteArray()
                }

                ApiResult.Success(result)
            } else {
                ApiResult.Error(response.status.value, "Server returned ${response.status}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error downloading app update: $version")
            ApiResult.Error(HttpStatusCode.InternalServerError.value, e.message ?: "Unknown error")
        }
    }

    /**
     * Читает доступные данные из канала в буфер
     * @return Количество прочитанных байт или -1, если канал закрыт
     */
    private suspend fun ByteReadChannel.readAvailable(buffer: ByteArray, offset: Int, length: Int): Int {
        if (isClosedForRead) return -1
        val bytesRead = availableForRead.coerceAtMost(length)
        if (bytesRead == 0) return 0

        readFully(buffer, offset, bytesRead)
        return bytesRead
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8192 // 8 KB
        private const val PROGRESS_UPDATE_THRESHOLD = 64 * 1024 // 64 KB
    }
}