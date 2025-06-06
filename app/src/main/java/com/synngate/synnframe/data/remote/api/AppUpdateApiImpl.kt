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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

interface DownloadProgressListener {
    fun onProgressUpdate(bytesDownloaded: Long, totalBytes: Long)
}

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
            // Запрос к JSON-файлу с информацией о версии
            val url = "${server.updateUrl}/synnframe/update"
            val response = client.get(url) {
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

    override suspend fun downloadUpdateToFile(
        version: String,
        destinationFile: File,
        progressListener: DownloadProgressListener?,
        downloadJob: Job?
    ): ApiResult<Unit> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        return try {
            // Запрос для скачивания APK с указанной версией
            val url = "${server.updateUrl}/synnframe/update?version=${version}"

            Timber.d("Downloading update from: $url to file: ${destinationFile.absolutePath}")

            // Создаем директорию для файла, если её ещё нет
            destinationFile.parentFile?.mkdirs()

            // Используем prepareGet для возможности работы с потоком данных
            val call = client.prepareGet(url) {
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
            }

            val response = call.execute()

            if (response.status == HttpStatusCode.OK) {
                // Получаем общий размер файла из заголовка Content-Length
                val contentLength = response.contentLength() ?: -1L
                Timber.d("Update file size: $contentLength bytes")

                // Получаем канал для чтения данных
                val channel = response.bodyAsChannel()

                // Читаем данные из канала и пишем их в файл
                val bytesDownloaded = withContext(Dispatchers.IO) {
                    FileOutputStream(destinationFile).use { outputStream ->
                        var downloadedBytes = 0L
                        var lastReportedProgress = 0L

                        // Буфер для чтения данных блоками
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

                        // Читаем данные блоками
                        while (!channel.isClosedForRead) {
                            // Проверяем, не отменена ли загрузка
                            downloadJob?.isActive?.let { isActive ->
                                if (!isActive) {
                                    Timber.d("Download was cancelled")
                                    throw kotlinx.coroutines.CancellationException("Download cancelled by user")
                                }
                            }

                            val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                            if (bytesRead < 0) break

                            // Записываем данные в файл
                            outputStream.write(buffer, 0, bytesRead)

                            // Обновляем счетчик загруженных байт
                            downloadedBytes += bytesRead

                            // Отправляем обновление прогресса
                            if (progressListener != null && (
                                        downloadedBytes - lastReportedProgress >= PROGRESS_UPDATE_THRESHOLD ||
                                                downloadedBytes == bytesRead.toLong() ||
                                                channel.isClosedForRead
                                        )) {
                                lastReportedProgress = downloadedBytes
                                progressListener.onProgressUpdate(downloadedBytes, contentLength)
                            }
                        }

                        // Сбрасываем буфер на диск
                        outputStream.flush()

                        // Возвращаем общее количество загруженных байт
                        downloadedBytes
                    }
                }

                // Отправляем финальное обновление прогресса
                progressListener?.onProgressUpdate(bytesDownloaded, contentLength)

                // Проверяем, что размер загруженного файла соответствует ожидаемому
                if (contentLength > 0 && bytesDownloaded != contentLength) {
                    return ApiResult.Error(
                        HttpStatusCode.InternalServerError.value,
                        "Downloaded size ($bytesDownloaded) doesn't match expected size ($contentLength)"
                    )
                }

                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(response.status.value, "Server returned ${response.status}")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Пробрасываем исключение отмены выше
            Timber.d(e, "Download was cancelled")
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Error downloading app update: $version")
            ApiResult.Error(HttpStatusCode.InternalServerError.value, e.message ?: "Unknown error")
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8192 // 8 KB
        private const val PROGRESS_UPDATE_THRESHOLD = 64 * 1024 // 64 KB
    }
}