package com.synngate.synnframe.presentation.ui.tasks.model

data class ScanBarcodeDialogState(
    val isScannerActive: Boolean = true,
    val lastScannedBarcode: String? = null,
    val scannerMessage: String? = null,
)