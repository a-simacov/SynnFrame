package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.AuthResponseDto

interface AuthApi {

    suspend fun authenticate(password: String, deviceInfo: Map<String, String>): ApiResult<AuthResponseDto>
}