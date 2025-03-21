package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.service.ServerProvider
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.util.network.ApiUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import timber.log.Timber

/**
 * Реализация интерфейса ProductApi
 */
class ProductApiImpl(
    private val client: HttpClient,
    private val serverProvider: ServerProvider
) : ProductApi {

    override suspend fun getProducts(): ApiResult<List<Product>> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        return try {
            val url = "${server.apiUrl}/products"
            val response = client.get(url) {
                // Basic аутентификация
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                // Заголовок с ID текущего пользователя
                header("User-Auth-Id", serverProvider.getCurrentUserId() ?: "")
            }

            if (response.status.isSuccess()) {
                val products = response.body<List<Product>>()
                ApiResult.Success(products)
            } else {
                ApiResult.Error(
                    response.status.value,
                    "Server returned status code: ${response.status.value}"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting products from server")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Unknown error while fetching products"
            )
        }
    }
}