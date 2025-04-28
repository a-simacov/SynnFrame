package com.synngate.synnframe.presentation.ui.dynamicmenu.model

import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.ScreenElementsContainer

data class DynamicTasksState(
    val menuItemId: String = "",
    val menuItemName: String = "",
    val endpoint: String = "",
    override val screenSettings: ScreenSettings = ScreenSettings(),
    val tasks: List<DynamicTask> = emptyList(),
    val searchValue: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val foundTask: DynamicTask? = null
) : ScreenElementsContainer {

    fun hasElement(element: ScreenElementType): Boolean {
        return screenSettings.screenElements.contains(element)
    }
}