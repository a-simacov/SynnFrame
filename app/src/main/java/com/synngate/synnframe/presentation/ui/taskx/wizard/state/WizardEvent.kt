package com.synngate.synnframe.presentation.ui.taskx.wizard.state

/**
 * События, которые обрабатывает машина состояний визарда
 */
sealed class WizardEvent {
    // События загрузки
    object LoadSuccess : WizardEvent()
    data class LoadFailure(val error: String) : WizardEvent()

    // События навигации
    object NextStep : WizardEvent()
    object PreviousStep : WizardEvent()

    // События диалога выхода
    object ShowExitDialog : WizardEvent()
    object DismissExitDialog : WizardEvent()
    object ConfirmExit : WizardEvent()

    // События установки объектов
    data class SetObject(val obj: Any, val stepId: String) : WizardEvent()

    // События ошибок
    data class SetError(val error: String) : WizardEvent()
    object ClearError : WizardEvent()

    // События отправки формы
    object SubmitForm : WizardEvent()
    object SendSuccess : WizardEvent()
    data class SendFailure(val error: String) : WizardEvent()

    // Прочие события
    object StartLoading : WizardEvent()
    object StopLoading : WizardEvent()
    object RetryOperation : WizardEvent()
}