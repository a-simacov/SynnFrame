package com.synngate.synnframe.domain.model.wizard

interface WizardState {

    val id: String

    val isTerminal: Boolean

    fun handleEvent(event: WizardEvent): WizardState?
}

sealed class WizardEvent {

    data class Next(val result: Any) : WizardEvent()

    object Back : WizardEvent()

    object Cancel : WizardEvent()

    object Complete : WizardEvent()

    data class ProcessBarcode(val barcode: String) : WizardEvent()

    data class Initialize(val taskId: String, val actionId: String) : WizardEvent()
}