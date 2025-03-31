package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.AuthResponseDto
import com.synngate.synnframe.data.remote.service.ApiService

class AuthApiImpl(private val apiService: ApiService) : AuthApi {

    override suspend fun authenticate(password: String, deviceInfo: Map<String, String>): ApiResult<AuthResponseDto> {
        return apiService.authenticate(password, deviceInfo)
    }
}