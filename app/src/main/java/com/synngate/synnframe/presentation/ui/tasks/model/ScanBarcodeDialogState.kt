package com.synngate.synnframe.presentation.ui.tasks.model

data class ScanBarcodeDialogState(
    val isScannerActive: Boolean = true,
    val additionalQuantity: String = "",
    val isError: Boolean = false,
    val lastScannedBarcode: String? = null
)