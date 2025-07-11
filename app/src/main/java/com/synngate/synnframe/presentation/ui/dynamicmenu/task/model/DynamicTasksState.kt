package com.synngate.synnframe.presentation.ui.dynamicmenu.task.model

import com.synngate.synnframe.domain.entity.operation.DynamicTask
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import com.synngate.synnframe.presentation.common.search.SearchResultType
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.ScreenElementsContainer

data class DynamicTasksState(
    val menuItemId: String = "",
    val menuItemName: String = "",
    val endpoint: String = "",
    override val screenSettings: ScreenSettings = ScreenSettings(),
    val tasks: List<DynamicTask> = emptyList(),
    val taskTypeId: String? = null,
    val searchValue: String = "",
    val isLoading: Boolean = false,
    val isCreatingTask: Boolean = false,
    val error: String? = null,
    val foundTask: DynamicTask? = null,
    val lastSearchQuery: String = "",
    val searchResultType: SearchResultType? = null,
    val isLocalSearch: Boolean = false,
    val savedSearchKey: String? = null, // Сохраненный ключ поиска
    val hasValidSavedSearchKey: Boolean = false, // Флаг валидности сохраненного ключа
    val showSavedKeyDialog: Boolean = false, // Показывать ли диалог ввода ключа
    val isValidatingKey: Boolean = false, // Идет ли валидация ключа
    val keyValidationError: String? = null, // Ошибка валидации ключа
    val savedKeyEndpoint: String? = null, // Endpoint для валидации ключа
    val showDeleteDialog: Boolean = false,
    val taskToDelete: DynamicTask? = null,
    val isDeleting: Boolean = false
) : ScreenElementsContainer {

    fun hasElement(element: ScreenElementType): Boolean {
        return screenSettings.screenElements.contains(element)
    }

    fun shouldShowSearchIndicator(): Boolean {
        return searchValue.isNotEmpty() ||
                lastSearchQuery.isNotEmpty() &&
                searchResultType != null
    }

    fun canCreateTask(): Boolean {
        return !taskTypeId.isNullOrBlank() && !isCreatingTask && !isLoading
    }

    fun isSearchSaveable(): Boolean {
        return screenSettings.screenElements.contains(ScreenElementType.SEARCH_SAVEABLE)
    }

    fun hasDeletableTasks(): Boolean {
        return tasks.any { it.isDeletable() }
    }
}