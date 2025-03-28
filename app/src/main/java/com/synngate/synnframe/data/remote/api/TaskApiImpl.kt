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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
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

    override suspend fun uploadTask(taskId: String, task: Task): ApiResult<Unit> {
        return apiService.uploadTask(taskId, task)
    }
}