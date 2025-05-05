package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.CommonResponseDto
import com.synngate.synnframe.data.remote.dto.FactActionRequestDto
import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import io.ktor.client.HttpClient

class TaskXApiImpl(
    httpClient: HttpClient,
    serverProvider: ServerProvider
) : BaseApiImpl(httpClient, serverProvider), TaskXApi {

    private suspend fun executeTaskAction(
        taskId: String,
        endpoint: String,
        action: String,
        body: Any? = null
    ): ApiResult<CommonResponseDto> {
        val (_, path) = parseEndpoint(endpoint)
        val targetPath = "$path/$taskId/$action"

        return executeApiRequest(
            endpoint = "POST $targetPath",
            body = body
        )
    }

    override suspend fun startTask(taskId: String, endpoint: String): ApiResult<CommonResponseDto> {
        return executeTaskAction(taskId, endpoint, "start")
    }

    override suspend fun pauseTask(taskId: String, endpoint: String): ApiResult<CommonResponseDto> {
        return executeTaskAction(taskId, endpoint, "pause")
    }

    override suspend fun finishTask(
        taskId: String,
        endpoint: String
    ): ApiResult<CommonResponseDto> {
        return executeTaskAction(taskId, endpoint, "finish")
    }

    override suspend fun addFactAction(
        taskId: String,
        factAction: FactAction,
        endpoint: String,
        finalizePlannedAction: Boolean
    ): ApiResult<CommonResponseDto> {
        val requestDto = FactActionRequestDto.fromDomain(factAction)

        // Добавляем параметр finalizePlannedAction в запрос
        val params = mapOf("finalizePlannedAction" to finalizePlannedAction.toString())

        val (_, path) = parseEndpoint(endpoint)
        val targetPath = "$path/$taskId/fact-action"

        // Выполняем запрос с дополнительными параметрами
        return executeApiRequest(
            endpoint = "POST $targetPath",
            body = requestDto,
            params = params
        )
    }
}