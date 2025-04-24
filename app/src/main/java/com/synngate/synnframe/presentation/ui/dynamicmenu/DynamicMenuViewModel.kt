package com.synngate.synnframe.presentation.ui.dynamicmenu

import com.synngate.synnframe.domain.entity.DynamicMenuItemType
import com.synngate.synnframe.domain.usecase.dynamicmenu.DynamicMenuUseCases
import com.synngate.synnframe.presentation.ui.dynamicmenu.model.DynamicMenuEvent
import com.synngate.synnframe.presentation.ui.dynamicmenu.model.DynamicMenuState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import timber.log.Timber

class DynamicMenuViewModel(
    private val dynamicMenuUseCases: DynamicMenuUseCases,
) : BaseViewModel<DynamicMenuState, DynamicMenuEvent>(DynamicMenuState()) {

    init {
        loadDynamicMenu()
    }

    fun loadDynamicMenu() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = dynamicMenuUseCases.getDynamicMenu()
                if (result.isSuccess()) {
                    val menuItems = result.getOrNull() ?: emptyList()
                    updateState {
                        it.copy(
                            menuItems = menuItems,
                            isLoading = false,
                            error = if (menuItems.isEmpty()) "Нет доступных операций" else null
                        )
                    }
                } else {
                    val errorMessage = "Ошибка загрузки операций: ${result as? com.synngate.synnframe.data.remote.api.ApiResult.Error}"
                    Timber.e(errorMessage)
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while loading operation menu")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка загрузки операций: ${e.message}"
                    )
                }
            }
        }
    }

    fun onMenuItemClick(menuItemId: String, menuItemName: String, menuItemType: DynamicMenuItemType) {
        sendEvent(DynamicMenuEvent.NavigateToDynamicTasks(menuItemId, menuItemName, menuItemType))
    }

    fun onRefresh() {
        loadDynamicMenu()
    }
}