package com.synngate.synnframe.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorItem(
    val code: String,
    val title: String
)