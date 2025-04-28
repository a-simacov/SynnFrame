package com.synngate.synnframe.presentation.ui.dynamicmenu.product.model

import com.synngate.synnframe.domain.entity.operation.DynamicProduct

data class DynamicProductDetailState(
    val product: DynamicProduct? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedUnitId: String? = null,
    val showBarcodes: Boolean = false,
    val isInfoCopied: Boolean = false,
    val lastCopiedBarcode: String? = null,
    val showExtendedUnitInfo: Boolean = false
)