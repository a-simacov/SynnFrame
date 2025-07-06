package com.synngate.synnframe.presentation.ui.dynamicmenu.customlist

import com.synngate.synnframe.data.remote.api.ApiResult
import com.synngate.synnframe.domain.entity.operation.CustomListItem
import com.synngate.synnframe.domain.entity.operation.ScreenSettings
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.presentation.common.search.SearchResultType
import com.synngate.synnframe.presentation.ui.dynamicmenu.customlist.model.DynamicCustomListEvent
import com.synngate.synnframe.presentation.ui.dynamicmenu.customlist.model.DynamicCustomListState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import timber.log.Timber

class DynamicCustomListViewModel(
    menuItemId: String,
    menuItemName: String,
    val endpoint: String,
    screenSettings: ScreenSettings,
    private val dynamicMenuUseCases: DynamicMenuUseCases
) : BaseViewModel<DynamicCustomListState, DynamicCustomListEvent>(
    DynamicCustomListState(
        menuItemId = menuItemId,
        menuItemName = menuItemName,
        endpoint = endpoint,
        screenSettings = screenSettings
    )
) {

    init {
        // Не загружаем данные автоматически при открытии экрана
    }

    fun loadCustomList() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null, searchResultType = null, lastSearchQuery = "") }

            try {
                val result = dynamicMenuUseCases.getCustomList(endpoint)

                if (result.isSuccess()) {
                    val responseDto = result.getOrNull()
                    if (responseDto != null) {
                        val items = responseDto.list

                        updateState {
                            it.copy(
                                items = items,
                                isLoading = false,
                            )
                        }
                        Timber.d("Loaded ${items.size} custom list items")
                    } else {
                        updateState {
                            it.copy(
                                isLoading = false,
                                error = "Empty response received from server"
                            )
                        }
                    }
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Error loading items: ${(result as? ApiResult.Error)?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading custom list")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Error loading items: ${e.message}"
                    )
                }
            }
        }
    }

    fun onSearchKeyClick(key: String) {
        updateState { it.copy(searchKey = key) }
        onSearch()
    }

    fun onSearchValueChanged(newSearch: String) {
        updateState { it.copy(searchKey = newSearch) }
    }

    fun onSearchChange(newSearch: String) {
        updateState { it.copy(searchKey = newSearch) }
    }

    fun onSearchDone() {
        onSearch()
    }

    fun onSearch() {
        val key = uiState.value.searchKey.trim()
        
        // Если поле поиска пустое - очищаем список без запроса к серверу
        if (key.isBlank()) {
            updateState { 
                it.copy(
                    items = emptyList(),
                    searchResultType = null,
                    lastSearchQuery = "",
                    error = null
                ) 
            }
            return
        }

        updateState { 
            it.copy(
                searchKey = key, 
                isLoading = true, 
                error = null,
                lastSearchQuery = key
            ) 
        }

        launchIO {
            try {
                Timber.d("Searching custom list with value: $key")
                val result = dynamicMenuUseCases.searchCustomList(endpoint, key)

                if (result.isSuccess()) {
                    val responseDto = result.getOrNull()
                    if (responseDto != null) {
                        val items = responseDto.list

                        updateState {
                            it.copy(
                                items = items,
                                isLoading = false,
                                searchResultType = SearchResultType.REMOTE
                            )
                        }
                        Timber.d("Search found ${items.size} items")
                    } else {
                        updateState {
                            it.copy(
                                isLoading = false,
                                error = "No results found",
                                searchResultType = null
                            )
                        }
                    }
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Search error: ${(result as? ApiResult.Error)?.message}",
                            searchResultType = null
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error searching custom list")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Search error: ${e.message}",
                        searchResultType = null
                    )
                }
            }
        }
    }

    fun onItemClick(item: CustomListItem) {
        Timber.d("Clicked on custom list item: ${item.id}")
        sendEvent(DynamicCustomListEvent.ShowItemDetails(item))
    }

    fun onRefresh() {
        val searchKey = uiState.value.searchKey.trim()
        if (searchKey.isNotBlank()) {
            onSearch()
        } else {
            // Если нет поискового запроса, просто очищаем список
            updateState { 
                it.copy(
                    items = emptyList(),
                    searchResultType = null,
                    lastSearchQuery = "",
                    error = null
                ) 
            }
        }
    }

    fun clearError() {
        updateState { it.copy(error = null) }
    }
}