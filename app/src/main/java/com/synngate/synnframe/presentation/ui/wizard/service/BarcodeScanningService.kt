package com.synngate.synnframe.presentation.ui.wizard.service

import timber.log.Timber

interface BarcodeScanningService {

    suspend fun processBarcode(
        barcode: String,
        expectedBarcode: String? = null,
        onResult: (found: Boolean, data: Any?) -> Unit,
        onError: (message: String) -> Unit
    )
}

abstract class BaseBarcodeScanningService : BarcodeScanningService {
    override suspend fun processBarcode(
        barcode: String,
        expectedBarcode: String?,
        onResult: (found: Boolean, data: Any?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            if (expectedBarcode != null && barcode != expectedBarcode) {
                onResult(false, null)
                return
            }

            findItemByBarcode(barcode, onResult, onError)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обработке штрихкода: $barcode")
            onError("Ошибка при обработке штрихкода: ${e.message}")
        }
    }

    protected abstract suspend fun findItemByBarcode(
        barcode: String,
        onResult: (found: Boolean, data: Any?) -> Unit,
        onError: (message: String) -> Unit
    )
}