package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ActionSearchResponseDto(
    val result: String? = null,
    val message: String? = null
)