package com.synngate.synnframe.domain.entity.operation

import com.synngate.synnframe.domain.entity.OperationMenuType
import kotlinx.serialization.Serializable

@Serializable
data class OperationMenuItem(
    val id: String,
    val name: String,
    val type: OperationMenuType = OperationMenuType.SHOW_LIST
)