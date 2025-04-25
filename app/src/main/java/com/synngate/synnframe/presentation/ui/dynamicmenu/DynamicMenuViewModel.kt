package com.synngate.synnframe.presentation.ui.dynamicmenu

import com.synngate.synnframe.domain.entity.DynamicMenuItemType
import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.presentation.ui.dynamicmenu.model.DynamicMenuEvent
import com.synngate.synnframe.presentation.ui.dynamicmenu.model.DynamicMenuState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import timber.log.Timber

class DynamicMenuViewModel(
    private val dynamicMenuUseCases: DynamicMenuUseCases,
) : BaseViewModel<DynamicMenuState, DynamicMenuEvent>(DynamicMenuState()) {

    private var currentMenuItemId: String? = null

    init {
        loadDynamicMenu(null)
    }

    fun loadDynamicMenu(menuItemId: String?) {
        currentMenuItemId = menuItemId

        launchIO {
            updateState { it.copy(isLoading = true, error = null) }
            try {
                val result = dynamicMenuUseCases.getDynamicMenu(menuItemId)
                if (result.isSuccess()) {
                    val menuItems = result.getOrNull() ?: emptyList()
                    updateState {
                        it.copy(
                            menuItems = menuItems,
                            currentMenuItemId = menuItemId,
                            isLoading = false,
                            error = if (menuItems.isEmpty()) "Нет доступных элементов меню" else null
                        )
                    }
                } else {
                    updateState { it.copy(
                        isLoading = false,
                        error = "Ошибка загрузки меню: ${(result as? com.synngate.synnframe.data.remote.api.ApiResult.Error)?.message}"
                    )}
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading dynamic menu")
                updateState { it.copy(
                    isLoading = false,
                    error = "Ошибка: ${e.message}"
                )}
            }
        }
    }

    fun onMenuItemClick(menuItem: DynamicMenuItem) {
        when (menuItem.type) {
            DynamicMenuItemType.SUBMENU -> {
                loadDynamicMenu(menuItem.id)
            }
            DynamicMenuItemType.TASKS -> {
                if (menuItem.endpoint == null) {
                    sendEvent(DynamicMenuEvent.ShowSnackbar("Ошибка: отсутствует endpoint для задания"))
                    return
                }
                sendEvent(DynamicMenuEvent.NavigateToDynamicTasks(
                    menuItemId = menuItem.id,
                    menuItemName = menuItem.name,
                    endpoint = menuItem.endpoint,
                    screenSettings = menuItem.screenSettings
                ))
            }
            DynamicMenuItemType.PRODUCTS -> {
                if (menuItem.endpoint == null) {
                    sendEvent(DynamicMenuEvent.ShowSnackbar("Ошибка: отсутствует endpoint для товаров"))
                    return
                }
                sendEvent(DynamicMenuEvent.ShowSnackbar("Работа с товарами будет реализована позже"))
            }
        }
    }

    fun onBackPressed() {
        if (currentMenuItemId != null) {
            val parentId = uiState.value.menuItems.firstOrNull()?.parentId
            loadDynamicMenu(parentId)
        } else {
            sendEvent(DynamicMenuEvent.NavigateBack)
        }
    }

    fun onRefresh() {
        loadDynamicMenu(currentMenuItemId)
    }
}