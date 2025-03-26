package com.synngate.synnframe.presentation.ui.logs

/*
 * Обратите внимание: для применения этого файла замените существующий файл LogListViewModel.kt
 * Этот файл содержит обновленный ViewModel с поддержкой диалога выбора периода фильтрации
 */

import com.synngate.synnframe.domain.entity.LogType
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.log.LogUseCases
import com.synngate.synnframe.presentation.common.dialog.DateFilterPreset
import com.synngate.synnframe.presentation.ui.logs.model.LogListEvent
import com.synngate.synnframe.presentation.ui.logs.model.LogListState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LogListViewModel(
    private val logUseCases: LogUseCases,
    private val loggingService: LoggingService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<LogListState, LogListEvent>(LogListState(), ioDispatcher) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    init {
        loadLogs()
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
                    updateState { it.copy(
                        isLoading = false,
                        error = "Error loading logs: ${e.message}"
                    ) }
                }.collect { logs ->
                    updateState { it.copy(
                        logs = logs,
                        isLoading = false,
                        error = null
                    ) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during logs loading")
                updateState { it.copy(
                    isLoading = false,
                    error = "Error loading logs: ${e.message}"
                ) }

                loggingService.logError("Error loading logs: ${e.message}")
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
                activeDateFilterPreset = null // Сбрасываем предустановленный период
            )
        }
        loadLogs()
    }

    fun applyDateFilterPreset(preset: DateFilterPreset) {
        val (startDate, endDate) = preset.getDates()
        updateState {
            it.copy(
                dateFromFilter = startDate,
                dateToFilter = endDate,
                activeDateFilterPreset = preset,
                isDateFilterDialogVisible = false
            )
        }
        loadLogs()
    }

    fun resetFilters() {
        updateState { it.copy(
            messageFilter = "",
            selectedTypes = emptySet(),
            dateFromFilter = null,
            dateToFilter = null,
            activeDateFilterPreset = null
        ) }
        loadLogs()
    }

    fun clearDateFilter() {
        updateState { it.copy(
            dateFromFilter = null,
            dateToFilter = null,
            activeDateFilterPreset = null
        ) }
        loadLogs()
    }

    fun showDateFilterDialog() {
        sendEvent(LogListEvent.ShowDateFilterDialog)
        updateState { it.copy(isDateFilterDialogVisible = true) }
    }

    fun hideDateFilterDialog() {
        sendEvent(LogListEvent.HideDateFilterDialog)
        updateState { it.copy(isDateFilterDialogVisible = false) }
    }

    fun showDeleteAllConfirmation() {
        sendEvent(LogListEvent.ShowDeleteAllConfirmation)
    }

    fun deleteAllLogs() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val result = logUseCases.deleteAllLogs()
                if (result.isSuccess) {
                    updateState { it.copy(
                        logs = emptyList(),
                        isLoading = false,
                        error = null
                    ) }

                    loggingService.logInfo("Все логи были удалены")
                    sendEvent(LogListEvent.ShowSnackbar("Все логи успешно удалены"))
                } else {
                    val exception = result.exceptionOrNull()
                    updateState { it.copy(
                        isLoading = false,
                        error = "Ошибка удаления логов: ${exception?.message}"
                    ) }

                    loggingService.logError("Ошибка удаления логов: ${exception?.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during logs deletion")
                updateState { it.copy(
                    isLoading = false,
                    error = "Ошибка удаления логов: ${e.message}"
                ) }

                loggingService.logError("Ошибка удаления логов: ${e.message}")
            }
        }
    }

    fun navigateToLogDetail(logId: Int) {
        sendEvent(LogListEvent.NavigateToLogDetail(logId))
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

    fun formatDateFilterPeriod(): String {
        val fromDate = uiState.value.dateFromFilter
        val toDate = uiState.value.dateToFilter

        if (fromDate == null || toDate == null) return ""

        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        return "${fromDate.format(formatter)} - ${toDate.format(formatter)}"
    }
}