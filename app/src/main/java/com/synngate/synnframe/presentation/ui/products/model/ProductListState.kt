package com.synngate.synnframe.presentation.ui.product.model

import com.synngate.synnframe.domain.entity.Product

/**
 * Состояние экрана списка товаров
 */
data class ProductListState(
    // Список товаров
    val products: List<Product> = emptyList(),

    // Флаг загрузки данных
    val isLoading: Boolean = false,

    // Ошибка (если есть)
    val error: String? = null,

    // Поисковый запрос
    val searchQuery: String = "",

    // Режим выбора товара (для задания)
    val isSelectionMode: Boolean = false,

    // Признак синхронизации с сервером
    val isSyncing: Boolean = false,

    // Время последней синхронизации
    val lastSyncTime: String? = null,

    // Общее количество товаров
    val productsCount: Int = 0
)

/**
 * События для экрана списка товаров
 */
sealed class ProductListEvent {
    // Навигация к экрану деталей товара
    data class NavigateToProductDetail(val productId: String) : ProductListEvent()

    // Показать снэкбар с сообщением
    data class ShowSnackbar(val message: String) : ProductListEvent()

    // Возврат к заданию с выбранным товаром
    data class ReturnToTaskWithProduct(val product: Product) : ProductListEvent()

    // Навигация назад
    data object NavigateBack : ProductListEvent()
}