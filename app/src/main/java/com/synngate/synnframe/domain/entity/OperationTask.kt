package com.synngate.synnframe.domain.entity.operation

import kotlinx.serialization.Serializable

@Serializable
data class OperationTask(
    val id: String,
    val name: String
)