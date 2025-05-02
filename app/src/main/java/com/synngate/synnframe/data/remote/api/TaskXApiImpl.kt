package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.FactActionRequestDto
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.util.network.ApiUtils
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
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

    /**
     * Базовый метод для выполнения запросов, связанных с действиями задания.
     *
     * @param taskId Идентификатор задания
     * @param endpoint Базовый эндпоинт API
     * @param action Действие (start, pause, finish и т.д.)
     * @param body Опциональное тело запроса
     * @return ApiResult<Boolean> с результатом операции
     */
    private suspend fun executeTaskAction(
        taskId: String,
        endpoint: String,
        action: String,
        body: Any? = null
    ): ApiResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Получаем данные сервера
            val server = serverProvider.getActiveServer()
                ?: return@withContext ApiResult.Error(
                    HttpStatusCode.InternalServerError.value,
                    "No active server configured"
                )

            // Разбираем эндпоинт и формируем URL
            val (_, path) = parseEndpoint(endpoint)
            val url = buildTaskActionUrl(server, path, taskId, action)

            // Получаем данные пользователя и авторизации
            val userId = serverProvider.getCurrentUserId() ?: ""
            val authHeader = buildAuthHeader(server)

            // Выполняем POST-запрос
            val response = executePostRequest(url, authHeader, userId, body)

            // Обрабатываем ответ
            if (response.status.isSuccess()) {
                ApiResult.Success(true)
            } else {
                ApiResult.Error(
                    response.status.value,
                    response.status.description ?: "Unknown error"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error executing task $action for taskId: $taskId")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Строит URL для запроса действия с заданием
     */
    private fun buildTaskActionUrl(
        server: Server,
        path: String,
        taskId: String,
        action: String
    ): String {
        return "${server.apiUrl}$path/$taskId/$action"
    }

    /**
     * Создает заголовок авторизации
     */
    private fun buildAuthHeader(server: Server): String {
        return "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}"
    }

    /**
     * Выполняет POST-запрос с заданными параметрами
     */
    private suspend fun executePostRequest(
        url: String,
        authHeader: String,
        userId: String,
        body: Any? = null
    ): HttpResponse {
        return httpClient.post(url) {
            header("Authorization", authHeader)
            header("User-Auth-Id", userId)

            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    /**
     * Разбирает строку эндпоинта и определяет HTTP-метод
     */
    private fun parseEndpoint(endpoint: String): Pair<HttpMethod, String> {
        // Если эндпоинт начинается с '/', используем GET
        if (endpoint.startsWith("/")) {
            return Pair(HttpMethod.GET, endpoint)
        }

        // Пробуем разобрать как "METHOD path"
        val parts = endpoint.trim().split(" ", limit = 2)
        if (parts.size == 2) {
            try {
                val method = HttpMethod.valueOf(parts[0].uppercase())
                val path = parts[1]

                // Форматируем путь, добавляя '/' если нужно
                val formattedPath = if (path.startsWith("/")) path else "/$path"
                return Pair(method, formattedPath)
            } catch (e: IllegalArgumentException) {
                Timber.w("Unsupported HTTP method: ${parts[0]}. Using GET by default.")
            }
        }

        // По умолчанию используем GET
        val defaultPath = if (endpoint.startsWith("/")) endpoint else "/$endpoint"
        return Pair(HttpMethod.GET, defaultPath)
    }

    override suspend fun startTask(taskId: String, endpoint: String): ApiResult<Boolean> {
        Timber.d("Starting task: $taskId with endpoint: $endpoint")
        return executeTaskAction(taskId, endpoint, "start")
    }

    override suspend fun pauseTask(taskId: String, endpoint: String): ApiResult<Boolean> {
        Timber.d("Pausing task: $taskId with endpoint: $endpoint")
        return executeTaskAction(taskId, endpoint, "pause")
    }

    override suspend fun finishTask(taskId: String, endpoint: String): ApiResult<Boolean> {
        Timber.d("Finishing task: $taskId with endpoint: $endpoint")
        return executeTaskAction(taskId, endpoint, "finish")
    }

    override suspend fun addFactAction(
        taskId: String,
        factAction: FactAction,
        endpoint: String
    ): ApiResult<Boolean> {
        Timber.d("Adding fact action for task: $taskId with endpoint: $endpoint")

        // Преобразуем доменный объект в DTO для отправки
        val requestDto = FactActionRequestDto.fromDomain(factAction)

        return executeTaskAction(taskId, endpoint, "fact-action", requestDto)
    }
}