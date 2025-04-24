package com.synngate.synnframe.presentation.ui.dynamicmenu.model

import com.synngate.synnframe.domain.entity.DynamicMenuItemType

sealed class DynamicMenuEvent {
    data class NavigateToDynamicTasks(
        val menuItemId: String,
        val menuItemName: String,
        val menuItemType: DynamicMenuItemType
    ) : DynamicMenuEvent()

    data object NavigateBack : DynamicMenuEvent()

    data class ShowSnackbar(val message: String) : DynamicMenuEvent()
}