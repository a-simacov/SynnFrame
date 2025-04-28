package com.synngate.synnframe.presentation.ui.dynamicmenu.product.component

import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.GenericScreenComponentRegistry
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.ScreenElementsContainer
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.SearchComponent

fun <S : ScreenElementsContainer> GenericScreenComponentRegistry<S>.initializeProductComponents(
    productsProvider: (S) -> List<DynamicProduct>,
    isLoadingProvider: (S) -> Boolean,
    errorProvider: (S) -> String?,
    onProductClickProvider: (S) -> ((DynamicProduct) -> Unit),
    searchValueProvider: (S) -> String,
    onSearchValueChangedProvider: (S) -> ((String) -> Unit),
    onSearchProvider: (S) -> (() -> Unit)
) {
    registerComponent(ScreenElementType.SHOW_LIST) { state ->
        ProductListComponent(
            state = state,
            products = productsProvider(state),
            isLoading = isLoadingProvider(state),
            error = errorProvider(state),
            onProductClick = onProductClickProvider(state)
        )
    }

    registerComponent(ScreenElementType.SEARCH) { state ->
        SearchComponent(
            searchValue = searchValueProvider(state),
            onSearchValueChanged = onSearchValueChangedProvider(state),
            onSearch = onSearchProvider(state)
        )
    }
}