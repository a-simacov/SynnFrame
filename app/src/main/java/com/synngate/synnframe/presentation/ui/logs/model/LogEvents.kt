package com.synngate.synnframe.presentation.ui.logs.model

/**
 * События для экрана списка логов
 */
sealed class LogListEvent {
    /**
     * Показать сообщение в SnackBar
     */
    data class ShowSnackbar(val message: String) : LogListEvent()

    /**
     * Навигация к экрану детальной информации лога
     */
    data class NavigateToLogDetail(val logId: Int) : LogListEvent()

    /**
     * Показать диалог подтверждения удаления всех логов
     */
    data object ShowDeleteAllConfirmation : LogListEvent()
}

/**
 * События для экрана деталей лога
 */
sealed class LogDetailEvent {
    /**
     * Показать сообщение в SnackBar
     */
    data class ShowSnackbar(val message: String) : LogDetailEvent()

    /**
     * Навигация назад после удаления лога
     */
    data object NavigateBack : LogDetailEvent()

    /**
     * Показать диалог подтверждения удаления лога
     */
    data object ShowDeleteConfirmation : LogDetailEvent()

    /**
     * Скрыть диалог подтверждения удаления лога
     */
    data object HideDeleteConfirmation : LogDetailEvent()
}