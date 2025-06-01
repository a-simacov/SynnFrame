package com.synngate.synnframe.presentation.ui.taskx.wizard.model

sealed class ActionWizardEvent {
    // Оставляем для совместимости со старым кодом
    data object NavigateBack : ActionWizardEvent()
    data object NavigateToTaskDetail : ActionWizardEvent()

    // Активно используемое событие
    data class ShowSnackbar(val message: String) : ActionWizardEvent()
}