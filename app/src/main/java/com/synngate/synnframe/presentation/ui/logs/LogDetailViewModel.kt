// Файл: com.synngate.synnframe.presentation.ui.logs.LogDetailViewModel.kt

package com.synngate.synnframe.presentation.ui.logs

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.service.ClipboardService
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.domain.usecase.log.LogUseCases
import com.synngate.synnframe.presentation.di.ClearableViewModel
import com.synngate.synnframe.presentation.ui.logs.model.LogDetailState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.format.DateTimeFormatter

/**
 * ViewModel для экрана деталей лога
 */
class LogDetailViewModel(
    private val logId: Int,
    private val logUseCases: LogUseCases,
    private val loggingService: LoggingService,
    private val clipboardService: ClipboardService,
    private val ioDispatcher: CoroutineDispatcher
) : ClearableViewModel() {

    private val _state = MutableStateFlow(LogDetailState())
    val state: StateFlow<LogDetailState> = _state.asStateFlow()

    // Форматтер для отображения даты
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    init {
        loadLog()
    }

    /**
     * Загрузка данных лога
     */
    fun loadLog() {
        viewModelScope.launch(ioDispatcher) {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val log = logUseCases.getLogById(logId)

                if (log != null) {
                    _state.update {
                        it.copy(
                            log = log,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Лог с ID $logId не найден"
                        )
                    }

                    loggingService.logWarning("Попытка просмотра несуществующего лога с ID $logId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading log with ID $logId")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка загрузки лога: ${e.message}"
                    )
                }

                viewModelScope.launch {
                    loggingService.logError("Ошибка загрузки лога с ID $logId: ${e.message}")
                }
            }
        }
    }

    /**
     * Копирование содержимого лога в буфер обмена
     */
    fun copyLogToClipboard() {
        val log = state.value.log ?: return

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

        _state.update { it.copy(isTextCopied = isCopied) }

        // Автоматически сбрасываем флаг через некоторое время
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000) // 2 секунды
            _state.update { it.copy(isTextCopied = false) }
        }

        // Логируем успешное копирование
        if (isCopied) {
            viewModelScope.launch {
                loggingService.logInfo("Лог с ID ${log.id} скопирован в буфер обмена")
            }
        }
    }

    /**
     * Показать диалог подтверждения удаления
     */
    fun showDeleteConfirmation() {
        _state.update { it.copy(showDeleteConfirmation = true) }
    }

    /**
     * Скрыть диалог подтверждения удаления
     */
    fun hideDeleteConfirmation() {
        _state.update { it.copy(showDeleteConfirmation = false) }
    }

    /**
     * Удаление лога
     */
    fun deleteLog(onDeleted: () -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            _state.update {
                it.copy(
                    isDeletingLog = true,
                    showDeleteConfirmation = false,
                    error = null
                )
            }

            try {
                val result = logUseCases.deleteLog(logId)

                if (result.isSuccess) {
                    _state.update {
                        it.copy(
                            isDeletingLog = false,
                            error = null
                        )
                    }

                    loggingService.logInfo("Лог с ID $logId успешно удален")

                    // Вызываем колбэк для навигации назад
                    onDeleted()
                } else {
                    val exception = result.exceptionOrNull()
                    _state.update {
                        it.copy(
                            isDeletingLog = false,
                            error = "Ошибка удаления лога: ${exception?.message}"
                        )
                    }

                    loggingService.logError("Ошибка удаления лога с ID $logId: ${exception?.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception during log deletion")
                _state.update {
                    it.copy(
                        isDeletingLog = false,
                        error = "Ошибка удаления лога: ${e.message}"
                    )
                }

                viewModelScope.launch {
                    loggingService.logError("Исключение при удалении лога с ID $logId: ${e.message}")
                }
            }
        }
    }

    /**
     * Форматирование типа лога для отображения
     */
    private fun formatLogType(type: com.synngate.synnframe.domain.entity.LogType): String {
        return when (type) {
            com.synngate.synnframe.domain.entity.LogType.INFO -> "Информация"
            com.synngate.synnframe.domain.entity.LogType.WARNING -> "Предупреждение"
            com.synngate.synnframe.domain.entity.LogType.ERROR -> "Ошибка"
        }
    }
}