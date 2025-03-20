package com.synngate.synnframe.presentation.ui.products.model

sealed class ProductDetailEvent {

    data class ShowSnackbar(val message: String) : ProductDetailEvent()

    data object NavigateBack : ProductDetailEvent()

    data object CopyProductInfoToClipboard : ProductDetailEvent()

    data class CopyBarcodeToClipboard(val barcode: String) : ProductDetailEvent()

    data object ToggleBarcodesPanel : ProductDetailEvent()
}