package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ValidationResponseDto(
    val result: Boolean,
    val message: String? = null
)