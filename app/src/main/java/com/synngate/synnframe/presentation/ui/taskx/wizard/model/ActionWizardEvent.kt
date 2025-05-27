package com.synngate.synnframe.presentation.ui.taskx.wizard.model

sealed class ActionWizardEvent {
    data object NavigateBack : ActionWizardEvent()
    data object NavigateToTaskDetail : ActionWizardEvent()
    data class ShowSnackbar(val message: String) : ActionWizardEvent()
}