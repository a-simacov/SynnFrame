package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.ApiErrorItem
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.util.network.ApiUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

abstract class BaseApiImpl(
    protected val httpClient: HttpClient,
    protected val serverProvider: ServerProvider
) {
    protected val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    enum class HttpMethod {
        GET,
        POST,
        PUT,
        DELETE
    }

    protected fun parseEndpoint(endpoint: String): Pair<HttpMethod, String> {
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

    protected fun buildUrl(
        server: Server,
        path: String,
        params: Map<String, String> = emptyMap(),
        method: HttpMethod = HttpMethod.GET
    ): String {
        val baseUrl = "${server.apiUrl}${path}"

        return if (params.isNotEmpty() && method == HttpMethod.GET) {
            val queryParams = params.entries.joinToString("&") { "${it.key}=${it.value}" }
            "$baseUrl?$queryParams"
        } else {
            baseUrl
        }
    }

    protected fun buildAuthHeader(server: Server): String {
        return "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}"
    }

    protected suspend fun executeHttpRequest(
        method: HttpMethod,
        url: String,
        authHeader: String,
        userId: String,
        body: Any? = null,
        params: Map<String, String> = emptyMap()
    ): HttpResponse {
        return when (method) {
            HttpMethod.GET -> httpClient.get(url) {
                header("Authorization", authHeader)
                header("User-Auth-Id", userId)
            }
            HttpMethod.POST -> httpClient.post(url) {
                header("Authorization", authHeader)
                header("User-Auth-Id", userId)
                contentType(ContentType.Application.Json)
                if (body != null) setBody(body) else if (params.isNotEmpty()) setBody(params)
            }
            HttpMethod.PUT -> httpClient.put(url) {
                header("Authorization", authHeader)
                header("User-Auth-Id", userId)
                contentType(ContentType.Application.Json)
                if (body != null) setBody(body) else if (params.isNotEmpty()) setBody(params)
            }
            HttpMethod.DELETE -> httpClient.delete(url) {
                header("Authorization", authHeader)
                header("User-Auth-Id", userId)
            }
        }
    }

    protected suspend inline fun <reified T> processResponse(response: HttpResponse): ApiResult<T> {
        return if (response.status.isSuccess()) {
            try {
                val result = response.body<T>()
                ApiResult.Success(result)
            } catch (e: Exception) {
                Timber.e(e, "Error parsing response: ${e.message}")
                createApiError("Error parsing response: ${e.message}")
            }
        } else {
            createErrorResult(response)
        }
    }

    protected inline fun <reified T> createApiError(
        message: String,
        code: Int = HttpStatusCode.InternalServerError.value
    ): ApiResult<T> {
        return ApiResult.Error(code, message)
    }

    protected suspend inline fun <reified T> createErrorResult(response: HttpResponse): ApiResult<T> {
        val statusCode = response.status.value
        val responseBody = response.bodyAsText()

        try {
            val errorItems = jsonParser.decodeFromString<List<ApiErrorItem>>(responseBody)
            if (errorItems.isNotEmpty()) {
                val errorMessage = errorItems.joinToString("\n") { it.title }
                return ApiResult.Error(statusCode, errorMessage)
            }
        } catch (e: Exception) {
            Timber.d("Response is not a list of error items: ${e.message}")

            try {
                val errorItem = jsonParser.decodeFromString<ApiErrorItem>(responseBody)
                return ApiResult.Error(statusCode, errorItem.title)
            } catch (e: Exception) {
                Timber.d("Response is not a single error item: ${e.message}")
            }
        }

        return ApiResult.Error(
            statusCode,
            if (responseBody.isNotBlank()) responseBody
            else "Server returned status code: $statusCode"
        )
    }

    /**
     * Выполняет API-запрос с обработкой ошибок и типизированным ответом
     * @param endpoint Строка эндпоинта (может начинаться с HTTP-метода)
     * @param params Параметры запроса
     * @param methodOverride Принудительно заданный метод (перекрывает метод из endpoint)
     * @param body Тело запроса (используется вместо params для не-GET запросов)
     */
    protected suspend inline fun <reified T> executeApiRequest(
        endpoint: String,
        params: Map<String, String> = emptyMap(),
        methodOverride: HttpMethod? = null,
        body: Any? = null
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val server = serverProvider.getActiveServer() ?:
            return@withContext createApiError<T>("No active server configured")

            val (method, path) = parseEndpoint(endpoint)
            val actualMethod = methodOverride ?: method

            val url = buildUrl(server, path, params, actualMethod)

            val userId = serverProvider.getCurrentUserId() ?: ""
            val authHeader = buildAuthHeader(server)

            val response = executeHttpRequest(actualMethod, url, authHeader, userId, body, params)

            processResponse<T>(response)
        } catch (e: Exception) {
            Timber.e(e, "Error executing request to $endpoint: ${e.message}")
            createApiError("Network error: ${e.message}")
        }
    }
}