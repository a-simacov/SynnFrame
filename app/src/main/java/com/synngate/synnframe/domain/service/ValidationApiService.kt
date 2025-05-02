package com.synngate.synnframe.domain.service

import com.synngate.synnframe.data.remote.dto.ApiErrorItem
import com.synngate.synnframe.data.remote.dto.ValidationResponseDto
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.util.network.ApiUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Сервис для API-валидации
 * Выполняет запросы к API для проверки валидности данных
 */
class ValidationApiService(
    private val client: HttpClient,
    private val serverProvider: ServerProvider
) {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun validate(endpoint: String, value: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val server = serverProvider.getActiveServer() ?: throw Exception("No active server configured")
            val userId = serverProvider.getCurrentUserId() ?: ""
            val authHeader = "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}"

            // Формируем URL для запроса
            val requestUrl = buildString {
                append(server.apiUrl)
                if (!endpoint.startsWith("/")) append("/")
                append(endpoint)
                if (!endpoint.contains("?")) append("?")
                else append("&")
                append("value=$value")
            }

            Timber.d("Отправка запроса валидации на: $requestUrl")

            val response = client.get(requestUrl) {
                header("Authorization", authHeader)
                header("User-Auth-Id", userId)
            }

            if (response.status.isSuccess()) {
                try {
                    // Пробуем получить ответ в ожидаемом формате
                    val validationResponse = response.body<ValidationResponseDto>()
                    return@withContext Pair(validationResponse.result, validationResponse.message)
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка при разборе ответа валидации: ${e.message}")

                    // Возможно, ответ в другом формате - пробуем получить в виде текста
                    val responseText = response.bodyAsText()
                    Timber.d("Текст ответа: $responseText")

                    try {
                        // Проверяем, может это массив ошибок
                        val errorItems = jsonParser.decodeFromString<List<ApiErrorItem>>(responseText)
                        if (errorItems.isNotEmpty()) {
                            val errorMessage = errorItems.joinToString("\n") { it.title }
                            return@withContext Pair(false, errorMessage)
                        }
                    } catch (e2: Exception) {
                        // Если не удалось распарсить, возвращаем текст ответа как ошибку
                        return@withContext Pair(false, "Ошибка валидации: $responseText")
                    }

                    return@withContext Pair(false, "Неизвестная ошибка валидации")
                }
            } else {
                // Обработка ошибок HTTP
                val errorMessage = try {
                    val responseText = response.bodyAsText()
                    try {
                        val errorItems = jsonParser.decodeFromString<List<ApiErrorItem>>(responseText)
                        if (errorItems.isNotEmpty()) {
                            errorItems.joinToString("\n") { it.title }
                        } else {
                            "Ошибка сервера: ${response.status.value}"
                        }
                    } catch (e: Exception) {
                        "Ошибка сервера: ${response.status.value} - $responseText"
                    }
                } catch (e: Exception) {
                    "Ошибка сервера: ${response.status.value}"
                }

                return@withContext Pair(false, errorMessage)
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при выполнении запроса валидации: ${e.message}")
            return@withContext Pair(false, "Ошибка соединения: ${e.message}")
        }
    }
}