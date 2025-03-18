// Файл: com.synngate.synnframe.presentation.ui.products.ProductDetailState.kt

package com.synngate.synnframe.presentation.ui.products.model

import com.synngate.synnframe.domain.entity.Product

/**
 * Состояние экрана деталей товара
 */
data class ProductDetailState(
    // Данные товара
    val product: Product? = null,

    // Статус загрузки
    val isLoading: Boolean = true,

    // Сообщение об ошибке
    val error: String? = null,

    // Текущая выбранная единица измерения
    val selectedUnitId: String? = null,

    // Показывать ли панель штрихкодов
    val showBarcodes: Boolean = false
)