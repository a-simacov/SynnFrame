package com.synngate.synnframe.domain.entity.operation

import com.synngate.synnframe.domain.entity.DynamicMenuItemType
import kotlinx.serialization.Serializable

@Serializable
data class DynamicMenuItem(
    val id: String,
    val name: String,
    val type: DynamicMenuItemType = DynamicMenuItemType.SUBMENU,
    val parentId: String? = null,  // null - корневой элемент, не null - элемент подменю
    val endpoint: String? = null,  // API-эндпоинт для получения данных
    val screenSettings: ScreenSettings = ScreenSettings()
)