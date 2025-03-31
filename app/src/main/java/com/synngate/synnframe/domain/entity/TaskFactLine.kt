package com.synngate.synnframe.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class TaskFactLine(
    val id: String,

    val taskId: String,

    val productId: String,

    val quantity: Float,

    val binCode: String? = null
)