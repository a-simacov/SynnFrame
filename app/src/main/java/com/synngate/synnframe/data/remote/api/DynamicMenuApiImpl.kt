package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.util.network.ApiUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import timber.log.Timber

class DynamicMenuApiImpl(
    private val client: HttpClient,
    private val serverProvider: ServerProvider
) : DynamicMenuApi {

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
                ApiResult.Error(
                    response.status.value,
                    "Server returned status code: ${response.status.value}"
                )
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

        return try {
            val baseUrl = "${server.apiUrl}${endpoint}"
            val url = if (params.isNotEmpty()) {
                val queryParams = params.entries.joinToString("&") { "${it.key}=${it.value}" }
                "$baseUrl?$queryParams"
            } else {
                baseUrl
            }

            val response = client.get(url) {
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                header("User-Auth-Id", serverProvider.getCurrentUserId() ?: "")
            }

            if (response.status.isSuccess()) {
                try {
                    val tasks = response.body<List<DynamicTask>>()
                    ApiResult.Success(tasks)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing dynamic tasks JSON: ${e.message}")
                    ApiResult.Error(
                        HttpStatusCode.InternalServerError.value,
                        "Error parsing dynamic tasks: ${e.message}"
                    )
                }
            } else {
                ApiResult.Error(
                    response.status.value,
                    "Server returned status code: ${response.status.value}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting dynamic tasks from server")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Failed to fetch dynamic tasks"
            )
        }
    }

    override suspend fun searchDynamicTask(endpoint: String, searchValue: String): ApiResult<DynamicTask> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        return try {
            val url = "${server.apiUrl}${endpoint}?value=$searchValue"
            val response = client.get(url) {
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                header("User-Auth-Id", serverProvider.getCurrentUserId() ?: "")
            }

            if (response.status.isSuccess()) {
                try {
                    val task = response.body<DynamicTask>()
                    ApiResult.Success(task)
                } catch (e: Exception) {
                    ApiResult.Error(
                        HttpStatusCode.InternalServerError.value,
                        e.message ?: "Error parsing search result"
                    )
                }
            } else {
                val errorText = response.bodyAsText()
                ApiResult.Error(
                    response.status.value,
                    errorText
                )
            }
        } catch (e: Exception) {
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Error searching task"
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

        return try {
            val baseUrl = "${server.apiUrl}${endpoint}"
            val url = if (params.isNotEmpty()) {
                val queryParams = params.entries.joinToString("&") { "${it.key}=${it.value}" }
                "$baseUrl?$queryParams"
            } else {
                baseUrl
            }

            val response = client.get(url) {
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                header("User-Auth-Id", serverProvider.getCurrentUserId() ?: "")
            }

            if (response.status.isSuccess()) {
                try {
                    val products = response.body<List<DynamicProduct>>()
                    ApiResult.Success(products)
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing dynamic products JSON: ${e.message}")
                    ApiResult.Error(
                        HttpStatusCode.InternalServerError.value,
                        "Error parsing dynamic products: ${e.message}"
                    )
                }
            } else {
                ApiResult.Error(
                    response.status.value,
                    "Server returned status code: ${response.status.value}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting dynamic products from server")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Failed to fetch dynamic products"
            )
        }
    }
}