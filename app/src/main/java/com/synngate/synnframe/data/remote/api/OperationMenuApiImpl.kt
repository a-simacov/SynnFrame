package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.domain.entity.operation.OperationMenuItem
import com.synngate.synnframe.domain.entity.operation.OperationTask
import com.synngate.synnframe.util.network.ApiUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import timber.log.Timber

class OperationMenuApiImpl(
    private val client: HttpClient,
    private val serverProvider: ServerProvider
) : OperationMenuApi {

    override suspend fun getOperationMenu(): ApiResult<List<OperationMenuItem>> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        return try {
            val url = "${server.apiUrl}/menu"
            val response = client.get(url) {
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                header("User-Auth-Id", serverProvider.getCurrentUserId() ?: "")
            }

            if (response.status.isSuccess()) {
                try {
                    val menuItems = response.body<List<OperationMenuItem>>()
                    ApiResult.Success(menuItems)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing operation menu JSON: ${e.message}")
                    ApiResult.Error(
                        HttpStatusCode.InternalServerError.value,
                        "Error parsing operation menu: ${e.message}"
                    )
                }
            } else {
                ApiResult.Error(
                    response.status.value,
                    "Server returned status code: ${response.status.value}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting operation menu from server")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Failed to fetch operation menu"
            )
        }
    }

    override suspend fun getOperationTasks(operationId: String): ApiResult<List<OperationTask>> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        return try {
            val url = "${server.apiUrl}/menu/$operationId"
            val response = client.get(url) {
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                header("User-Auth-Id", serverProvider.getCurrentUserId() ?: "")
            }

            if (response.status.isSuccess()) {
                try {
                    val tasks = response.body<List<OperationTask>>()
                    ApiResult.Success(tasks)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing operation tasks JSON: ${e.message}")
                    ApiResult.Error(
                        HttpStatusCode.InternalServerError.value,
                        "Error parsing operation tasks: ${e.message}"
                    )
                }
            } else {
                ApiResult.Error(
                    response.status.value,
                    "Server returned status code: ${response.status.value}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting operation tasks from server for operation ID: $operationId")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Failed to fetch operation tasks"
            )
        }
    }
}