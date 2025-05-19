package com.synngate.synnframe.data.remote.api

import com.synngate.synnframe.data.remote.dto.ActionSearchResponseDto

interface ActionSearchApi {

    suspend fun searchAction(
        endpoint: String,
        searchValue: String
    ): ApiResult<ActionSearchResponseDto>
}