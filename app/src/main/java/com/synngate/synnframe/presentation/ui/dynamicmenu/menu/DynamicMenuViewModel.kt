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
        Timber.d("Загрузка меню с ID: $menuItemId")
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }
            try {
                val result = dynamicMenuUseCases.getDynamicMenu(menuItemId)
                if (result.isSuccess()) {
                    val menuItems = result.getOrNull() ?: emptyList()
                    Timber.d("Загружено ${menuItems.size} элементов меню, текущий ID: $menuItemId")
                    updateState {
                        it.copy(
                            menuItems = menuItems,
                            currentMenuItemId = menuItemId,
                            isLoading = false,
                            error = if (menuItems.isEmpty()) "Нет доступных элементов меню" else null
                        )
                    }
                } else {
                    Timber.e("Ошибка загрузки меню: ${(result as? com.synngate.synnframe.data.remote.api.ApiResult.Error)?.message}")
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
        Timber.d("Клик на элемент меню: ${menuItem.id}, тип: ${menuItem.type}")

        when (menuItem.type) {
            DynamicMenuItemType.SUBMENU -> {
                // Сохраняем текущий ID в историю перед переходом
                val currentState = uiState.value
                val updatedHistory = currentState.navigationHistory + currentState.currentMenuItemId

                Timber.d("Переход в подменю: ${menuItem.id}, история: $updatedHistory")

                // Обновляем состояние с новой историей
                updateState { it.copy(navigationHistory = updatedHistory) }

                // Загружаем новое подменю
                loadDynamicMenu(menuItem.id)
            }
            DynamicMenuItemType.TASKS -> {
                if (menuItem.endpoint == null) {
                    sendEvent(DynamicMenuEvent.ShowSnackbar("Ошибка: отсутствует endpoint для задания"))
                    return
                }
                Timber.d("Переход к списку заданий: ${menuItem.id}")
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
                    sendEvent(DynamicMenuEvent.ShowSnackbar("Ошибка: отсутствует endpoint для товаров"))
                    return
                }
                Timber.d("Переход к списку товаров: ${menuItem.id}")
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
        Timber.d("Вызван onBackPressed")

        val currentState = uiState.value

        if (currentState.navigationHistory.isEmpty()) {
            // Если история пуста, выходим из динамического меню
            Timber.d("История навигации пуста, выход из динамического меню")
            sendEvent(DynamicMenuEvent.NavigateBack)
            return
        }

        // Получаем последний ID из истории
        val previousHistory = currentState.navigationHistory.dropLast(1)
        val previousMenuItemId = currentState.navigationHistory.lastOrNull()

        Timber.d("Возврат к предыдущему меню: $previousMenuItemId, обновленная история: $previousHistory")

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