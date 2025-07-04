package com.synngate.synnframe.presentation.ui.taskx.model

/**
 * События экрана детального просмотра задания
 */
sealed class TaskXDetailEvent {
    // Навигация
    data object NavigateBack : TaskXDetailEvent()
    data class NavigateToActionWizard(val taskId: String, val actionId: String) : TaskXDetailEvent()

    // Уведомления
    data class ShowSnackbar(val message: String) : TaskXDetailEvent()

    // Комбинированное событие для навигации с уведомлением
    data class NavigateBackWithMessage(val message: String) : TaskXDetailEvent()
    
    // Событие для показа диалога с пользовательским сообщением
    data class ShowUserMessageDialog(val message: String, val isSuccess: Boolean) : TaskXDetailEvent()
}