package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommonResponseDto(
    val success: Boolean,
    val status: Int,
    val message: String = "",
    val userMessage: String? = null
)