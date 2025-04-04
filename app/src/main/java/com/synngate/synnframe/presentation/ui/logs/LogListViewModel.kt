package com.synngate.synnframe.presentation.ui.logs

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.LogType
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.log.LogUseCases
import com.synngate.synnframe.presentation.ui.logs.model.LogListState
import com.synngate.synnframe.presentation.ui.logs.model.LogListStateEvent
import com.synngate.synnframe.presentation.ui.logs.model.LogListUiEvent
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LogListViewModel(
    private val logUseCases: LogUseCases,
    private val loggingService: LoggingService,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<LogListState, LogListUiEvent>(LogListState(), ioDispatcher) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    private val stateEvents = MutableSharedFlow<LogListStateEvent>()

    init {
        loadLogs()

        // Обрабатываем события состояния внутри ViewModel
        viewModelScope.launch {
            stateEvents.collect { event ->
                // Применяем обновление состояния через интерфейс
                handleStateEvent(event)

                // Для событий с побочными эффектами выполняем дополнительные действия
                when (event) {
                    is LogListStateEvent.CleanupOldLogs -> cleanupOldLogs(event.days)
                    is LogListStateEvent.DeleteAllLogs -> deleteAllLogs()
                    else -> {} // Для всех остальных событий достаточно обработки через handleStateEvent
                }
            }
        }
    }

    // Публичные методы для триггера событий
    fun showDeleteAllConfirmation() {
        viewModelScope.launch {
            stateEvents.emit(LogListStateEvent.ShowDeleteAllConfirmation)
        }
    }

    fun hideDeleteAllConfirmation() {
        viewModelScope.launch {
            stateEvents.emit(LogListStateEvent.HideDeleteAllConfirmation)
        }
    }

    fun showCleanupDialog() {
        viewModelScope.launch {
            stateEvents.emit(LogListStateEvent.ShowCleanupDialog)
        }
    }

    fun hideCleanupDialog() {
        viewModelScope.launch {
            stateEvents.emit(LogListStateEvent.HideCleanupDialog)
        }
    }

    fun showDateFilterDialog() {
        viewModelScope.launch {
            stateEvents.emit(LogListStateEvent.ShowDateFilterDialog)
        }
    }

    fun hideDateFilterDialog() {
        viewModelScope.launch {
            stateEvents.emit(LogListStateEvent.HideDateFilterDialog)
        }
    }

    fun onCleanupConfirmed(days: Int) {
        viewModelScope.launch {
            stateEvents.emit(LogListStateEvent.HideCleanupDialog)
            stateEvents.emit(LogListStateEvent.CleanupOldLogs(days))
        }
    }

    fun onDeleteAllConfirmed() {
        viewModelScope.launch {
            stateEvents.emit(LogListStateEvent.HideDeleteAllConfirmation)
            stateEvents.emit(LogListStateEvent.DeleteAllLogs)
        }
    }

    fun loadLogs() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val currentState = uiState.value

                val messageFilter = currentState.messageFilter.takeIf { it.isNotEmpty() }
                val typeFilter = currentState.selectedTypes.takeIf { it.isNotEmpty() }?.toList()
                val dateFrom = currentState.dateFromFilter
                val dateTo = currentState.dateToFilter

                logUseCases.getFilteredLogs(
                    messageFilter = messageFilter,
                    typeFilter = typeFilter,
                    dateFromFilter = dateFrom,
                    dateToFilter = dateTo
                ).catch { e ->
                    Timber.e(e, "Error loading logs")
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Error loading logs: ${e.message}"
                        )
                    }
                }.collect { logs ->
                    updateState {
                        it.copy(
                            logs = logs,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during logs loading")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Error loading logs: ${e.message}"
                    )
                }

                loggingService.logError("Error loading logs: ${e.message}")
            }
        }
    }

    private fun cleanupOldLogs(daysToKeep: Int) {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = logUseCases.cleanupOldLogs(daysToKeep)
                if (result.isSuccess) {
                    val deletedCount = result.getOrNull() ?: 0
                    updateState { it.copy(isLoading = false, error = null) }

                    loadLogs()

                    sendEvent(LogListUiEvent.ShowSnackbar("Удалено $deletedCount логов старше $daysToKeep дней"))
                } else {
                    val exception = result.exceptionOrNull()
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка при очистке логов: ${exception?.message}"
                        )
                    }
                    loggingService.logError("Ошибка при очистке логов: ${exception?.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during logs cleanup")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка при очистке логов: ${e.message}"
                    )
                }
                loggingService.logError("Ошибка при очистке логов: ${e.message}")
            }
        }
    }

    fun updateMessageFilter(filter: String) {
        updateState { it.copy(messageFilter = filter) }
        loadLogs()
    }

    fun updateTypeFilter(types: Set<LogType>) {
        updateState { it.copy(selectedTypes = types) }
        loadLogs()
    }

    fun updateDateFilter(dateFrom: LocalDateTime?, dateTo: LocalDateTime?) {
        updateState {
            it.copy(
                dateFromFilter = dateFrom,
                dateToFilter = dateTo,
                activeDateFilterPreset = null
            )
        }
        loadLogs()
    }

    fun clearDateFilter() {
        updateState {
            it.copy(
                dateFromFilter = null,
                dateToFilter = null,
                activeDateFilterPreset = null
            )
        }
        loadLogs()
    }

    private fun deleteAllLogs() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = logUseCases.deleteAllLogs()
                if (result.isSuccess) {
                    updateState {
                        it.copy(
                            logs = emptyList(),
                            isLoading = false,
                            error = null
                        )
                    }

                    loggingService.logInfo("Все логи были удалены")
                    sendEvent(LogListUiEvent.ShowSnackbar("Все логи успешно удалены"))
                } else {
                    val exception = result.exceptionOrNull()
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Ошибка удаления логов: ${exception?.message}"
                        )
                    }

                    loggingService.logError("Ошибка удаления логов: ${exception?.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during logs deletion")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка удаления логов: ${e.message}"
                    )
                }

                loggingService.logError("Ошибка удаления логов: ${e.message}")
            }
        }
    }

    fun navigateToLogDetail(logId: Int) {
        sendEvent(LogListUiEvent.NavigateToLogDetail(logId))
    }

    fun formatLogType(type: LogType): String {
        return when (type) {
            LogType.INFO -> "Информация"
            LogType.WARNING -> "Предупреждение"
            LogType.ERROR -> "Ошибка"
        }
    }

    fun formatLogDate(dateTime: LocalDateTime): String {
        return dateTime.format(dateFormatter)
    }
}