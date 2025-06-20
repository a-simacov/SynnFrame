package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.DynamicTasksResponseDto
import com.synngate.synnframe.data.remote.dto.SearchKeyValidationRequestDto
import com.synngate.synnframe.data.remote.dto.SearchKeyValidationResponseDto
import com.synngate.synnframe.data.remote.dto.TaskCreateRequestDto
import com.synngate.synnframe.data.remote.dto.TaskStartRequestDto
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.presentation.ui.taskx.dto.TaskXResponseDto
import io.ktor.client.HttpClient
import timber.log.Timber

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
    ): ApiResult<DynamicTasksResponseDto> {
        return executeApiRequest(endpoint, params)
    }

    override suspend fun createTask(endpoint: String, taskTypeId: String, searchKey: String?): ApiResult<TaskXResponseDto> {
        Timber.d("Creating new task with taskTypeId: $taskTypeId, searchKey: $searchKey")

        // Создаем DTO с ключом поиска, если он есть
        val requestBody = if (!searchKey.isNullOrEmpty()) {
            TaskCreateRequestDto(searchKey = searchKey)
        } else {
            null
        }

        return executeApiRequest(
            endpoint = "$endpoint/$taskTypeId/new",
            methodOverride = HttpMethod.POST,
            body = requestBody
        )
    }

    override suspend fun searchDynamicTask(
        endpoint: String,
        searchValue: String
    ): ApiResult<DynamicTasksResponseDto> {
        val result = executeApiRequest<DynamicTasksResponseDto>(
            endpoint,
            params = mapOf("value" to searchValue)
        )

        return result
//        return when (result) {
//            is ApiResult.Success -> {
//                if (result.data.isNotEmpty()) {
//                    ApiResult.Success(result.data.first())
//                } else {
//                    ApiResult.Error(HttpStatusCode.NotFound.value, "Task not found")
//                }
//            }
//            is ApiResult.Error -> result
//        }
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

    override suspend fun validateSearchKey(endpoint: String, key: String): ApiResult<SearchKeyValidationResponseDto> {
        Timber.d("Validating search key: $key")

        val requestBody = SearchKeyValidationRequestDto(key = key)

        return executeApiRequest(
            endpoint = endpoint,
            methodOverride = HttpMethod.POST,
            body = requestBody
        )
    }
}