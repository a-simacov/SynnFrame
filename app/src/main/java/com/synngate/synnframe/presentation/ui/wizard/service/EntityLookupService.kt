package com.synngate.synnframe.presentation.ui.wizard.service

interface EntityLookupService<T> {

    suspend fun processBarcode(
        barcode: String,
        expectedBarcode: String? = null,
        onResult: (found: Boolean, data: T?) -> Unit,
        onError: (message: String) -> Unit
    )

    suspend fun searchEntities(query: String, additionalParams: Map<String, Any> = emptyMap()): List<T>
}