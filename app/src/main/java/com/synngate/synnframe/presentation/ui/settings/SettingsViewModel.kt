// Файл: com.synngate.synnframe.presentation.ui.settings.SettingsViewModel.kt

package com.synngate.synnframe.presentation.ui.settings

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.server.ServerUseCases
import com.synngate.synnframe.domain.usecase.settings.SettingsUseCases
import com.synngate.synnframe.presentation.di.ClearableViewModel
import com.synngate.synnframe.presentation.di.SettingsViewModel
import com.synngate.synnframe.presentation.theme.ThemeMode
import com.synngate.synnframe.presentation.ui.settings.model.SettingsState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min

/**
 * ViewModel для экрана настроек приложения
 */
class SettingsViewModelImpl(
    private val settingsUseCases: SettingsUseCases,
    private val serverUseCases: ServerUseCases,
    private val loggingService: LoggingService,
    private val ioDispatcher: CoroutineDispatcher
) : ClearableViewModel(), SettingsViewModel {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    /**
     * Класс для объединения всех настроек
     */
    private data class SettingsData(
        val showServersOnStartup: Boolean,
        val periodicUploadEnabled: Boolean,
        val uploadIntervalSeconds: Int,
        val themeMode: ThemeMode,
        val languageCode: String,
        val navigationButtonHeight: Float,
        val activeServer: Server?
    )

    /**
     * Загрузка всех настроек приложения
     */
    private fun loadSettings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Загружаем настройки последовательно
            val showServers = settingsUseCases.showServersOnStartup.first()
            val periodicUpload = settingsUseCases.periodicUploadEnabled.first()
            val uploadInterval = settingsUseCases.uploadIntervalSeconds.first()
            val themeMode = settingsUseCases.themeMode.first()
            val languageCode = settingsUseCases.languageCode.first()
            val buttonHeight = settingsUseCases.navigationButtonHeight.first()
            val activeServer = serverUseCases.getActiveServer().first()

            // Обновляем состояние с загруженными данными
            _state.update { it.copy(
                showServersOnStartup = showServers,
                periodicUploadEnabled = periodicUpload,
                uploadIntervalSeconds = uploadInterval,
                themeMode = themeMode,
                languageCode = languageCode,
                navigationButtonHeight = buttonHeight,
                activeServer = activeServer,
                isLoading = false
            ) }

            // Запускаем наблюдение за изменениями после первоначальной загрузки
            observeSettingsChanges()
        }
    }

    /**
     * Наблюдение за изменениями настроек
     */
    private fun observeSettingsChanges() {
        viewModelScope.launch {
            settingsUseCases.showServersOnStartup.collect { showServers ->
                _state.update { it.copy(showServersOnStartup = showServers) }
            }
        }

        // Аналогично для других настроек...
    }

    /**
     * Обновление настройки отображения серверов при запуске
     */
    fun updateShowServersOnStartup(show: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val result = settingsUseCases.setShowServersOnStartup(show)

                if (result.isSuccess) {
                    _state.update { it.copy(
                        showServersOnStartup = show,
                        isLoading = false,
                        error = null
                    ) }
                } else {
                    val exception = result.exceptionOrNull()
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Ошибка обновления настройки: ${exception?.message}"
                    ) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating showServersOnStartup setting")
                _state.update { it.copy(
                    isLoading = false,
                    error = "Ошибка обновления настройки: ${e.message}"
                ) }

                viewModelScope.launch {
                    loggingService.logError("Ошибка обновления настройки отображения серверов: ${e.message}")
                }
            }
        }
    }

    /**
     * Обновление настроек периодической выгрузки
     */
    fun updatePeriodicUpload(enabled: Boolean, intervalSeconds: Int? = null) {
        viewModelScope.launch(ioDispatcher) {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val interval = intervalSeconds ?: state.value.uploadIntervalSeconds

                val result = settingsUseCases.setPeriodicUpload(enabled, interval)

                if (result.isSuccess) {
                    _state.update { it.copy(
                        periodicUploadEnabled = enabled,
                        uploadIntervalSeconds = interval,
                        isLoading = false,
                        error = null
                    ) }
                } else {
                    val exception = result.exceptionOrNull()
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Ошибка обновления настроек выгрузки: ${exception?.message}"
                    ) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating periodic upload settings")
                _state.update { it.copy(
                    isLoading = false,
                    error = "Ошибка обновления настроек выгрузки: ${e.message}"
                ) }

                viewModelScope.launch {
                    loggingService.logError("Ошибка обновления настроек периодической выгрузки: ${e.message}")
                }
            }
        }
    }

    /**
     * Обновление интервала выгрузки (в секундах)
     */
    fun updateUploadInterval(intervalSeconds: Int) {
        val validInterval = max(30, min(3600, intervalSeconds)) // Ограничиваем от 30 секунд до 1 часа
        updatePeriodicUpload(state.value.periodicUploadEnabled, validInterval)
    }

    /**
     * Обновление режима темы
     */
    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch(ioDispatcher) {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val result = settingsUseCases.setThemeMode(themeMode)

                if (result.isSuccess) {
                    _state.update { it.copy(
                        themeMode = themeMode,
                        isLoading = false,
                        error = null
                    ) }
                } else {
                    val exception = result.exceptionOrNull()
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Ошибка обновления темы: ${exception?.message}"
                    ) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating theme mode")
                _state.update { it.copy(
                    isLoading = false,
                    error = "Ошибка обновления темы: ${e.message}"
                ) }

                viewModelScope.launch {
                    loggingService.logError("Ошибка обновления режима темы: ${e.message}")
                }
            }
        }
    }

    /**
     * Обновление языка интерфейса
     */
    fun updateLanguageCode(languageCode: String) {
        viewModelScope.launch(ioDispatcher) {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val result = settingsUseCases.setLanguageCode(languageCode)

                if (result.isSuccess) {
                    _state.update { it.copy(
                        languageCode = languageCode,
                        isLoading = false,
                        error = null
                    ) }
                } else {
                    val exception = result.exceptionOrNull()
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Ошибка обновления языка: ${exception?.message}"
                    ) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating language code")
                _state.update { it.copy(
                    isLoading = false,
                    error = "Ошибка обновления языка: ${e.message}"
                ) }

                viewModelScope.launch {
                    loggingService.logError("Ошибка обновления языка интерфейса: ${e.message}")
                }
            }
        }
    }

    /**
     * Обновление высоты кнопки навигации
     */
    fun updateNavigationButtonHeight(height: Float) {
        viewModelScope.launch(ioDispatcher) {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val validHeight = max(48f, min(96f, height)) // Ограничиваем от 48dp до 96dp
                val result = settingsUseCases.setNavigationButtonHeight(validHeight)

                if (result.isSuccess) {
                    _state.update { it.copy(
                        navigationButtonHeight = validHeight,
                        isLoading = false,
                        error = null
                    ) }
                } else {
                    val exception = result.exceptionOrNull()
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Ошибка обновления высоты кнопки: ${exception?.message}"
                    ) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating navigation button height")
                _state.update { it.copy(
                    isLoading = false,
                    error = "Ошибка обновления высоты кнопки: ${e.message}"
                ) }

                viewModelScope.launch {
                    loggingService.logError("Ошибка обновления высоты кнопки навигации: ${e.message}")
                }
            }
        }
    }

    /**
     * Проверка наличия обновлений
     */
    fun checkForUpdates() {
        viewModelScope.launch(ioDispatcher) {
            _state.update { it.copy(
                isCheckingForUpdates = true,
                error = null,
                lastVersion = null,
                releaseDate = null,
                showUpdateConfirmDialog = false
            ) }

            try {
                val result = settingsUseCases.checkForUpdates()

                if (result.isSuccess) {
                    val (version, date) = result.getOrNull()!!

                    _state.update { it.copy(
                        isCheckingForUpdates = false,
                        lastVersion = version,
                        releaseDate = date,
                        showUpdateConfirmDialog = version != null,
                        error = null
                    ) }
                } else {
                    val exception = result.exceptionOrNull()
                    _state.update { it.copy(
                        isCheckingForUpdates = false,
                        error = "Ошибка проверки обновлений: ${exception?.message}"
                    ) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking for updates")
                _state.update { it.copy(
                    isCheckingForUpdates = false,
                    error = "Ошибка проверки обновлений: ${e.message}"
                ) }

                viewModelScope.launch {
                    loggingService.logError("Ошибка проверки наличия обновлений: ${e.message}")
                }
            }
        }
    }

    /**
     * Скрыть диалог подтверждения обновления
     */
    fun hideUpdateConfirmDialog() {
        _state.update { it.copy(showUpdateConfirmDialog = false) }
    }

    /**
     * Загрузка обновления
     */
    fun downloadUpdate() {
        val version = state.value.lastVersion ?: return

        viewModelScope.launch(ioDispatcher) {
            _state.update { it.copy(
                isDownloadingUpdate = true,
                showUpdateConfirmDialog = false,
                error = null
            ) }

            try {
                val result = settingsUseCases.downloadUpdate(version)

                if (result.isSuccess) {
                    val filePath = result.getOrNull()!!

                    _state.update { it.copy(
                        isDownloadingUpdate = false,
                        downloadedUpdatePath = filePath,
                        error = null
                    ) }

                    // Автоматически начинаем установку
                    installUpdate(filePath)
                } else {
                    val exception = result.exceptionOrNull()
                    _state.update { it.copy(
                        isDownloadingUpdate = false,
                        error = "Ошибка загрузки обновления: ${exception?.message}"
                    ) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error downloading update")
                _state.update { it.copy(
                    isDownloadingUpdate = false,
                    error = "Ошибка загрузки обновления: ${e.message}"
                ) }

                viewModelScope.launch {
                    loggingService.logError("Ошибка загрузки обновления: ${e.message}")
                }
            }
        }
    }

    /**
     * Установка обновления
     */
    private fun installUpdate(filePath: String) {
        viewModelScope.launch(ioDispatcher) {
            _state.update { it.copy(
                isInstallingUpdate = true,
                error = null
            ) }

            try {
                val result = settingsUseCases.installUpdate(filePath)

                if (result.isSuccess) {
                    _state.update { it.copy(
                        isInstallingUpdate = false,
                        error = null
                    ) }

                    // Установка будет выполнена через Intent в UI слое
                } else {
                    val exception = result.exceptionOrNull()
                    _state.update { it.copy(
                        isInstallingUpdate = false,
                        error = "Ошибка установки обновления: ${exception?.message}"
                    ) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error installing update")
                _state.update { it.copy(
                    isInstallingUpdate = false,
                    error = "Ошибка установки обновления: ${e.message}"
                ) }

                viewModelScope.launch {
                    loggingService.logError("Ошибка установки обновления: ${e.message}")
                }
            }
        }
    }

    /**
     * Запуск локального веб-сервера (заглушка)
     *
     * Примечание: Реальная реализация будет использовать сервис для управления
     * локальным веб-сервером, который должен быть реализован отдельно
     */
    fun toggleWebServer() {
        _state.update {
            it.copy(isWebServerRunning = !it.isWebServerRunning)
        }

        viewModelScope.launch {
            if (state.value.isWebServerRunning) {
                loggingService.logInfo("Локальный веб-сервер запущен")
            } else {
                loggingService.logInfo("Локальный веб-сервер остановлен")
            }
        }
    }
}