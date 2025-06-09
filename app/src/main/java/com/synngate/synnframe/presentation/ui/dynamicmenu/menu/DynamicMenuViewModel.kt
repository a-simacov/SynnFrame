package com.synngate.synnframe.presentation.ui.dynamicmenu.menu

import com.synngate.synnframe.domain.entity.DynamicMenuItemType
import com.synngate.synnframe.domain.entity.operation.DynamicMenuItem
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.presentation.ui.dynamicmenu.menu.model.DynamicMenuEvent
import com.synngate.synnframe.presentation.ui.dynamicmenu.menu.model.DynamicMenuState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import timber.log.Timber

class DynamicMenuViewModel(
    private val dynamicMenuUseCases: DynamicMenuUseCases,
) : BaseViewModel<DynamicMenuState, DynamicMenuEvent>(DynamicMenuState()) {

    init {
        loadDynamicMenu(null)
    }

    fun loadDynamicMenu(menuItemId: String?) {
        Timber.d("Loading menu with ID: $menuItemId")
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }
            try {
                val result = dynamicMenuUseCases.getDynamicMenu(menuItemId)
                if (result.isSuccess()) {
                    val menuItems = result.getOrNull() ?: emptyList()
                    Timber.d("Loaded ${menuItems.size} menu items, current ID: $menuItemId")
                    updateState {
                        it.copy(
                            menuItems = menuItems,
                            currentMenuItemId = menuItemId,
                            isLoading = false,
                            error = if (menuItems.isEmpty()) "No available menu items" else null
                        )
                    }
                } else {
                    Timber.e("Error loading menu: ${(result as? com.synngate.synnframe.data.remote.api.ApiResult.Error)?.message}")
                    updateState { it.copy(
                        isLoading = false,
                        error = "Error loading menu: ${(result as? com.synngate.synnframe.data.remote.api.ApiResult.Error)?.message}"
                    )}
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading dynamic menu")
                updateState { it.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )}
            }
        }
    }

    fun onMenuItemClick(menuItem: DynamicMenuItem) {
        Timber.d("Click on menu item: ${menuItem.id}, type: ${menuItem.type}")

        when (menuItem.type) {
            DynamicMenuItemType.SUBMENU -> {
                // Сохраняем текущий ID в историю перед переходом
                val currentState = uiState.value
                val updatedHistory = currentState.navigationHistory + currentState.currentMenuItemId

                Timber.d("Navigating to submenu: ${menuItem.id}, history: $updatedHistory")

                // Обновляем состояние с новой историей
                updateState { it.copy(navigationHistory = updatedHistory) }

                // Загружаем новое подменю
                loadDynamicMenu(menuItem.id)
            }
            DynamicMenuItemType.TASKS -> {
                if (menuItem.endpoint == null) {
                    sendEvent(DynamicMenuEvent.ShowSnackbar("Error: missing endpoint for task"))
                    return
                }
                Timber.d("Navigating to task list: ${menuItem.id}")
                sendEvent(
                    DynamicMenuEvent.NavigateToDynamicTasks(
                        menuItemId = menuItem.id,
                        menuItemName = menuItem.name,
                        endpoint = menuItem.endpoint,
                        screenSettings = menuItem.screenSettings
                    ))
            }
            DynamicMenuItemType.PRODUCTS -> {
                if (menuItem.endpoint == null) {
                    sendEvent(DynamicMenuEvent.ShowSnackbar("Error: missing endpoint for products"))
                    return
                }
                Timber.d("Navigating to product list: ${menuItem.id}")
                sendEvent(
                    DynamicMenuEvent.NavigateToDynamicProducts(
                        menuItemId = menuItem.id,
                        menuItemName = menuItem.name,
                        endpoint = menuItem.endpoint,
                        screenSettings = menuItem.screenSettings
                    ))
            }
        }
    }

    fun onBackPressed() {
        Timber.d("onBackPressed called")

        val currentState = uiState.value

        if (currentState.navigationHistory.isEmpty()) {
            // Если история пуста, выходим из динамического меню
            Timber.d("Navigation history is empty, exiting dynamic menu")
            sendEvent(DynamicMenuEvent.NavigateBack)
            return
        }

        // Получаем последний ID из истории
        val previousHistory = currentState.navigationHistory.dropLast(1)
        val previousMenuItemId = currentState.navigationHistory.lastOrNull()

        Timber.d("Returning to previous menu: $previousMenuItemId, updated history: $previousHistory")

        // Обновляем историю
        updateState { it.copy(navigationHistory = previousHistory) }

        // Загружаем предыдущее меню
        loadDynamicMenu(previousMenuItemId)
    }

    fun onRefresh() {
        loadDynamicMenu(uiState.value.currentMenuItemId)
    }

    fun clearError() {
        updateState { it.copy(error = null) }
    }
}