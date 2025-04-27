package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.ApiErrorItem
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.util.network.ApiUtils
import com.synngate.synnframe.util.serialization.dynamicProductJson
import com.synngate.synnframe.util.serialization.dynamicTaskJson
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
import kotlinx.serialization.json.Json
import timber.log.Timber

class DynamicMenuApiImpl(
    private val client: HttpClient,
    private val serverProvider: ServerProvider
) : DynamicMenuApi {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    enum class HttpMethod {
        GET,
        POST,
        PUT,
        DELETE
    }

    override suspend fun getDynamicMenu(menuItemId: String?): ApiResult<List<DynamicMenuItem>> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        return try {
            val url = if (menuItemId == null) {
                "${server.apiUrl}/menu"
            } else {
                "${server.apiUrl}/menu/$menuItemId"
            }

            val response = client.get(url) {
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                header("User-Auth-Id", serverProvider.getCurrentUserId() ?: "")
            }

            if (response.status.isSuccess()) {
                try {
                    val menuItems = response.body<List<DynamicMenuItem>>()
                    ApiResult.Success(menuItems)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing dynamic menu JSON: ${e.message}")
                    ApiResult.Error(
                        HttpStatusCode.InternalServerError.value,
                        "Error parsing dynamic menu: ${e.message}"
                    )
                }
            } else {
                createErrorResult(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting dynamic menu from server")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Failed to fetch dynamic menu"
            )
        }
    }

    override suspend fun getDynamicTasks(
        endpoint: String,
        params: Map<String, String>
    ): ApiResult<List<DynamicTask>> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        val (httpMethod, path) = parseEndpoint(endpoint)

        val baseUrl = "${server.apiUrl}${path}"
        val url = if (params.isNotEmpty() && httpMethod == HttpMethod.GET) {
            val queryParams = params.entries.joinToString("&") { "${it.key}=${it.value}" }
            "$baseUrl?$queryParams"
        } else {
            baseUrl
        }

        val userId = serverProvider.getCurrentUserId() ?: ""
        val authHeader = "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}"

        return try {
            val response = when (httpMethod) {
                HttpMethod.GET -> client.get(url) {
                    header("Authorization", authHeader)
                    header("User-Auth-Id", userId)
                }
                HttpMethod.POST -> client.post(url) {
                    header("Authorization", authHeader)
                    header("User-Auth-Id", userId)
                    contentType(ContentType.Application.Json)
                    if (params.isNotEmpty()) {
                        setBody(params)
                    }
                }
                HttpMethod.PUT -> client.put(url) {
                    header("Authorization", authHeader)
                    header("User-Auth-Id", userId)
                    contentType(ContentType.Application.Json)
                    if (params.isNotEmpty()) {
                        setBody(params)
                    }
                }
                HttpMethod.DELETE -> client.delete(url) {
                    header("Authorization", authHeader)
                    header("User-Auth-Id", userId)
                }
            }

            if (response.status.isSuccess()) {
                try {
                    val responseText = response.bodyAsText()

                    // Используем нестандартное декодирование JSON
                    val dynamicTasksList = try {
                        // Используем созданный нами сериализатор
                        val jsonFormat = dynamicTaskJson
                        jsonFormat.decodeFromString<List<DynamicTask.Base>>(responseText)
                    } catch (e: Exception) {
                        Timber.e(e, "Error with custom decoding, trying default decoder")
                        // Если не получилось, пробуем стандартный декодер
                        response.body<List<DynamicTask.Base>>()
                    }

                    ApiResult.Success(dynamicTasksList)
                } catch (e: Exception) {
                    val bodyText = response.bodyAsText()
                    Timber.e(e, "Error parsing tasks response: ${e.message}\nResponse body: ${bodyText.take(500)}")

                    ApiResult.Error(
                        HttpStatusCode.InternalServerError.value,
                        "Error parsing tasks: ${e.message}"
                    )
                }
            } else {
                createErrorResult(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error executing $httpMethod tasks request")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Tasks request execution failed"
            )
        }
    }

    override suspend fun searchDynamicTask(endpoint: String, searchValue: String): ApiResult<DynamicTask> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        val (httpMethod, path) = parseEndpoint(endpoint)

        val baseUrl = "${server.apiUrl}${path}"
        val url = if (httpMethod == HttpMethod.GET) {
            "$baseUrl?value=$searchValue"
        } else {
            baseUrl
        }

        val userId = serverProvider.getCurrentUserId() ?: ""
        val authHeader = "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}"

        return try {
            val response = when (httpMethod) {
                HttpMethod.GET -> client.get(url) {
                    header("Authorization", authHeader)
                    header("User-Auth-Id", userId)
                }
                HttpMethod.POST -> client.post(url) {
                    header("Authorization", authHeader)
                    header("User-Auth-Id", userId)
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("value" to searchValue))
                }
                HttpMethod.PUT -> client.put(url) {
                    header("Authorization", authHeader)
                    header("User-Auth-Id", userId)
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("value" to searchValue))
                }
                HttpMethod.DELETE -> client.delete(url) {
                    header("Authorization", authHeader)
                    header("User-Auth-Id", userId)
                }
            }

            if (response.status.isSuccess()) {
                try {
                    // Получаем содержимое ответа для логирования и отладки
                    val responseText = response.bodyAsText()
                    Timber.d("Task search response body: $responseText")

                    // Пробуем разные подходы к парсингу
                    val dynamicTask = try {
                        val jsonFormat = dynamicTaskJson
                        jsonFormat.decodeFromString<DynamicTask.Base>(responseText)
                    } catch (e: Exception) {
                        Timber.e(e, "Error with custom decoding, trying default decoder")
                        // Если не получилось, пробуем стандартный декодер
                        response.body<DynamicTask.Base>()
                    }

                    ApiResult.Success(dynamicTask)
                } catch (e: Exception) {
                    val bodyText = response.bodyAsText()
                    Timber.e(e, "Error parsing task search response: ${e.message}\nResponse body: ${bodyText.take(500)}")

                    ApiResult.Error(
                        HttpStatusCode.InternalServerError.value,
                        "Error parsing task search result: ${e.message}"
                    )
                }
            } else {
                createErrorResult(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error searching task with $httpMethod request")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Task search failed"
            )
        }
    }

    override suspend fun getDynamicProducts(
        endpoint: String,
        params: Map<String, String>
    ): ApiResult<List<DynamicProduct>> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        val (httpMethod, path) = parseEndpoint(endpoint)

        val baseUrl = "${server.apiUrl}${path}"
        val url = if (params.isNotEmpty() && httpMethod == HttpMethod.GET) {
            val queryParams = params.entries.joinToString("&") { "${it.key}=${it.value}" }
            "$baseUrl?$queryParams"
        } else {
            baseUrl
        }

        val userId = serverProvider.getCurrentUserId() ?: ""
        val authHeader = "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}"

        return try {
            val response = when (httpMethod) {
                HttpMethod.GET -> client.get(url) {
                    header("Authorization", authHeader)
                    header("User-Auth-Id", userId)
                }
                HttpMethod.POST -> client.post(url) {
                    header("Authorization", authHeader)
                    header("User-Auth-Id", userId)
                    contentType(ContentType.Application.Json)
                    if (params.isNotEmpty()) {
                        setBody(params)
                    }
                }
                HttpMethod.PUT -> client.put(url) {
                    header("Authorization", authHeader)
                    header("User-Auth-Id", userId)
                    contentType(ContentType.Application.Json)
                    if (params.isNotEmpty()) {
                        setBody(params)
                    }
                }
                HttpMethod.DELETE -> client.delete(url) {
                    header("Authorization", authHeader)
                    header("User-Auth-Id", userId)
                }
            }

            if (response.status.isSuccess()) {
                try {
                    val responseText = response.bodyAsText()

                    // Используем нестандартное декодирование JSON
                    val dynamicProductsList = try {
                        // Используем созданный нами сериализатор
                        val jsonFormat = dynamicProductJson
                        jsonFormat.decodeFromString<List<DynamicProduct.Base>>(responseText)
                    } catch (e: Exception) {
                        Timber.e(e, "Error with custom decoding, trying default decoder")
                        // Если не получилось, пробуем стандартный декодер
                        response.body<List<DynamicProduct.Base>>()
                    }

                    ApiResult.Success(dynamicProductsList)
                } catch (e: Exception) {
                    val bodyText = response.bodyAsText()
                    Timber.e(e, "Error parsing products response: ${e.message}\nResponse body: ${bodyText.take(500)}")

                    ApiResult.Error(
                        HttpStatusCode.InternalServerError.value,
                        "Error parsing products: ${e.message}"
                    )
                }
            } else {
                createErrorResult(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error executing $httpMethod products request")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Products request execution failed"
            )
        }
    }

    private suspend fun createErrorResult(response: HttpResponse): ApiResult.Error {
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

                return ApiResult.Error(
                    statusCode,
                    if (responseBody.isNotBlank()) responseBody
                    else "Server returned status code: $statusCode"
                )
            }
        }

        return ApiResult.Error(
            statusCode,
            "Server returned status code: $statusCode"
        )
    }

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