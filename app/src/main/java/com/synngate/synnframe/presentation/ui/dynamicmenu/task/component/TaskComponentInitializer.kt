package com.synngate.synnframe.presentation.ui.dynamicmenu.task.component

import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.GenericScreenComponentRegistry
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.ScreenElementsContainer
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.SearchComponent

fun <S : ScreenElementsContainer> GenericScreenComponentRegistry<S>.initializeTaskComponents(
    tasksProvider: (S) -> List<DynamicTask>,
    isLoadingProvider: (S) -> Boolean,
    errorProvider: (S) -> String?,
    onTaskClickProvider: (S) -> ((DynamicTask) -> Unit),
    searchValueProvider: (S) -> String,
    onSearchValueChangedProvider: (S) -> ((String) -> Unit),
    onSearchProvider: (S) -> (() -> Unit)
) {
    registerComponent(ScreenElementType.SHOW_LIST) { state ->
        TaskListComponent(
            state = state,
            tasks = tasksProvider(state),
            isLoading = isLoadingProvider(state),
            error = errorProvider(state),
            onTaskClick = onTaskClickProvider(state)
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
