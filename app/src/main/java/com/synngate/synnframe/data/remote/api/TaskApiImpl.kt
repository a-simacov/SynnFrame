package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.TaskAvailabilityResponseDto
import com.synngate.synnframe.data.remote.dto.TaskDto
import com.synngate.synnframe.data.remote.service.ApiService
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.util.network.ApiUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import timber.log.Timber

/**
 * Реализация интерфейса TaskApi
 */
class TaskApiImpl(
    private val client: HttpClient,
    private val serverProvider: ServerProvider,
    private val apiService: ApiService
) : TaskApi {

    override suspend fun getTasks(): ApiResult<List<Task>> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        return try {
            val url = "${server.apiUrl}/tasks"
            val response = client.get(url) {
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                header("User-Auth-Id", serverProvider.getCurrentUserId() ?: "")
            }

            if (response.status.isSuccess()) {
                try {
                    val taskDtos = response.body<List<TaskDto>>()
                    val tasks = taskDtos.map { it.toDomainModel() }
                    ApiResult.Success(tasks)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing tasks JSON: ${e.message}")
                    val bodyText = response.bodyAsText()
                    Timber.d("Response body: ${bodyText.take(500)}...")
                    ApiResult.Error(
                        HttpStatusCode.InternalServerError.value,
                        "Error parsing tasks: ${e.message}"
                    )
                }
            } else {
                ApiResult.Error(
                    response.status.value,
                    "Server returned status code: ${response.status.value}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting tasks from server")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Failed to fetch tasks"
            )
        }
    }

    override suspend fun checkTaskAvailability(taskId: String): ApiResult<TaskAvailabilityResponseDto> {
        return apiService.checkTaskAvailability(taskId)
    }

    // в com.synngate.synnframe.data.remote.api.TaskApiImpl
    override suspend fun uploadTask(taskId: String, task: Task): ApiResult<Unit> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        return try {
            val url = "${server.apiUrl}/tasks/$taskId"
            Timber.d("Выгрузка задания на сервер: $url")

            // Логирование запроса
            Timber.d("Задание для выгрузки: ${task.id}, статус: ${task.status}, факт строк: ${task.factLines.size}")

            val response = client.post(url) {
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                header("User-Auth-Id", serverProvider.getCurrentUserId() ?: "")
                contentType(ContentType.Application.Json)
                setBody(task)
            }

            if (response.status.isSuccess()) {
                Timber.i("Задание $taskId успешно выгружено на сервер")
                ApiResult.Success(Unit)
            } else {
                // Логирование детальной информации об ошибке
                val responseText = response.bodyAsText()
                Timber.e("Ошибка выгрузки задания $taskId. Код: ${response.status.value}, Ответ: $responseText")

                ApiResult.Error(
                    response.status.value,
                    "Failed to upload task: ${response.status.description}. Response: $responseText"
                )
            }
        } catch (e: Exception) {
            // Подробное логирование исключения
            Timber.e(e, "Exception during task upload: $taskId")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                "Task upload failed: ${e.message ?: "Unknown error"}"
            )
        }
    }
}