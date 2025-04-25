package com.synngate.synnframe.presentation.ui.dynamicmenu.model

import com.synngate.synnframe.domain.entity.operation.ScreenSettings

sealed class DynamicMenuEvent {
    data class NavigateToDynamicTasks(
        val menuItemId: String,
        val menuItemName: String,
        val endpoint: String,
        val screenSettings: ScreenSettings
    ) : DynamicMenuEvent()

    data class NavigateToDynamicProducts(
        val menuItemId: String,
        val menuItemName: String,
        val endpoint: String,
        val screenSettings: ScreenSettings
    ) : DynamicMenuEvent()

    data object NavigateBack : DynamicMenuEvent()
    data class ShowSnackbar(val message: String) : DynamicMenuEvent()
}