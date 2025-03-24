package com.synngate.synnframe.data.remote.service

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.dto.AuthRequestDto
import com.synngate.synnframe.data.remote.dto.AuthResponseDto
import com.synngate.synnframe.data.remote.dto.TaskAvailabilityResponseDto
import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.util.network.ApiUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import timber.log.Timber

/**
 * Интерфейс сервиса API
 */
interface ApiService {
    /**
     * Тестирование подключения к серверу
     *
     * @param server сервер для тестирования
     * @return ответ сервера
     */
    suspend fun testConnection(server: Server): ApiResult<Unit>

    /**
     * Аутентификация пользователя
     *
     * @param password пароль пользователя
     * @param deviceInfo информация об устройстве
     * @return ответ с данными пользователя
     */
    suspend fun authenticate(password: String, deviceInfo: Map<String, String>): ApiResult<AuthResponseDto>

    /**
     * Проверка доступности задания
     *
     * @param taskId идентификатор задания
     * @return ответ с информацией о доступности задания
     */
    suspend fun checkTaskAvailability(taskId: String): ApiResult<TaskAvailabilityResponseDto>

    /**
     * Выгрузка задания на сервер
     *
     * @param taskId идентификатор задания
     * @param task данные задания
     * @return ответ с результатом выгрузки
     */
    suspend fun uploadTask(taskId: String, task: Task): ApiResult<Unit>
}

/**
 * Реализация сервиса API с использованием Ktor
 */
class ApiServiceImpl(
    private val client: HttpClient,
    private val serverProvider: ServerProvider
) : ApiService {

    override suspend fun testConnection(server: Server): ApiResult<Unit> {
        return try {
            val response = client.get(server.echoUrl) {
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
            }

            if (response.status.isSuccess()) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(response.status.value, "Server returned ${response.status}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error testing connection to server: ${server.host}:${server.port}")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Connection failed"
            )
        }
    }

    override suspend fun authenticate(password: String, deviceInfo: Map<String, String>): ApiResult<AuthResponseDto> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        return try {
            val url = "${server.apiUrl}/auth"
            val response = client.post(url) {
                header("User-Auth-Pass", password)
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                contentType(ContentType.Application.Json)
                setBody(
                    AuthRequestDto(
                        deviceIp = deviceInfo["deviceIp"] ?: "",
                        deviceId = deviceInfo["deviceId"] ?: "",
                        deviceName = deviceInfo["deviceName"] ?: ""
                    )
                )
            }

            if (response.status.isSuccess()) {
                val authResponse = response.body<AuthResponseDto>()
                ApiResult.Success(authResponse)
            } else {
                try {
                    val errorBody = response.body<Map<String, String>>()
                    val errorMessage = errorBody["message"] ?: "Authentication failed"
                    ApiResult.Error(response.status.value, errorMessage)
                } catch (e: Exception) {
                    ApiResult.Error(response.status.value, "Authentication failed")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error authenticating user")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Authentication error"
            )
        }
    }

    override suspend fun checkTaskAvailability(taskId: String): ApiResult<TaskAvailabilityResponseDto> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        return try {
            val url = "${server.apiUrl}/tasks/$taskId/availability"
            val response = client.get(url) {
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                header("User-Auth-Id", serverProvider.getCurrentUserId() ?: "")
            }

            if (response.status.isSuccess()) {
                val availability = response.body<TaskAvailabilityResponseDto>()
                ApiResult.Success(availability)
            } else {
                ApiResult.Error(response.status.value, "Failed to check task availability")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking task availability: $taskId")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Task availability check failed"
            )
        }
    }

    override suspend fun uploadTask(taskId: String, task: Task): ApiResult<Unit> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        return try {
            val url = "${server.apiUrl}/tasks/$taskId"
            val response = client.post(url) {
                // Basic аутентификация
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                // Заголовок с ID текущего пользователя
                header("User-Auth-Id", serverProvider.getCurrentUserId() ?: "")
                // Тело запроса
                contentType(ContentType.Application.Json)
                setBody(task)
            }

            if (response.status.isSuccess()) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(response.status.value, "Failed to upload task")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error uploading task: $taskId")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Task upload failed"
            )
        }
    }
}

/**
 * Интерфейс для получения активного сервера и текущего пользователя
 * (этот интерфейс остается без изменений)
 */
interface ServerProvider {
    /**
     * Получение активного сервера
     */
    suspend fun getActiveServer(): Server?

    /**
     * Получение идентификатора текущего пользователя
     */
    suspend fun getCurrentUserId(): String?
}