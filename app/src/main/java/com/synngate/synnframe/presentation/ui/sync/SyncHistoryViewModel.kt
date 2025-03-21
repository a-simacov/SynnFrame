package com.synngate.synnframe.presentation.ui.sync

import com.synngate.synnframe.data.sync.SyncHistoryRecord
import com.synngate.synnframe.domain.service.SynchronizationController
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import timber.log.Timber

/**
 * Состояние UI для экрана истории синхронизаций
 */
data class SyncHistoryState(
    val isLoading: Boolean = false,
    val syncHistory: List<SyncHistoryRecord> = emptyList(),
    val selectedRecord: SyncHistoryRecord? = null,
    val error: String? = null
)

/**
 * События для экрана истории синхронизаций
 */
sealed class SyncHistoryEvent {
    data class ShowSnackbar(val message: String) : SyncHistoryEvent()
    data object NavigateBack : SyncHistoryEvent()
}

/**
 * ViewModel для экрана истории синхронизаций
 */
class SyncHistoryViewModel(
    private val synchronizationController: SynchronizationController,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<SyncHistoryState, SyncHistoryEvent>(SyncHistoryState()) {

    init {
        loadSyncHistory()
    }

    /**
     * Загрузка истории синхронизаций
     */
    private fun loadSyncHistory() {
        launchIO {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                synchronizationController.getSyncHistory()
                    .catch { e ->
                        Timber.e(e, "Ошибка при загрузке истории синхронизаций")
                        updateState {
                            it.copy(
                                isLoading = false,
                                error = "Ошибка загрузки истории: ${e.message}"
                            )
                        }
                        sendEvent(SyncHistoryEvent.ShowSnackbar("Ошибка загрузки истории"))
                    }
                    .collect { history ->
                        updateState {
                            it.copy(
                                isLoading = false,
                                syncHistory = history,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Исключение при загрузке истории синхронизаций")
                updateState {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка: ${e.message}"
                    )
                }
                sendEvent(SyncHistoryEvent.ShowSnackbar("Ошибка загрузки истории"))
            }
        }
    }

    /**
     * Обработка клика по элементу истории
     */
    fun onHistoryItemClick(record: SyncHistoryRecord) {
        updateState { it.copy(selectedRecord = record) }
    }

    /**
     * Закрытие диалога с деталями
     */
    fun closeDetails() {
        updateState { it.copy(selectedRecord = null) }
    }

    /**
     * Обновление данных
     */
    fun refreshData() {
        loadSyncHistory()
    }

    /**
     * Навигация назад
     */
    fun navigateBack() {
        sendEvent(SyncHistoryEvent.NavigateBack)
    }
}