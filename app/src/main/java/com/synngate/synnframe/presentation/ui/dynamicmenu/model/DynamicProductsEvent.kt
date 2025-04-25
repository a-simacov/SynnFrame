package com.synngate.synnframe.presentation.ui.dynamicmenu.model

import com.synngate.synnframe.domain.entity.operation.DynamicProduct

sealed class DynamicProductsEvent {
    data object NavigateBack : DynamicProductsEvent()
    data class ShowSnackbar(val message: String) : DynamicProductsEvent()
    data class NavigateToProductDetail(val product: DynamicProduct) : DynamicProductsEvent()
    data class ReturnSelectedProductId(val productId: String) : DynamicProductsEvent()
}