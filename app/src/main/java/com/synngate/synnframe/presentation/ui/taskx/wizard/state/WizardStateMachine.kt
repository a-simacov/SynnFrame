package com.synngate.synnframe.presentation.ui.taskx.wizard.state

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import timber.log.Timber

class WizardStateMachine {

    fun transition(state: ActionWizardState, event: WizardEvent): ActionWizardState {
        val currentStateType = determineStateType(state)

        Timber.d("WizardStateMachine: переход из состояния $currentStateType по событию $event")

        return when (currentStateType) {
            WizardState.LOADING -> handleLoadingState(state, event)
            WizardState.STEP -> handleStepState(state, event)
            WizardState.SUMMARY -> handleSummaryState(state, event)
            WizardState.SENDING -> handleSendingState(state, event)
            WizardState.SUCCESS -> handleSuccessState(state, event)
            WizardState.ERROR -> handleErrorState(state, event)
            WizardState.EXIT_DIALOG -> handleExitDialogState(state, event)
        }
    }

    private fun determineStateType(state: ActionWizardState): WizardState {
        return when {
            state.isLoading -> WizardState.LOADING
            state.showExitDialog -> WizardState.EXIT_DIALOG
            state.showSummary -> WizardState.SUMMARY
            state.error != null -> WizardState.ERROR
            else -> WizardState.STEP
        }
    }

    private fun handleLoadingState(
        state: ActionWizardState,
        event: WizardEvent
    ): ActionWizardState {
        return when (event) {
            is WizardEvent.LoadSuccess -> state.copy(isLoading = false, error = null)
            is WizardEvent.LoadFailure -> state.copy(isLoading = false, error = event.error)
            is WizardEvent.StopLoading -> state.copy(isLoading = false)
            is WizardEvent.SetError -> state.copy(error = event.error)
            else -> state
        }
    }

    private fun handleStepState(state: ActionWizardState, event: WizardEvent): ActionWizardState {
        return when (event) {
            is WizardEvent.NextStep -> {
                val currentStep = state.currentStepIndex
                val totalSteps = state.steps.size

                if (currentStep >= totalSteps - 1) {
                    state.copy(showSummary = true)
                } else {
                    state.copy(currentStepIndex = currentStep + 1)
                }
            }

            is WizardEvent.PreviousStep -> {
                if (state.currentStepIndex > 0) {
                    state.copy(currentStepIndex = state.currentStepIndex - 1)
                } else {
                    state.copy(showExitDialog = true)
                }
            }

            is WizardEvent.ShowExitDialog -> state.copy(showExitDialog = true)
            is WizardEvent.SetObject -> {
                val updatedObjects = state.selectedObjects.toMutableMap()
                updatedObjects[event.stepId] = event.obj

                val updatedFactAction = updateFactActionWithObject(
                    state.factAction,
                    state.getCurrentStep()?.factActionField,
                    event.obj
                )

                state.copy(
                    selectedObjects = updatedObjects,
                    factAction = updatedFactAction,
                    error = null
                )
            }

            is WizardEvent.SetError -> state.copy(error = event.error)
            is WizardEvent.ClearError -> state.copy(error = null)
            is WizardEvent.StartLoading -> state.copy(isLoading = true)
            is WizardEvent.StopLoading -> state.copy(isLoading = false)
            else -> state
        }
    }

    private fun handleSummaryState(
        state: ActionWizardState,
        event: WizardEvent
    ): ActionWizardState {
        return when (event) {
            is WizardEvent.PreviousStep -> state.copy(showSummary = false)
            is WizardEvent.SubmitForm -> state.copy(isLoading = true)
            is WizardEvent.ShowExitDialog -> state.copy(showExitDialog = true)
            is WizardEvent.StartLoading -> state.copy(isLoading = true)
            is WizardEvent.StopLoading -> state.copy(isLoading = false)
            is WizardEvent.ClearError -> state.copy(error = null)
            else -> state
        }
    }

    private fun handleSendingState(
        state: ActionWizardState,
        event: WizardEvent
    ): ActionWizardState {
        return when (event) {
            is WizardEvent.SendSuccess -> state.copy(isLoading = false)
            is WizardEvent.SendFailure -> {
                state.copy(
                    isLoading = false,
                    error = event.error,
                    sendingFailed = true
                )
            }

            is WizardEvent.StopLoading -> state.copy(isLoading = false)
            else -> state
        }
    }

    private fun handleSuccessState(
        state: ActionWizardState,
        event: WizardEvent
    ): ActionWizardState {
        // В большинстве случаев после успеха обычно происходит навигация
        // и состояние не меняется
        return state
    }

    private fun handleErrorState(state: ActionWizardState, event: WizardEvent): ActionWizardState {
        return when (event) {
            is WizardEvent.ClearError -> state.copy(error = null)
            is WizardEvent.RetryOperation -> state.copy(error = null, isLoading = true)
            is WizardEvent.StartLoading -> state.copy(isLoading = true)
            is WizardEvent.StopLoading -> state.copy(isLoading = false)
            is WizardEvent.SetObject -> {
                val updatedObjects = state.selectedObjects.toMutableMap()
                updatedObjects[event.stepId] = event.obj

                val updatedFactAction = updateFactActionWithObject(
                    state.factAction,
                    state.getCurrentStep()?.factActionField,
                    event.obj
                )

                state.copy(
                    selectedObjects = updatedObjects,
                    factAction = updatedFactAction,
                    error = null,
                    isLoading = false
                )
            }

            is WizardEvent.SetError -> state.copy(error = event.error)
            else -> state
        }
    }

    private fun handleExitDialogState(
        state: ActionWizardState,
        event: WizardEvent
    ): ActionWizardState {
        return when (event) {
            is WizardEvent.DismissExitDialog -> state.copy(showExitDialog = false)
            is WizardEvent.ConfirmExit -> state
            is WizardEvent.ClearError -> state.copy(error = null)
            else -> state
        }
    }

    private fun updateFactActionWithObject(
        factAction: FactAction?,
        field: FactActionField?,
        obj: Any
    ): FactAction? {
        if (factAction == null || field == null) return factAction

        return when {
            field == FactActionField.STORAGE_PRODUCT && obj is TaskProduct ->
                factAction.copy(storageProduct = obj)

            field == FactActionField.STORAGE_PRODUCT_CLASSIFIER && obj is Product ->
                factAction.copy(storageProductClassifier = obj)

            field == FactActionField.STORAGE_BIN && obj is BinX ->
                factAction.copy(storageBin = obj)

            field == FactActionField.STORAGE_PALLET && obj is Pallet ->
                factAction.copy(storagePallet = obj)

            field == FactActionField.ALLOCATION_BIN && obj is BinX ->
                factAction.copy(placementBin = obj)

            field == FactActionField.ALLOCATION_PALLET && obj is Pallet ->
                factAction.copy(placementPallet = obj)

            field == FactActionField.QUANTITY && obj is Number ->
                factAction.copy(quantity = obj.toFloat())

            else -> factAction
        }
    }
}