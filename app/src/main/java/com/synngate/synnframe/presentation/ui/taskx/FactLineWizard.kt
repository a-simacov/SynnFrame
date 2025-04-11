package com.synngate.synnframe.presentation.ui.taskx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel
import com.synngate.synnframe.presentation.ui.wizard.WizardScreen
import timber.log.Timber

@Composable
fun FactLineWizard(
    viewModel: TaskXDetailViewModel,
    wizardViewModel: FactLineWizardViewModel,
    modifier: Modifier = Modifier
) {
    val wizardState by viewModel.wizardController.wizardState.collectAsState()

    wizardState?.let { state ->
        WizardScreen(
            state = state,
            onStepComplete = { result ->
                try {
                    viewModel.processWizardStep(result)
                } catch (e: Exception) {
                    Timber.e(e, "Processing wizard step result error: ${e.message}")
                }
            },
            onComplete = { viewModel.completeWizard() },
            onCancel = { viewModel.cancelWizard() },
            modifier = modifier
        )
    }
}