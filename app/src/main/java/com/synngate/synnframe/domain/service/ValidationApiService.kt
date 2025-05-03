package com.synngate.synnframe.domain.service

interface ValidationApiService {

    suspend fun validate(endpoint: String, value: String): Pair<Boolean, String?>
}