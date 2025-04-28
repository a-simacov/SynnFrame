package com.synngate.synnframe.presentation.ui.dynamicmenu.model

import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.ScreenElementsContainer

data class DynamicProductsState(
    val menuItemId: String = "",
    val menuItemName: String = "",
    val endpoint: String = "",
    override val screenSettings: ScreenSettings = ScreenSettings(),
    val products: List<DynamicProduct> = emptyList(),
    val searchValue: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val foundProduct: DynamicProduct? = null,
    val showBatchScannerDialog: Boolean = false,
    val showScannerDialog: Boolean = false,
    val selectedProduct: DynamicProduct? = null,
    val isSelectionMode: Boolean = false
) : ScreenElementsContainer {

    fun hasElement(element: ScreenElementType): Boolean {
        return screenSettings.screenElements.contains(element)
    }
}