package com.synngate.synnframe.domain.service

interface ValidationApiService {

    suspend fun validate(
        endpoint: String,
        value: String,
        context: Map<String, Any> = emptyMap()
    ): Pair<Boolean, String?>
}