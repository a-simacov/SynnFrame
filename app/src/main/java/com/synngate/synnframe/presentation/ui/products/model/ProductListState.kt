package com.synngate.synnframe.presentation.ui.products.model

import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product

data class ProductListState(

    val products: List<Product> = emptyList(),

    val isLoading: Boolean = false,

    val error: String? = null,

    val searchQuery: String = "",

    val isSelectionMode: Boolean = false,

    val isSyncing: Boolean = false,

    val lastSyncTime: String? = null,

    val productsCount: Int = 0,

    val filterByAccountingModel: AccountingModel? = null,

    val sortOrder: SortOrder = SortOrder.NAME_ASC,

    val showFilterPanel: Boolean = false,

    val showBatchScannerDialog: Boolean = false,

    val showScannerDialog: Boolean = false,

    val selectedProduct: Product? = null
)

sealed class ProductListEvent {

    data class NavigateToProductDetail(val productId: String) : ProductListEvent()

    data class ShowSnackbar(val message: String) : ProductListEvent()

    data class ReturnSelectedProductId(val productId: String) : ProductListEvent()

    data object NavigateBack : ProductListEvent()
}