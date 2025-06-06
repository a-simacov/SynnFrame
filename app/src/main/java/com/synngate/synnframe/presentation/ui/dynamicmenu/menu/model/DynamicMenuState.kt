package com.synngate.synnframe.presentation.ui.dynamicmenu.menu.model

import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem

data class DynamicMenuState(
    val menuItems: List<DynamicMenuItem> = emptyList(),
    val currentMenuItemId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigationHistory: List<String?> = emptyList()
)