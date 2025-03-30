package com.synngate.synnframe.presentation.ui.products.model

import android.graphics.Color

// com.synngate.synnframe.presentation.ui.products.model.ui
data class ProductListItemUiModel(
    val id: String,
    val name: String,
    val articleText: String,
    val mainUnitText: String,
    val backgroundTint: Color? = null,
    val isSelected: Boolean = false
)

// UI-модель для детального просмотра товара
data class ProductDetailUiModel(
    val id: String,
    val name: String,
    val articleText: String,
    val accountingModelText: String,
    val units: List<ProductUnitUiModel>,
    val barcodes: List<BarcodeUiModel>
)

data class ProductUnitUiModel(
    val id: String,
    val name: String,
    val quantityText: String,
    val mainBarcode: String,
    val isMainUnit: Boolean,
    val additionalBarcodesCount: Int
)

data class BarcodeUiModel(
    val barcode: String,
    val isMainBarcode: Boolean
)