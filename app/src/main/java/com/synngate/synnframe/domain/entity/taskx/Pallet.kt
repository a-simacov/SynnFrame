package com.synngate.synnframe.domain.entity.taskx

import kotlinx.serialization.Serializable

@Serializable
data class Pallet(
    val code: String,
    val isClosed: Boolean = false
)