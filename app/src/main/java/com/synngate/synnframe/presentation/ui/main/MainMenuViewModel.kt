package com.synngate.synnframe.presentation.ui.main

import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.main.model.MainMenuEvent
import com.synngate.synnframe.presentation.ui.main.model.MainMenuState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import timber.log.Timber

/**
 * ViewModel для экрана главного меню
 */
class MainMenuViewModel(
    private val userUseCases: UserUseCases,
    private val productUseCases: ProductUseCases,
    private val synchronizationController: SynchronizationController,
) : BaseViewModel<MainMenuState, MainMenuEvent>(MainMenuState()) {


    init {
        loadData()
    }

    private fun loadData() {
        launchIO {
            updateState { it.copy(isLoading = true) }

            try {
                // Комбинируем потоки данных для эффективной загрузки
                combine(
                    userUseCases.getCurrentUser(),
                    productUseCases.getProductsCount()
                ) { user, productsCount ->
                    Pair(user, productsCount)
                }.catch { e ->
                    Timber.e(e, "Error loading main menu data")
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Error loading data"
                        )
                    }
                }.collectLatest { (user, productsCount) ->
                    updateState {
                        it.copy(
                            currentUser = user,
                            totalProductsCount = productsCount,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during main menu data loading")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error loading data"
                    )
                }
            }
        }
    }

    fun onProductsClick() {
        sendEvent(MainMenuEvent.NavigateToProducts)
    }

    fun onLogsClick() {
        sendEvent(MainMenuEvent.NavigateToLogs)
    }

    fun onSettingsClick() {
        sendEvent(MainMenuEvent.NavigateToSettings)
    }

    fun onChangeUserClick() {
        launchIO {
            try {
                // Выход текущего пользователя
                val result = userUseCases.logoutUser()

                if (result.isSuccess) {
                    sendEvent(MainMenuEvent.NavigateToLogin)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Error during user logout"
                    updateState { it.copy(error = error) }
                    sendEvent(MainMenuEvent.ShowSnackbar(error))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during user logout")
                val errorMessage = e.message ?: "Error during user logout"
                updateState { it.copy(error = errorMessage) }
                sendEvent(MainMenuEvent.ShowSnackbar(errorMessage))
            }
        }
    }

    fun onExitClick() {
        updateState { it.copy(showExitConfirmation = true) }
        sendEvent(MainMenuEvent.ShowExitConfirmation)
    }

    fun hideExitConfirmation() {
        updateState { it.copy(showExitConfirmation = false) }
    }

    fun exitApp() {
        sendEvent(MainMenuEvent.ExitApp)
    }

    fun refreshData() {
        loadData()
    }

    fun syncData() {
        launchIO {
            updateState { it.copy(isSyncing = true) }

            try {
                // Запускаем синхронизацию через контроллер
                val syncResult = synchronizationController.startManualSync()

                if (syncResult.isSuccess) {
                    val result = syncResult.getOrNull()

                    // Информация о синхронизации теперь хранится в контроллере
                    // и будет автоматически доступна всем экранам

                    // Обновляем состояние только для UI этого экрана
                    updateState { state ->
                        state.copy(
                            isSyncing = false,
                            // Больше не храним lastSyncTime в состоянии ViewModel
                        )
                    }

                    val message =
                        "Synchronization completed. Products updated: ${result?.productsDownloadedCount ?: 0}"
                    sendEvent(MainMenuEvent.ShowSnackbar(message))
                } else {
                    updateState { it.copy(isSyncing = false) }
                    val error = syncResult.exceptionOrNull()?.message ?: "Synchronization error"
                    sendEvent(MainMenuEvent.ShowSnackbar(error))
                }

                // Перезагружаем данные после синхронизации
                loadData()
            } catch (e: Exception) {
                Timber.e(e, "Error during data synchronization")
                updateState { it.copy(isSyncing = false) }
                sendEvent(MainMenuEvent.ShowSnackbar("Synchronization error: ${e.message}"))
            }
        }
    }

    fun onDynamicMenuClick() {
        sendEvent(MainMenuEvent.NavigateToDynamicMenu)
    }
}