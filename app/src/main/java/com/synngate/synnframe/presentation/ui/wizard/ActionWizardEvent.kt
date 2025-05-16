package com.synngate.synnframe.presentation.ui.wizard

sealed class ActionWizardEvent {

    object NavigateBack : ActionWizardEvent()

    data class NavigateBackWithSuccess(val actionId: String) : ActionWizardEvent()

    data class ShowSnackbar(val message: String) : ActionWizardEvent()
}