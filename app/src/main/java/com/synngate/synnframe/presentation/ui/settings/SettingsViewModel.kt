package com.synngate.synnframe.presentation.ui.settings

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.domain.service.UpdateInstaller
import com.synngate.synnframe.domain.service.UpdateInstallerImpl
import com.synngate.synnframe.domain.service.WebServerManager
import com.synngate.synnframe.domain.usecase.server.ServerUseCases
import com.synngate.synnframe.domain.usecase.settings.SettingsUseCases
import com.synngate.synnframe.presentation.theme.ThemeMode
import com.synngate.synnframe.presentation.ui.settings.model.SettingsEvent
import com.synngate.synnframe.presentation.ui.settings.model.SettingsState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

class SettingsViewModel(
    private val settingsUseCases: SettingsUseCases,
    private val serverUseCases: ServerUseCases,
    private val loggingService: LoggingService,
    private val webServerManager: WebServerManager,
    private val synchronizationController: SynchronizationController,
    private val updateInstaller: UpdateInstaller,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<SettingsState, SettingsEvent>(SettingsState()) {

    init {
        loadSettings()
        observeWebServerState()
    }

    /**
     * Загрузка всех настроек приложения
     */
    private fun loadSettings() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            //todo зедсь можно было использовать combine
            try {
                // Загружаем настройки последовательно
                val showServers = settingsUseCases.showServersOnStartup.first()
                val periodicUpload = settingsUseCases.periodicUploadEnabled.first()
                val uploadInterval = settingsUseCases.uploadIntervalSeconds.first()
                val themeMode = settingsUseCases.themeMode.first()
                val languageCode = settingsUseCases.languageCode.first()
                val buttonHeight = settingsUseCases.navigationButtonHeight.first()
                val activeServer = serverUseCases.getActiveServer().first()

                // Обновляем состояние с загруженными данными
                updateState {
                    it.copy(
                        showServersOnStartup = showServers,
                        periodicUploadEnabled = periodicUpload,
                        uploadIntervalSeconds = uploadInterval,
                        themeMode = themeMode,
                        languageCode = languageCode,
                        navigationButtonHeight = buttonHeight,
                        activeServer = activeServer,
                        isLoading = false
                    )
                }

                // Запускаем наблюдение за изменениями
                observeSettingsChanges()
            } catch (e: Exception) {
                Timber.e(e, "Error loading settings")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка загрузки настроек: ${e.message}"
                    )
                }
                sendEvent(SettingsEvent.ShowSnackbar("Ошибка загрузки настроек"))
            }
        }
    }

    /**
     * Наблюдение за состоянием веб-сервера
     */
    private fun observeWebServerState() {
        viewModelScope.launch {
            webServerManager.isRunning.collect { isRunning ->
                updateState { it.copy(isWebServerRunning = isRunning) }
            }
        }
    }

    /**
     * Наблюдение за изменениями настроек
     */
    private fun observeSettingsChanges() {
        viewModelScope.launch {
            settingsUseCases.showServersOnStartup.collect { showServers ->
                updateState { it.copy(showServersOnStartup = showServers) }
            }
        }

        viewModelScope.launch {
            settingsUseCases.periodicUploadEnabled.collect { enabled ->
                updateState { it.copy(periodicUploadEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            settingsUseCases.uploadIntervalSeconds.collect { interval ->
                updateState { it.copy(uploadIntervalSeconds = interval) }
            }
        }

        viewModelScope.launch {
            settingsUseCases.themeMode.collect { theme ->
                updateState { it.copy(themeMode = theme) }
            }
        }

        viewModelScope.launch {
            settingsUseCases.languageCode.collect { language ->
                updateState { it.copy(languageCode = language) }
            }
        }

        viewModelScope.launch {
            settingsUseCases.navigationButtonHeight.collect { height ->
                updateState { it.copy(navigationButtonHeight = height) }
            }
        }

        viewModelScope.launch {
            serverUseCases.getActiveServer().collect { server ->
                updateState { it.copy(activeServer = server) }
            }
        }
    }

    /**
     * Обновление настройки отображения серверов при запуске
     */
    fun updateShowServersOnStartup(show: Boolean) {
        if (uiState.value.showServersOnStartup == show) return

        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = settingsUseCases.setShowServersOnStartup(show)

                if (result.isSuccess) {
                    updateState {
                        it.copy(
                            showServersOnStartup = show,
                            isLoading = false,
                            error = null
                        )
                    }
                    sendEvent(SettingsEvent.SettingsUpdated)
                } else {
                    val exception = result.exceptionOrNull()
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка обновления настройки: ${exception?.message}"
                        )
                    }
                    sendEvent(SettingsEvent.ShowSnackbar("Ошибка обновления настройки"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating showServersOnStartup setting")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка обновления настройки: ${e.message}"
                    )
                }
                sendEvent(SettingsEvent.ShowSnackbar("Ошибка обновления настройки"))
            }
        }
    }

    /**
     * Обновление настроек периодической выгрузки
     */
    fun updatePeriodicUpload(enabled: Boolean, intervalSeconds: Int? = null) {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val interval = intervalSeconds ?: uiState.value.uploadIntervalSeconds

                val result = settingsUseCases.setPeriodicUpload(enabled, interval)

                if (result.isSuccess) {
                    updateState {
                        it.copy(
                            periodicUploadEnabled = enabled,
                            uploadIntervalSeconds = interval,
                            isLoading = false,
                            error = null
                        )
                    }
                    sendEvent(SettingsEvent.SettingsUpdated)
                } else {
                    val exception = result.exceptionOrNull()
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка обновления настроек выгрузки: ${exception?.message}"
                        )
                    }
                    sendEvent(SettingsEvent.ShowSnackbar("Ошибка обновления настроек выгрузки"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating periodic upload settings")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка обновления настроек выгрузки: ${e.message}"
                    )
                }
                sendEvent(SettingsEvent.ShowSnackbar("Ошибка обновления настроек выгрузки"))
            }
        }
    }

    /**
     * Обновление интервала выгрузки (в секундах)
     */
    fun updateUploadInterval(intervalSeconds: Int) {
        val validInterval =
            max(30, min(3600, intervalSeconds)) // Ограничиваем от 30 секунд до 1 часа
        updatePeriodicUpload(uiState.value.periodicUploadEnabled, validInterval)
    }

    /**
     * Обновление режима темы
     */
    fun updateThemeMode(themeMode: ThemeMode) {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = settingsUseCases.setThemeMode(themeMode)

                if (result.isSuccess) {
                    updateState {
                        it.copy(
                            themeMode = themeMode,
                            isLoading = false,
                            error = null
                        )
                    }
                    sendEvent(SettingsEvent.SettingsUpdated)
                } else {
                    val exception = result.exceptionOrNull()
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка обновления темы: ${exception?.message}"
                        )
                    }
                    sendEvent(SettingsEvent.ShowSnackbar("Ошибка обновления темы"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating theme mode")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка обновления темы: ${e.message}"
                    )
                }
                sendEvent(SettingsEvent.ShowSnackbar("Ошибка обновления темы"))
            }
        }
    }

    /**
     * Обновление языка интерфейса
     */
    fun updateLanguageCode(languageCode: String) {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = settingsUseCases.setLanguageCode(languageCode)

                if (result.isSuccess) {
                    updateState {
                        it.copy(
                            languageCode = languageCode,
                            isLoading = false,
                            error = null
                        )
                    }
                    sendEvent(SettingsEvent.SettingsUpdated)
                } else {
                    val exception = result.exceptionOrNull()
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка обновления языка: ${exception?.message}"
                        )
                    }
                    sendEvent(SettingsEvent.ShowSnackbar("Ошибка обновления языка"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating language code")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка обновления языка: ${e.message}"
                    )
                }
                sendEvent(SettingsEvent.ShowSnackbar("Ошибка обновления языка"))
            }
        }
    }

    /**
     * Обновление высоты кнопки навигации
     */
    fun updateNavigationButtonHeight(height: Float) {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val validHeight = max(48f, min(96f, height)) // Ограничиваем от 48dp до 96dp
                val result = settingsUseCases.setNavigationButtonHeight(validHeight)

                if (result.isSuccess) {
                    updateState {
                        it.copy(
                            navigationButtonHeight = validHeight,
                            isLoading = false,
                            error = null
                        )
                    }
                    sendEvent(SettingsEvent.SettingsUpdated)
                } else {
                    val exception = result.exceptionOrNull()
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка обновления высоты кнопки: ${exception?.message}"
                        )
                    }
                    sendEvent(SettingsEvent.ShowSnackbar("Ошибка обновления высоты кнопки"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating navigation button height")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка обновления высоты кнопки: ${e.message}"
                    )
                }
                sendEvent(SettingsEvent.ShowSnackbar("Ошибка обновления высоты кнопки"))
            }
        }
    }

    /**
     * Проверка наличия обновлений
     */
    fun checkForUpdates() {
        launchIO {
            updateState {
                it.copy(
                    isCheckingForUpdates = true,
                    error = null,
                    lastVersion = null,
                    releaseDate = null,
                    showUpdateConfirmDialog = false
                )
            }

            try {
                val result = settingsUseCases.checkForUpdates()

                if (result.isSuccess) {
                    val (version, date) = result.getOrNull()!!

                    updateState {
                        it.copy(
                            isCheckingForUpdates = false,
                            lastVersion = version,
                            releaseDate = date,
                            showUpdateConfirmDialog = version != null,
                            error = null
                        )
                    }

                    if (version == null) {
                        sendEvent(SettingsEvent.ShowSnackbar("Обновлений не найдено"))
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    updateState {
                        it.copy(
                            isCheckingForUpdates = false,
                            error = "Ошибка проверки обновлений: ${exception?.message}"
                        )
                    }
                    sendEvent(SettingsEvent.ShowSnackbar("Ошибка проверки обновлений"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking for updates")
                updateState {
                    it.copy(
                        isCheckingForUpdates = false,
                        error = "Ошибка проверки обновлений: ${e.message}"
                    )
                }
                sendEvent(SettingsEvent.ShowSnackbar("Ошибка проверки обновлений"))
            }
        }
    }

    /**
     * Показать диалог подтверждения обновления
     */
    fun showUpdateConfirmDialog() {
        updateState { it.copy(showUpdateConfirmDialog = true) }
    }

    /**
     * Скрыть диалог подтверждения обновления
     */
    fun hideUpdateConfirmDialog() {
        updateState { it.copy(showUpdateConfirmDialog = false) }
    }

    /**
     * Загрузка обновления
     */
    // Добавим методы для работы с разрешениями
    fun checkInstallPermission(): Boolean {
        return if (updateInstaller is UpdateInstallerImpl) {
            (updateInstaller as UpdateInstallerImpl).canInstallPackages()
        } else {
            true
        }
    }

    fun getInstallPermissionIntent(): Intent? {
        return if (updateInstaller is UpdateInstallerImpl) {
            (updateInstaller as UpdateInstallerImpl).getInstallPermissionIntent()
        } else {
            null
        }
    }

    // Модификация метода загрузки обновления
    fun downloadUpdate() {
        val version = uiState.value.lastVersion ?: return

        // Перед загрузкой проверяем разрешение на установку
        if (!checkInstallPermission()) {
            sendEvent(SettingsEvent.RequestInstallPermission(getInstallPermissionIntent()))
            return
        }

        launchIO {
            updateState {
                it.copy(
                    isDownloadingUpdate = true,
                    showUpdateConfirmDialog = false,
                    error = null,
                    downloadProgress = 0 // Добавляем отслеживание прогресса
                )
            }

            try {
                val result = settingsUseCases.downloadUpdate(version)

                if (result.isSuccess) {
                    val filePath = result.getOrNull()!!

                    updateState {
                        it.copy(
                            isDownloadingUpdate = false,
                            downloadedUpdatePath = filePath,
                            error = null,
                            downloadProgress = 100 // Загрузка завершена
                        )
                    }

                    // Автоматически начинаем установку
                    installUpdate(filePath)
                } else {
                    val exception = result.exceptionOrNull()
                    updateState {
                        it.copy(
                            isDownloadingUpdate = false,
                            error = "Ошибка загрузки обновления: ${exception?.message}",
                            downloadProgress = 0
                        )
                    }
                    sendEvent(SettingsEvent.ShowSnackbar("Ошибка загрузки обновления"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error downloading update")
                updateState {
                    it.copy(
                        isDownloadingUpdate = false,
                        error = "Ошибка загрузки обновления: ${e.message}",
                        downloadProgress = 0
                    )
                }
                sendEvent(SettingsEvent.ShowSnackbar("Ошибка загрузки обновления"))
            }
        }
    }

    /**
     * Установка обновления
     */
    private fun installUpdate(filePath: String) {
        launchIO {
            updateState {
                it.copy(
                    isInstallingUpdate = true,
                    error = null
                )
            }

            try {
                // Вместо вызова UseCaseБ используем сервис напрямую
                val result = updateInstaller.initiateInstall(filePath)

                if (result.isSuccess) {
                    val uri = result.getOrNull()!!

                    updateState {
                        it.copy(
                            isInstallingUpdate = false,
                            error = null
                        )
                    }

                    // Отправляем событие для установки обновления с URI
                    sendEvent(SettingsEvent.InstallUpdate(uri))
                } else {
                    val exception = result.exceptionOrNull()
                    updateState {
                        it.copy(
                            isInstallingUpdate = false,
                            error = "Ошибка установки обновления: ${exception?.message}"
                        )
                    }
                    sendEvent(SettingsEvent.ShowSnackbar("Ошибка установки обновления"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error installing update")
                updateState {
                    it.copy(
                        isInstallingUpdate = false,
                        error = "Ошибка установки обновления: ${e.message}"
                    )
                }
                sendEvent(SettingsEvent.ShowSnackbar("Ошибка установки обновления"))
            }
        }
    }

    /**
     * Запуск локального веб-сервера
     */
    fun toggleWebServer() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = webServerManager.toggleServer()

                if (result.isSuccess) {
                    val isRunning = result.getOrNull() ?: false
                    updateState {
                        it.copy(
                            isWebServerRunning = isRunning,
                            isLoading = false,
                            error = null
                        )
                    }

                    val message = if (isRunning) {
                        "Локальный веб-сервер запущен"
                    } else {
                        "Локальный веб-сервер остановлен"
                    }
                    sendEvent(SettingsEvent.ShowSnackbar(message))
                } else {
                    val exception = result.exceptionOrNull()
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка управления сервером: ${exception?.message}"
                        )
                    }
                    sendEvent(SettingsEvent.ShowSnackbar("Ошибка управления сервером"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling web server")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка управления сервером: ${e.message}"
                    )
                }
                sendEvent(SettingsEvent.ShowSnackbar("Ошибка управления сервером"))
            }
        }
    }

    // Добавляем наблюдение за статусом синхронизации
    private fun observeSynchronizationState() {
        viewModelScope.launch {
            synchronizationController.isRunning.collect { isRunning ->
                updateState { it.copy(isSyncServiceRunning = isRunning) }
            }
        }

        viewModelScope.launch {
            synchronizationController.syncStatus.collect { status ->
                updateState { it.copy(syncStatus = status) }
            }
        }

        viewModelScope.launch {
            synchronizationController.lastSyncInfo.collect { info ->
                updateState { it.copy(lastSyncInfo = info) }
            }
        }

        viewModelScope.launch {
            synchronizationController.periodicSyncInfo.collect { info ->
                updateState {
                    it.copy(
                        periodicSyncEnabled = info.enabled,
                        syncIntervalSeconds = info.intervalSeconds,
                        nextScheduledSync = info.nextScheduledSync
                    )
                }
            }
        }
    }

    // Метод для запуска/остановки сервиса синхронизации
    fun toggleSyncService() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = synchronizationController.toggleService()

                if (result.isSuccess) {
                    val newState = result.getOrNull() ?: false
                    updateState {
                        it.copy(
                            isSyncServiceRunning = newState,
                            isLoading = false,
                            error = null
                        )
                    }

                    val message = if (newState) {
                        "Сервис синхронизации запущен"
                    } else {
                        "Сервис синхронизации остановлен"
                    }
                    sendEvent(SettingsEvent.ShowSnackbar(message))
                } else {
                    val exception = result.exceptionOrNull()
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка управления сервисом синхронизации: ${exception?.message}"
                        )
                    }
                    sendEvent(SettingsEvent.ShowSnackbar("Ошибка управления сервисом синхронизации"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling synchronization service")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка управления сервисом синхронизации: ${e.message}"
                    )
                }
                sendEvent(SettingsEvent.ShowSnackbar("Ошибка управления сервисом синхронизации"))
            }
        }
    }

    // Метод для запуска ручной синхронизации
    fun startManualSync() {
        launchIO {
            updateState { it.copy(isManualSyncing = true, error = null) }

            try {
                val result = synchronizationController.startManualSync()

                if (result.isSuccess) {
                    val syncResult = result.getOrNull()

                    if (syncResult != null && syncResult.successful) {
                        updateState {
                            it.copy(
                                isManualSyncing = false,
                                error = null
                            )
                        }

                        val message = "Синхронизация завершена успешно. " +
                                "Выгружено заданий: ${syncResult.tasksUploadedCount}, " +
                                "загружено заданий: ${syncResult.tasksDownloadedCount}, " +
                                "загружено товаров: ${syncResult.productsDownloadedCount}"
                        sendEvent(SettingsEvent.ShowSnackbar(message))
                    } else {
                        updateState {
                            it.copy(
                                isManualSyncing = false,
                                error = "Ошибка синхронизации: ${syncResult?.errorMessage}"
                            )
                        }
                        sendEvent(SettingsEvent.ShowSnackbar("Ошибка синхронизации"))
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    updateState {
                        it.copy(
                            isManualSyncing = false,
                            error = "Ошибка синхронизации: ${exception?.message}"
                        )
                    }
                    sendEvent(SettingsEvent.ShowSnackbar("Ошибка синхронизации"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during manual synchronization")
                updateState {
                    it.copy(
                        isManualSyncing = false,
                        error = "Ошибка синхронизации: ${e.message}"
                    )
                }
                sendEvent(SettingsEvent.ShowSnackbar("Ошибка синхронизации"))
            }
        }
    }

    // Метод для обновления настроек периодической синхронизации
    fun updatePeriodicSync(enabled: Boolean, intervalSeconds: Int? = null) {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = synchronizationController.updatePeriodicSync(enabled, intervalSeconds)

                if (result.isSuccess) {
                    updateState {
                        it.copy(
                            periodicSyncEnabled = enabled,
                            syncIntervalSeconds = intervalSeconds ?: it.syncIntervalSeconds,
                            isLoading = false,
                            error = null
                        )
                    }

                    val message = if (enabled) {
                        "Периодическая синхронизация включена"
                    } else {
                        "Периодическая синхронизация отключена"
                    }
                    sendEvent(SettingsEvent.ShowSnackbar(message))
                } else {
                    val exception = result.exceptionOrNull()
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка обновления настроек синхронизации: ${exception?.message}"
                        )
                    }
                    sendEvent(SettingsEvent.ShowSnackbar("Ошибка обновления настроек синхронизации"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating periodic sync settings")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка обновления настроек синхронизации: ${e.message}"
                    )
                }
                sendEvent(SettingsEvent.ShowSnackbar("Ошибка обновления настроек синхронизации"))
            }
        }
    }

    /**
     * Навигация к экрану серверов
     */
    fun navigateToServerList() {
        sendEvent(SettingsEvent.NavigateToServerList)
    }

    /**
     * Навигация назад
     */
    fun navigateBack() {
        sendEvent(SettingsEvent.NavigateBack)
    }

    fun onSyncHistoryClick() {
        sendEvent(SettingsEvent.NavigateToSyncHistory)
    }
}