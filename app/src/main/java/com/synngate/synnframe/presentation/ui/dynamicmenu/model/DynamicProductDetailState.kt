package com.synngate.synnframe.presentation.ui.dynamicmenu.model

import com.synngate.synnframe.domain.entity.operation.DynamicProduct

data class DynamicProductDetailState(
    val product: DynamicProduct = DynamicProduct.Empty,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedUnitId: String = "",
    val showBarcodes: Boolean = false,
    val isInfoCopied: Boolean = false,
    val lastCopiedBarcode: String? = null,
    val showExtendedUnitInfo: Boolean = false
)