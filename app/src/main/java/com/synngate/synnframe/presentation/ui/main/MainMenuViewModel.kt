package com.synngate.synnframe.presentation.ui.main

import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.main.model.MainMenuEvent
import com.synngate.synnframe.presentation.ui.main.model.MainMenuState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import timber.log.Timber
import java.time.format.DateTimeFormatter

/**
 * ViewModel для экрана главного меню
 */
class MainMenuViewModel(
    private val userUseCases: UserUseCases,
    private val taskUseCases: TaskUseCases,
    private val productUseCases: ProductUseCases,
    private val synchronizationController: SynchronizationController,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<MainMenuState, MainMenuEvent>(MainMenuState()) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    init {
        loadData()
    }

    /**
     * Загрузка данных для главного экрана
     */
    private fun loadData() {
        launchIO {
            updateState { it.copy(isLoading = true) }

            try {
                // Комбинируем потоки данных для эффективной загрузки
                combine(
                    userUseCases.getCurrentUser(),
                    taskUseCases.getTasksCountForCurrentUser(),
                    productUseCases.getProductsCount()
                ) { user, tasksCount, productsCount ->
                    Triple(user, tasksCount, productsCount)
                }.catch { e ->
                    Timber.e(e, "Error loading main menu data")
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Ошибка загрузки данных"
                        )
                    }
                }.collectLatest { (user, tasksCount, productsCount) ->
                    updateState {
                        it.copy(
                            currentUser = user,
                            assignedTasksCount = tasksCount,
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
                        error = e.message ?: "Ошибка загрузки данных"
                    )
                }
            }
        }
    }

    /**
     * Обработка нажатия на кнопку "Задания"
     */
    fun onTasksClick() {
        sendEvent(MainMenuEvent.NavigateToTasks)
    }

    /**
     * Обработка нажатия на кнопку "Товары"
     */
    fun onProductsClick() {
        sendEvent(MainMenuEvent.NavigateToProducts)
    }

    /**
     * Обработка нажатия на кнопку "Логи"
     */
    fun onLogsClick() {
        sendEvent(MainMenuEvent.NavigateToLogs)
    }

    /**
     * Обработка нажатия на кнопку "Настройки"
     */
    fun onSettingsClick() {
        sendEvent(MainMenuEvent.NavigateToSettings)
    }

    /**
     * Обработка нажатия на кнопку "Смена пользователя"
     */
    fun onChangeUserClick() {
        launchIO {
            try {
                // Выход текущего пользователя
                val result = userUseCases.logoutUser()

                if (result.isSuccess) {
                    sendEvent(MainMenuEvent.NavigateToLogin)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Ошибка выхода пользователя"
                    updateState { it.copy(error = error) }
                    sendEvent(MainMenuEvent.ShowSnackbar(error))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during user logout")
                val errorMessage = e.message ?: "Ошибка выхода пользователя"
                updateState { it.copy(error = errorMessage) }
                sendEvent(MainMenuEvent.ShowSnackbar(errorMessage))
            }
        }
    }

    /**
     * Обработка нажатия на кнопку "Закрыть"
     */
    fun onExitClick() {
        updateState { it.copy(showExitConfirmation = true) }
        sendEvent(MainMenuEvent.ShowExitConfirmation)
    }

    /**
     * Скрыть диалог подтверждения выхода
     */
    fun hideExitConfirmation() {
        updateState { it.copy(showExitConfirmation = false) }
    }

    /**
     * Выход из приложения
     */
    fun exitApp() {
        sendEvent(MainMenuEvent.ExitApp)
    }

    /**
     * Обновление данных
     */
    fun refreshData() {
        loadData()
    }

    /**
     * Синхронизация данных с сервером
     */
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

                    val message = "Синхронизация выполнена. Обновлено заданий: ${result?.tasksDownloadedCount ?: 0}, " +
                            "товаров: ${result?.productsDownloadedCount ?: 0}"
                    sendEvent(MainMenuEvent.ShowSnackbar(message))
                } else {
                    updateState { it.copy(isSyncing = false) }
                    val error = syncResult.exceptionOrNull()?.message ?: "Ошибка синхронизации"
                    sendEvent(MainMenuEvent.ShowSnackbar(error))
                }

                // Перезагружаем данные после синхронизации
                loadData()
            } catch (e: Exception) {
                Timber.e(e, "Error during data synchronization")
                updateState { it.copy(isSyncing = false) }
                sendEvent(MainMenuEvent.ShowSnackbar("Ошибка синхронизации: ${e.message}"))
            }
        }
    }
}