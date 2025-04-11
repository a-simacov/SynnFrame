package com.synngate.synnframe.presentation.ui.taskx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    // Получаем состояние мастера из контроллера
    val wizardState by viewModel.wizardController.wizardState.collectAsState()

    // Дополнительное логирование состояния
    LaunchedEffect(wizardState) {
        if (wizardState == null) {
            Timber.d("Wizard state: null")
        } else {
            Timber.d("Wizard state: step ${wizardState!!.currentStepIndex + 1}/${wizardState!!.steps.size}, " +
                    "current step: ${wizardState!!.currentStep?.title ?: "null"}")
        }
    }

    // Отображаем экран визарда
    wizardState?.let { state ->
        WizardScreen(
            state = state,
            onStepComplete = { result -> viewModel.processWizardStep(result) },
            onComplete = { viewModel.completeWizard() },
            onCancel = { viewModel.cancelWizard() },
            modifier = modifier
        )
    }
}