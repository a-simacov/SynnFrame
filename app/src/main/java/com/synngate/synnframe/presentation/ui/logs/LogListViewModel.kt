package com.synngate.synnframe.presentation.ui.logs

import com.synngate.synnframe.domain.entity.LogType
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.log.LogUseCases
import com.synngate.synnframe.presentation.ui.logs.model.LogListEvent
import com.synngate.synnframe.presentation.ui.logs.model.LogListState
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * ViewModel для экрана списка логов
 */
class LogListViewModel(
    private val logUseCases: LogUseCases,
    private val loggingService: LoggingService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<LogListState, LogListEvent>(LogListState(), ioDispatcher) {

    // Форматтер для отображения даты в UI
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    init {
        loadLogs()
    }

    /**
     * Загрузка логов с применением текущих фильтров
     */
    fun loadLogs() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val currentState = uiState.value

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
                    updateState { it.copy(
                        isLoading = false,
                        error = "Ошибка загрузки логов: ${e.message}"
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
                    error = "Ошибка загрузки логов: ${e.message}"
                ) }

                loggingService.logError("Ошибка загрузки логов: ${e.message}")
            }
        }
    }

    /**
     * Обновление фильтра по сообщению
     */
    fun updateMessageFilter(filter: String) {
        updateState { it.copy(messageFilter = filter) }
        loadLogs()
    }

    /**
     * Обновление фильтра по типу лога
     */
    fun updateTypeFilter(types: Set<LogType>) {
        updateState { it.copy(selectedTypes = types) }
        loadLogs()
    }

    /**
     * Обновление фильтра по диапазону дат
     */
    fun updateDateFilter(dateFrom: LocalDateTime?, dateTo: LocalDateTime?) {
        updateState { it.copy(dateFromFilter = dateFrom, dateToFilter = dateTo) }
    }

    /**
     * Сброс всех фильтров
     */
    fun resetFilters() {
        updateState { it.copy(
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
        updateState { it.copy(isFilterPanelVisible = !it.isFilterPanelVisible) }
    }

    /**
     * Показать диалог подтверждения удаления всех логов
     */
    fun showDeleteAllConfirmation() {
        sendEvent(LogListEvent.ShowDeleteAllConfirmation)
    }

    /**
     * Удаление всех логов
     */
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

    /**
     * Навигация к детальной информации о логе
     */
    fun navigateToLogDetail(logId: Int) {
        sendEvent(LogListEvent.NavigateToLogDetail(logId))
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
        return dateTime.format(dateFormatter)
    }
}