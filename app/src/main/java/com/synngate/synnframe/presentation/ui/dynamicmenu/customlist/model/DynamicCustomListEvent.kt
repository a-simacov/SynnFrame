package com.synngate.synnframe.presentation.ui.dynamicmenu.customlist.model

import com.synngate.synnframe.domain.entity.operation.CustomListItem

sealed class DynamicCustomListEvent {
    data class ShowSnackbar(val message: String) : DynamicCustomListEvent()
    data class ShowItemDetails(val item: CustomListItem) : DynamicCustomListEvent()
}