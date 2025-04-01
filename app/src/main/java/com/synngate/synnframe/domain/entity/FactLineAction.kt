package com.synngate.synnframe.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class FactLineAction(
    val type: FactLineActionType,
    val order: Int,
    val promptText: String
)