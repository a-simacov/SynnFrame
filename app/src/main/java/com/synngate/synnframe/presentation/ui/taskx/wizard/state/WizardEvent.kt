package com.synngate.synnframe.presentation.ui.taskx.wizard.state

sealed class WizardEvent {

    object LoadSuccess : WizardEvent()
    data class LoadFailure(val error: String) : WizardEvent()

    object NextStep : WizardEvent()
    object PreviousStep : WizardEvent()

    object ShowExitDialog : WizardEvent()
    object DismissExitDialog : WizardEvent()
    object ConfirmExit : WizardEvent()

    data class SetObject(val obj: Any, val stepId: String) : WizardEvent()

    // Новые события для работы с буфером
    data class SetObjectFromBuffer(
        val obj: Any,
        val stepId: String,
        val source: String,
        val isLocked: Boolean
    ) : WizardEvent()

    object AutoAdvanceFromBuffer : WizardEvent()

    data class ClearFieldInBuffer(val field: com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField) : WizardEvent()

    data class SetError(val error: String) : WizardEvent()
    object ClearError : WizardEvent()

    object SubmitForm : WizardEvent()
    object SendSuccess : WizardEvent()
    data class SendFailure(val error: String) : WizardEvent()

    object StartLoading : WizardEvent()
    object StopLoading : WizardEvent()
    object RetryOperation : WizardEvent()
}