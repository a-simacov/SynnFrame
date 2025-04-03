package com.synngate.synnframe.presentation.ui.logs

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.LogType
import com.synngate.synnframe.domain.service.ClipboardService
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.log.LogUseCases
import com.synngate.synnframe.presentation.ui.logs.model.LogDetailState
import com.synngate.synnframe.presentation.ui.logs.model.LogDetailStateEvent
import com.synngate.synnframe.presentation.ui.logs.model.LogDetailUiEvent
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.format.DateTimeFormatter

class LogDetailViewModel(
    private val logId: Int,
    private val logUseCases: LogUseCases,
    private val loggingService: LoggingService,
    private val clipboardService: ClipboardService,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<LogDetailState, LogDetailUiEvent>(LogDetailState(), ioDispatcher) {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    private val stateEvents = MutableSharedFlow<LogDetailStateEvent>()

    init {
        loadLog()

        viewModelScope.launch {
            stateEvents.collect { event ->
                handleStateEvent(event)
            }
        }
    }

    private fun handleStateEvent(event: LogDetailStateEvent) {
        when (event) {
            is LogDetailStateEvent.ShowDeleteConfirmation -> {
                updateState { it.copy(isDeleteConfirmationVisible = true) }
            }

            is LogDetailStateEvent.HideDeleteConfirmation -> {
                updateState { it.copy(isDeleteConfirmationVisible = false) }
            }
        }
    }

    fun loadLog() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                val log = logUseCases.getLogById(logId)

                if (log != null) {
                    updateState {
                        it.copy(
                            log = log,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = "Лог с ID $logId не найден"
                        )
                    }

                    loggingService.logWarning("Попытка просмотра несуществующего лога с ID $logId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading log with ID $logId")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка загрузки лога: ${e.message}"
                    )
                }

                loggingService.logError("Ошибка загрузки лога с ID $logId: ${e.message}")
            }
        }
    }

    fun copyLogToClipboard() {
        val log = uiState.value.log ?: return

        val formattedLog = """
            ID: ${log.id}
            Тип: ${formatLogType(log.type)}
            Дата: ${log.createdAt.format(dateFormatter)}
            Сообщение: ${log.message}
        """.trimIndent()

        val isCopied = clipboardService.copyToClipboard(
            text = formattedLog,
            label = "Log #${log.id}"
        )

        updateState { it.copy(isTextCopied = isCopied) }

        // Автоматически сбрасываем флаг через некоторое время
        launchIO {
            delay(2000)
            updateState { it.copy(isTextCopied = false) }
        }

        if (isCopied) {
            launchIO {
                loggingService.logInfo("Лог с ID ${log.id} скопирован в буфер обмена")
                sendEvent(LogDetailUiEvent.ShowSnackbar("Лог успешно скопирован в буфер обмена"))
            }
        }
    }

    fun showDeleteConfirmation() {
        viewModelScope.launch {
            stateEvents.emit(LogDetailStateEvent.ShowDeleteConfirmation)
        }
    }

    fun hideDeleteConfirmation() {
        viewModelScope.launch {
            stateEvents.emit(LogDetailStateEvent.HideDeleteConfirmation)
        }
    }

    fun deleteLog() {
        launchIO {
            updateState {
                it.copy(
                    isDeletingLog = true,
                    error = null
                )
            }

            try {
                val result = logUseCases.deleteLog(logId)

                if (result.isSuccess) {
                    updateState {
                        it.copy(
                            isDeletingLog = false,
                            error = null
                        )
                    }

                    loggingService.logInfo("Лог с ID $logId успешно удален")

                    // Отправляем событие навигации назад вместо прямого вызова колбэка
                    sendEvent(LogDetailUiEvent.NavigateBack)
                } else {
                    val exception = result.exceptionOrNull()
                    updateState {
                        it.copy(
                            isDeletingLog = false,
                            error = "Ошибка удаления лога: ${exception?.message}"
                        )
                    }

                    loggingService.logError("Ошибка удаления лога с ID $logId: ${exception?.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during log deletion")
                updateState {
                    it.copy(
                        isDeletingLog = false,
                        error = "Ошибка удаления лога: ${e.message}"
                    )
                }

                loggingService.logError("Исключение при удалении лога с ID $logId: ${e.message}")
            }
        }
    }

    private fun formatLogType(type: LogType): String {
        return when (type) {
            LogType.INFO -> "Информация"
            LogType.WARNING -> "Предупреждение"
            LogType.ERROR -> "Ошибка"
        }
    }
}