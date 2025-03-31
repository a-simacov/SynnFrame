package com.synngate.synnframe.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class Bin(
    val code: String,
    val name: String? = null
)