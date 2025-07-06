package com.synngate.synnframe.presentation.ui.dynamicmenu.customlist.model

import com.synngate.synnframe.domain.entity.operation.CustomListItem
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import com.synngate.synnframe.presentation.common.search.SearchResultType
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.ScreenElementsContainer

data class DynamicCustomListState(
    val menuItemId: String = "",
    val menuItemName: String = "",
    val endpoint: String = "",
    override val screenSettings: ScreenSettings = ScreenSettings(),
    val items: List<CustomListItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchKey: String = "",
    val searchResultType: SearchResultType? = null,
    val lastSearchQuery: String = ""
) : ScreenElementsContainer {
    fun hasElement(elementType: ScreenElementType): Boolean {
        return screenSettings.screenElements.contains(elementType)
    }

    fun shouldShowSearchIndicator(): Boolean {
        return searchResultType != null && lastSearchQuery.isNotEmpty()
    }
}