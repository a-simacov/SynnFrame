package com.synngate.synnframe.presentation.ui.dynamicmenu.model

import com.synngate.synnframe.domain.entity.DynamicMenuItemType
import com.synngate.synnframe.domain.entity.operation.DynamicTask

data class DynamicTasksState(
    val menuItemId: String = "",
    val menuItemName: String = "",
    val menuItemType: DynamicMenuItemType = DynamicMenuItemType.SHOW_LIST,
    val tasks: List<DynamicTask> = emptyList(),
    val searchValue: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val foundTask: DynamicTask? = null
)