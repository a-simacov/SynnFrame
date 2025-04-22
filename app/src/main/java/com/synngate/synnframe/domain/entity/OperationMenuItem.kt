package com.synngate.synnframe.domain.entity.operation

import kotlinx.serialization.Serializable

@Serializable
data class OperationMenuItem(
    val id: String,
    val name: String
)