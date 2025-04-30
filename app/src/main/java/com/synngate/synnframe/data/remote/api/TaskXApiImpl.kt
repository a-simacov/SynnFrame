package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.FactActionRequestDto
import com.synngate.synnframe.data.remote.dto.TaskXActionResponseDto
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.util.network.ApiUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class TaskXApiImpl(
    private val httpClient: HttpClient,
    private val serverProvider: ServerProvider
) : TaskXApi {

    override suspend fun startTask(taskId: String, endpoint: String): ApiResult<TaskXActionResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val server = serverProvider.getActiveServer() ?: throw Exception("No active server")
                val url = "${server.apiUrl}$endpoint/$taskId/start"

                val response: HttpResponse = httpClient.post(url) {
                    header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                    header("User-Auth-Id", serverProvider.getCurrentUserId() ?: "")
                }

                val responseBody = response.body<TaskXActionResponseDto>()
                ApiResult.Success(responseBody)
            } catch (e: Exception) {
                Timber.e(e, "Error starting task: $taskId")
                ApiResult.Error(message = e.message ?: "Unknown error", code = 500)
            }
        }
    }

    override suspend fun pauseTask(taskId: String, endpoint: String): ApiResult<TaskXActionResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val server = serverProvider.getActiveServer() ?: throw Exception("No active server")
                val url = "${server.apiUrl}$endpoint/$taskId/pause"

                val response: HttpResponse = httpClient.post(url) {
                    header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                    header("User-Auth-Id", serverProvider.getCurrentUserId() ?: "")
                }

                val responseBody = response.body<TaskXActionResponseDto>()
                ApiResult.Success(responseBody)
            } catch (e: Exception) {
                Timber.e(e, "Error pausing task: $taskId")
                ApiResult.Error(message = e.message ?: "Unknown error", code = 500)
            }
        }
    }

    override suspend fun finishTask(taskId: String, endpoint: String): ApiResult<TaskXActionResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val server = serverProvider.getActiveServer() ?: throw Exception("No active server")
                val url = "${server.apiUrl}$endpoint/$taskId/finish"

                val response: HttpResponse = httpClient.post(url) {
                    header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                    header("User-Auth-Id", serverProvider.getCurrentUserId() ?: "")
                }

                val responseBody = response.body<TaskXActionResponseDto>()
                ApiResult.Success(responseBody)
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
    ): ApiResult<TaskXActionResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val server = serverProvider.getActiveServer() ?: throw Exception("No active server")
                val url = "${server.apiUrl}$endpoint/$taskId/fact-action"

                val requestDto = FactActionRequestDto.fromDomain(factAction)

                val response: HttpResponse = httpClient.post(url) {
                    header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                    header("User-Auth-Id", serverProvider.getCurrentUserId() ?: "")
                    contentType(ContentType.Application.Json)
                    setBody(requestDto)
                }

                val responseBody = response.body<TaskXActionResponseDto>()
                ApiResult.Success(responseBody)
            } catch (e: Exception) {
                Timber.e(e, "Error adding fact action for task: $taskId")
                ApiResult.Error(message = e.message ?: "Unknown error", code = 500)
            }
        }
    }
}