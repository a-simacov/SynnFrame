package com.synngate.synnframe.presentation.ui.taskx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.synngate.synnframe.domain.model.wizard.StepResult
import com.synngate.synnframe.presentation.ui.wizard.WizardScreen
import timber.log.Timber

@Composable
fun FactLineWizard(
    viewModel: TaskXDetailViewModel,
    modifier: Modifier = Modifier
) {
    val wizardState by viewModel.wizardController.wizardState.collectAsState()

    wizardState?.let { state ->
        WizardScreen(
            state = state,
            onStepComplete = { result ->
                try {
                    // Проверяем, является ли это нажатием кнопки "Назад" из итогового экрана
                    if (result == null && state.isCompleted) {
                        Timber.d("FactLineWizard: Detected back from summary")
                        viewModel.returnFromSummaryScreen()
                    } else {
                        // Стандартная обработка
                        viewModel.processWizardStep(result)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Processing wizard step result error: ${e.message}")
                }
            },
            onStepSkip = { result ->
                try {
                    viewModel.processWizardStep(StepResult.Skip(result))
                } catch (e: Exception) {
                    Timber.e(e, "Processing wizard skip error: ${e.message}")
                }
            },
            onComplete = { viewModel.completeWizard() },
            onCancel = { viewModel.cancelWizard() },
            modifier = modifier
        )
    }
}