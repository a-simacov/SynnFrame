// Файл: com.synngate.synnframe.presentation.ui.logs.LogListViewModel.kt

package com.synngate.synnframe.presentation.ui.logs

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.LogType
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.log.LogUseCases
import com.synngate.synnframe.presentation.di.ClearableViewModel
import com.synngate.synnframe.presentation.ui.logs.model.LogListState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime

/**
 * ViewModel для экрана списка логов
 */
class LogListViewModel(
    private val logUseCases: LogUseCases,
    private val loggingService: LoggingService,
    private val ioDispatcher: CoroutineDispatcher
) : ClearableViewModel() {

    private val _state = MutableStateFlow(LogListState())
    val state: StateFlow<LogListState> = _state.asStateFlow()

    init {
        loadLogs()
    }

    /**
     * Загрузка логов с применением текущих фильтров
     */
    fun loadLogs() {
        viewModelScope.launch(ioDispatcher) {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val currentState = state.value

                // Подготовка параметров фильтрации
                val messageFilter = currentState.messageFilter.takeIf { it.isNotEmpty() }
                val typeFilter = currentState.selectedTypes.takeIf { it.isNotEmpty() }?.toList()
                val dateFrom = currentState.dateFromFilter
                val dateTo = currentState.dateToFilter

                // Получение отфильтрованных логов
                logUseCases.getFilteredLogs(
                    messageFilter = messageFilter,
                    typeFilter = typeFilter,
                    dateFromFilter = dateFrom,
                    dateToFilter = dateTo
                ).catch { e ->
                    Timber.e(e, "Error loading logs")
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Ошибка загрузки логов: ${e.message}"
                    ) }
                }.collect { logs ->
                    _state.update { it.copy(
                        logs = logs,
                        isLoading = false,
                        error = null
                    ) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during logs loading")
                _state.update { it.copy(
                    isLoading = false,
                    error = "Ошибка загрузки логов: ${e.message}"
                ) }

                viewModelScope.launch {
                    loggingService.logError("Ошибка загрузки логов: ${e.message}")
                }
            }
        }
    }

    /**
     * Обновление фильтра по сообщению
     */
    fun updateMessageFilter(filter: String) {
        _state.update { it.copy(messageFilter = filter) }
        loadLogs()
    }

    /**
     * Обновление фильтра по типу лога
     */
    fun updateTypeFilter(types: Set<LogType>) {
        _state.update { it.copy(selectedTypes = types) }
        loadLogs()
    }

    /**
     * Обновление фильтра по диапазону дат
     */
    fun updateDateFilter(dateFrom: LocalDateTime?, dateTo: LocalDateTime?) {
        _state.update { it.copy(dateFromFilter = dateFrom, dateToFilter = dateTo) }
        loadLogs()
    }

    /**
     * Сброс всех фильтров
     */
    fun resetFilters() {
        _state.update { it.copy(
            messageFilter = "",
            selectedTypes = emptySet(),
            dateFromFilter = null,
            dateToFilter = null
        ) }
        loadLogs()
    }

    /**
     * Переключение видимости панели фильтров
     */
    fun toggleFilterPanel() {
        _state.update { it.copy(isFilterPanelVisible = !it.isFilterPanelVisible) }
    }

    /**
     * Удаление всех логов
     */
    fun deleteAllLogs() {
        viewModelScope.launch(ioDispatcher) {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val result = logUseCases.deleteAllLogs()
                if (result.isSuccess) {
                    _state.update { it.copy(
                        logs = emptyList(),
                        isLoading = false,
                        error = null
                    ) }

                    loggingService.logInfo("Все логи были удалены")
                } else {
                    val exception = result.exceptionOrNull()
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Ошибка удаления логов: ${exception?.message}"
                    ) }

                    loggingService.logError("Ошибка удаления логов: ${exception?.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during logs deletion")
                _state.update { it.copy(
                    isLoading = false,
                    error = "Ошибка удаления логов: ${e.message}"
                ) }

                viewModelScope.launch {
                    loggingService.logError("Ошибка удаления логов: ${e.message}")
                }
            }
        }
    }

    /**
     * Удаление старых логов
     */
    fun cleanupOldLogs(daysToKeep: Int = 30) {
        viewModelScope.launch(ioDispatcher) {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val result = logUseCases.cleanupOldLogs(daysToKeep)
                if (result.isSuccess) {
                    val deletedCount = result.getOrDefault(0)
                    _state.update { it.copy(isLoading = false, error = null) }
                    loadLogs() // Перезагружаем список логов

                    loggingService.logInfo("Удалено старых логов: $deletedCount")
                } else {
                    val exception = result.exceptionOrNull()
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Ошибка очистки старых логов: ${exception?.message}"
                    ) }

                    loggingService.logError("Ошибка очистки старых логов: ${exception?.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during old logs cleanup")
                _state.update { it.copy(
                    isLoading = false,
                    error = "Ошибка очистки старых логов: ${e.message}"
                ) }

                viewModelScope.launch {
                    loggingService.logError("Ошибка очистки старых логов: ${e.message}")
                }
            }
        }
    }

    /**
     * Форматирование типа лога для отображения
     */
    fun formatLogType(type: LogType): String {
        return when (type) {
            LogType.INFO -> "Информация"
            LogType.WARNING -> "Предупреждение"
            LogType.ERROR -> "Ошибка"
        }
    }

    /**
     * Форматирование даты лога для отображения
     */
    fun formatLogDate(dateTime: LocalDateTime): String {
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
    }
}