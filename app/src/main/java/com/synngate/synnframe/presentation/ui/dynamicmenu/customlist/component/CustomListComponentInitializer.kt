package com.synngate.synnframe.presentation.ui.dynamicmenu.customlist.component

import com.synngate.synnframe.domain.entity.operation.CustomListItem
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.presentation.common.search.SearchResultType
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.GenericScreenComponentRegistry
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.ScreenElementsContainer
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.SearchComponent

fun <S : ScreenElementsContainer> GenericScreenComponentRegistry<S>.initializeCustomListComponents(
    itemsProvider: (S) -> List<CustomListItem>,
    isLoadingProvider: (S) -> Boolean,
    errorProvider: (S) -> String?,
    onItemClickProvider: (S) -> ((CustomListItem) -> Unit),
    searchValueProvider: (S) -> String,
    onSearchValueChangedProvider: (S) -> ((String) -> Unit),
    onSearchProvider: (S) -> (() -> Unit),
    searchResultTypeProvider: (S) -> SearchResultType?,
    lastSearchQueryProvider: (S) -> String
) {
    registerComponent(ScreenElementType.SHOW_LIST) { state ->
        CustomListComponent(
            state = state,
            items = itemsProvider(state),
            isLoading = isLoadingProvider(state),
            error = errorProvider(state),
            onItemClick = onItemClickProvider(state)
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