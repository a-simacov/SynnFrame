package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.TaskTypeDto
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.domain.entity.TaskType
import com.synngate.synnframe.util.network.ApiUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import timber.log.Timber

class TaskTypeApiImpl(
    private val client: HttpClient,
    private val serverProvider: ServerProvider
) : TaskTypeApi {
    override suspend fun getTaskTypes(): ApiResult<List<TaskType>> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        return try {
            val url = "${server.apiUrl}/task-types"
            val response = client.get(url) {
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
            }

            if (response.status.isSuccess()) {
                try {
                    val taskTypeDtos = response.body<List<TaskTypeDto>>()
                    val taskTypes = taskTypeDtos.map { it.toDomainModel() }
                    ApiResult.Success(taskTypes)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing task types JSON: ${e.message}")
                    ApiResult.Error(
                        HttpStatusCode.InternalServerError.value,
                        "Error parsing task types: ${e.message}"
                    )
                }
            } else {
                ApiResult.Error(
                    response.status.value,
                    "Server returned status code: ${response.status.value}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting task types from server")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Failed to fetch task types"
            )
        }
    }
}