// Ячейка X (BinX) - исправленная
package com.synngate.synnframe.domain.entity.taskx

import kotlinx.serialization.Serializable

@Serializable
data class BinX(
    val code: String,
    val zone: String
)