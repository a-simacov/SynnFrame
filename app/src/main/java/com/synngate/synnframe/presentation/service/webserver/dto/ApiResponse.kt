package com.synngate.synnframe.presentation.service.webserver.dto

import kotlinx.serialization.Serializable

@Serializable
sealed class ApiResponse {
    abstract val status: String
    abstract val timestamp: Long
}

@Serializable
data class SuccessResponse<T>(
    val data: T,
    override val status: String = "success",
    override val timestamp: Long = System.currentTimeMillis()
) : ApiResponse()

@Serializable
data class ErrorResponse(
    val error: String,
    val code: Int = 500,
    override val status: String = "error",
    override val timestamp: Long = System.currentTimeMillis()
) : ApiResponse()