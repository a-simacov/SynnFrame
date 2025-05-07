package com.synngate.synnframe.presentation.ui.wizard

/**
 * События экрана визарда действий
 */
sealed class ActionWizardEvent {
    /**
     * Возврат назад (действие отменено)
     */
    object NavigateBack : ActionWizardEvent()

    /**
     * Возврат назад с успешным результатом
     * @param actionId ID действия, которое было успешно выполнено
     */
    data class NavigateBackWithSuccess(val actionId: String) : ActionWizardEvent()

    /**
     * Показать сообщение пользователю
     */
    data class ShowSnackbar(val message: String) : ActionWizardEvent()
}