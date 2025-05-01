package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.FactActionRequestDto
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.util.network.ApiUtils
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class TaskXApiImpl(
    private val httpClient: HttpClient,
    private val serverProvider: ServerProvider
) : TaskXApi {

    enum class HttpMethod {
        GET,
        POST,
        PUT,
        DELETE
    }

    override suspend fun startTask(taskId: String, endpoint: String): ApiResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val server = serverProvider.getActiveServer() ?: throw Exception("No active server")
                val (httpMethod, path) = parseEndpoint(endpoint)
                val url = "${server.apiUrl}$path/$taskId/start"
                val userId = serverProvider.getCurrentUserId() ?: ""
                val authHeader = "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}"

                // Используем метод согласно разбору endpoint
                val response: HttpResponse = when (httpMethod) {
                    HttpMethod.POST -> httpClient.post(url) {
                        header("Authorization", authHeader)
                        header("User-Auth-Id", userId)
                    }

                    HttpMethod.PUT -> httpClient.put(url) {
                        header("Authorization", authHeader)
                        header("User-Auth-Id", userId)
                    }

                    HttpMethod.GET -> httpClient.get(url) {
                        header("Authorization", authHeader)
                        header("User-Auth-Id", userId)
                    }

                    HttpMethod.DELETE -> httpClient.delete(url) {
                        header("Authorization", authHeader)
                        header("User-Auth-Id", userId)
                    }
                }

                if (response.status.isSuccess()) {
                    // Не используем task из ответа, только признак успешности
                    ApiResult.Success(true)
                } else {
                    ApiResult.Error(
                        message = response.status.description ?: "Unknown error",
                        code = 500
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting task: $taskId")
                ApiResult.Error(message = e.message ?: "Unknown error", code = 500)
            }
        }
    }

    override suspend fun pauseTask(taskId: String, endpoint: String): ApiResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val server = serverProvider.getActiveServer() ?: throw Exception("No active server")
                val (httpMethod, path) = parseEndpoint(endpoint)
                val url = "${server.apiUrl}$path/$taskId/pause"
                val userId = serverProvider.getCurrentUserId() ?: ""
                val authHeader = "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}"

                // Используем метод согласно разбору endpoint
                val response: HttpResponse = httpClient.post(url) {
                    header("Authorization", authHeader)
                    header("User-Auth-Id", userId)
                }

                if (response.status.isSuccess()) {
                    // Не используем task из ответа, только признак успешности
                    ApiResult.Success(true)
                } else {
                    ApiResult.Error(
                        message = response.status.description ?: "Unknown error",
                        code = 500
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error pausing task: $taskId")
                ApiResult.Error(message = e.message ?: "Unknown error", code = 500)
            }
        }
    }

    override suspend fun finishTask(taskId: String, endpoint: String): ApiResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val server = serverProvider.getActiveServer() ?: throw Exception("No active server")
                val (httpMethod, path) = parseEndpoint(endpoint)
                val url = "${server.apiUrl}$path/$taskId/finish"
                val userId = serverProvider.getCurrentUserId() ?: ""
                val authHeader = "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}"

                // Используем метод согласно разбору endpoint
                val response: HttpResponse = httpClient.post(url) {
                    header("Authorization", authHeader)
                    header("User-Auth-Id", userId)
                }

                if (response.status.isSuccess()) {
                    // Не используем task из ответа, только признак успешности
                    ApiResult.Success(true)
                } else {
                    ApiResult.Error(
                        message = response.status.description ?: "Unknown error",
                        code = 500
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error finishing task: $taskId")
                ApiResult.Error(message = e.message ?: "Unknown error", code = 500)
            }
        }
    }

    override suspend fun addFactAction(
        taskId: String,
        factAction: FactAction,
        endpoint: String
    ): ApiResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val server = serverProvider.getActiveServer() ?: throw Exception("No active server")
                val (httpMethod, path) = parseEndpoint(endpoint)
                val url = "${server.apiUrl}$path/$taskId/fact-action"
                val userId = serverProvider.getCurrentUserId() ?: ""
                val authHeader = "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}"

                val requestDto = FactActionRequestDto.fromDomain(factAction)

                // Используем метод согласно разбору endpoint, но для отправки данных используем POST по умолчанию
                val response: HttpResponse = when (httpMethod) {
                    HttpMethod.POST, HttpMethod.PUT -> httpClient.post(url) {
                        header("Authorization", authHeader)
                        header("User-Auth-Id", userId)
                        contentType(ContentType.Application.Json)
                        setBody(requestDto)
                    }
                    // Для GET и DELETE не имеет смысла отправлять тело, но для совместимости используем POST
                    else -> httpClient.post(url) {
                        header("Authorization", authHeader)
                        header("User-Auth-Id", userId)
                        contentType(ContentType.Application.Json)
                        setBody(requestDto)
                    }
                }

                if (response.status.isSuccess()) {
                    ApiResult.Success(true)
                } else {
                    ApiResult.Error(
                        message = response.status.description ?: "Unknown error",
                        code = 500
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error adding fact action for task: $taskId")
                ApiResult.Error(message = e.message ?: "Unknown error", code = 500)
            }
        }
    }

    /**
     * Разбирает строку endpoint на HTTP метод и путь
     * @param endpoint Строка endpoint, может содержать HTTP метод, например "GET /tasks"
     * @return Пара из HTTP метода и пути
     */
    private fun parseEndpoint(endpoint: String): Pair<HttpMethod, String> {
        if (endpoint.startsWith("/")) {
            return Pair(HttpMethod.GET, endpoint)
        }

        val parts = endpoint.trim().split(" ", limit = 2)
        if (parts.size == 2) {
            try {
                val method = HttpMethod.valueOf(parts[0].uppercase())
                val path = parts[1]

                val formattedPath = if (path.startsWith("/")) path else "/$path"
                return Pair(method, formattedPath)
            } catch (e: IllegalArgumentException) {
                Timber.w("Unsupported HTTP method: ${parts[0]}. Using GET by default.")
            }
        }

        val defaultPath = if (endpoint.startsWith("/")) endpoint else "/$endpoint"
        return Pair(HttpMethod.GET, defaultPath)
    }
}