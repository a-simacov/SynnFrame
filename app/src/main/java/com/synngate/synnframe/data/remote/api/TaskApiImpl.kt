package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.TaskAvailabilityResponseDto
import com.synngate.synnframe.data.remote.service.ApiService
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.domain.entity.Task
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
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
                // Basic аутентификация
                header("Authorization", "Basic ${getBasicAuth(server.login, server.password)}")
                // Заголовок с ID текущего пользователя
                header("User-Auth-Id", serverProvider.getCurrentUserId() ?: "")
            }

            if (response.status.isSuccess()) {
                val tasks = response.body<List<Task>>()
                ApiResult.Success(tasks)
            } else {
                ApiResult.Error(
                    response.status.value,
                    "Server returned status: ${response.status}"
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

    /**
     * Получение строки Basic аутентификации
     */
    private fun getBasicAuth(login: String, password: String): String {
        val credentials = "$login:$password"
        return java.util.Base64.getEncoder().encodeToString(credentials.toByteArray())
    }
}