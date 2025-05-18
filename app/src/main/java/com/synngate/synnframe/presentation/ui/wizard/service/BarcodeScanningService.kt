package com.synngate.synnframe.presentation.ui.wizard.service

interface BarcodeScanningService {

    suspend fun processBarcode(
        barcode: String,
        expectedBarcode: String? = null,
        onResult: (found: Boolean, data: Any?) -> Unit,
        onError: (message: String) -> Unit
    )
}

