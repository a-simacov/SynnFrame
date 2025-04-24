package com.synngate.synnframe.domain.entity.operation

import com.synngate.synnframe.domain.entity.DynamicMenuItemType
import kotlinx.serialization.Serializable

@Serializable
data class DynamicMenuItem(
    val id: String,
    val name: String,
    val type: DynamicMenuItemType = DynamicMenuItemType.SHOW_LIST
)