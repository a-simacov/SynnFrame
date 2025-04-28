package com.synngate.synnframe.presentation.ui.dynamicmenu.product.model

sealed class DynamicProductDetailEvent {
    data object NavigateBack : DynamicProductDetailEvent()
    data class ShowSnackbar(val message: String) : DynamicProductDetailEvent()
    data object CopyProductInfoToClipboard : DynamicProductDetailEvent()
    data class CopyBarcodeToClipboard(val barcode: String) : DynamicProductDetailEvent()
    data object ToggleBarcodesPanel : DynamicProductDetailEvent()
}