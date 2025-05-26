package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.TaskStartRequestDto
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.presentation.ui.taskx.dto.TaskXResponseDto
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode

class DynamicMenuApiImpl(
    httpClient: HttpClient,
    serverProvider: ServerProvider
) : BaseApiImpl(httpClient, serverProvider), DynamicMenuApi {

    override suspend fun getDynamicMenu(menuItemId: String?): ApiResult<List<DynamicMenuItem>> {
        val endpoint = if (menuItemId == null) "/menu" else "/menu/$menuItemId"
        return executeApiRequest(endpoint)
    }

    override suspend fun getDynamicTasks(
        endpoint: String,
        params: Map<String, String>
    ): ApiResult<List<DynamicTask>> {
        return executeApiRequest(endpoint, params)
    }

    override suspend fun searchDynamicTask(
        endpoint: String,
        searchValue: String
    ): ApiResult<DynamicTask> {
        val result = executeApiRequest<List<DynamicTask>>(
            endpoint,
            params = mapOf("value" to searchValue)
        )

        return when (result) {
            is ApiResult.Success -> {
                if (result.data.isNotEmpty()) {
                    ApiResult.Success(result.data.first())
                } else {
                    ApiResult.Error(HttpStatusCode.NotFound.value, "Task not found")
                }
            }
            is ApiResult.Error -> result
        }
    }

    override suspend fun getDynamicProducts(
        endpoint: String,
        params: Map<String, String>
    ): ApiResult<List<DynamicProduct>> {
        return executeApiRequest(endpoint, params)
    }

    override suspend fun startDynamicTask(
        endpoint: String,
        taskId: String
    ): ApiResult<TaskXResponseDto> {
        val requestBody = TaskStartRequestDto(taskId = taskId, start = true)

        return executeApiRequest(
            endpoint = endpoint,
            methodOverride = HttpMethod.POST,
            body = requestBody
        )
    }

    override suspend fun getTaskDetails(
        endpoint: String,
        taskId: String
    ): ApiResult<DynamicTask> {
        return executeApiRequest(endpoint)
    }
}