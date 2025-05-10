package com.synngate.synnframe.data.remote.service

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.data.remote.dto.AuthRequestDto
import com.synngate.synnframe.data.remote.dto.AuthResponseDto
import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.util.network.ApiUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import timber.log.Timber

interface ApiService {

    suspend fun testConnection(server: Server): ApiResult<Unit>

    suspend fun authenticate(password: String, deviceInfo: Map<String, String>): ApiResult<AuthResponseDto>
}

class ApiServiceImpl(
    private val client: HttpClient,
    private val serverProvider: ServerProvider
) : ApiService {

    override suspend fun testConnection(server: Server): ApiResult<Unit> {
        return try {
            val response = client.get(server.echoUrl) {
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
            }

            if (response.status.isSuccess()) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(response.status.value, "Server returned ${response.status}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error testing connection to server: ${server.host}:${server.port}")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Connection failed"
            )
        }
    }

    override suspend fun authenticate(password: String, deviceInfo: Map<String, String>): ApiResult<AuthResponseDto> {
        val server = serverProvider.getActiveServer() ?: return ApiResult.Error(
            HttpStatusCode.InternalServerError.value,
            "No active server configured"
        )

        return try {
            val url = "${server.apiUrl}/auth"
            val response = client.post(url) {
                header("User-Auth-Pass", password)
                header("Authorization", "Basic ${ApiUtils.getBasicAuth(server.login, server.password)}")
                contentType(ContentType.Application.Json)
                setBody(
                    AuthRequestDto(
                        deviceIp = deviceInfo["deviceIp"] ?: "",
                        deviceId = deviceInfo["deviceId"] ?: "",
                        deviceName = deviceInfo["deviceName"] ?: ""
                    )
                )
            }

            if (response.status.isSuccess()) {
                val authResponse = response.body<AuthResponseDto>()
                ApiResult.Success(authResponse)
            } else {
                try {
                    val errorBody = response.body<Map<String, String>>()
                    val errorMessage = errorBody["message"] ?: "Authentication failed"
                    ApiResult.Error(response.status.value, errorMessage)
                } catch (e: Exception) {
                    ApiResult.Error(response.status.value, "Authentication failed")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error authenticating user")
            ApiResult.Error(
                HttpStatusCode.InternalServerError.value,
                e.message ?: "Authentication error"
            )
        }
    }
}