package com.synngate.synnframe.domain.entity.taskx

import kotlinx.serialization.Serializable

@Serializable
data class Pallet(
    val code: String,          // Код паллеты
    val isClosed: Boolean = false // Закрыта или открыта
)