package com.synngate.synnframe.presentation.ui.products.model

import com.synngate.synnframe.domain.entity.Product

data class ProductDetailState(

    val product: Product? = null,

    val isLoading: Boolean = true,

    val error: String? = null,

    val selectedUnitId: String? = null,

    val showBarcodes: Boolean = false,

    val isInfoCopied: Boolean = false,

    val lastCopiedBarcode: String? = null,

    val showExtendedUnitInfo: Boolean = false,

    // Показывать ли диалог сканирования штрихкода
    val showBarcodeScanner: Boolean = false
)