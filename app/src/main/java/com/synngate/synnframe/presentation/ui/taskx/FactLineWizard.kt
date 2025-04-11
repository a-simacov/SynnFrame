package com.synngate.synnframe.presentation.ui.taskx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel
import com.synngate.synnframe.presentation.ui.wizard.WizardScreen

/**
 * Компонент визарда для добавления строки факта
 */
@Composable
fun FactLineWizard(
    viewModel: TaskXDetailViewModel,
    wizardViewModel: FactLineWizardViewModel,
    modifier: Modifier = Modifier
) {
    // Получаем состояние мастера из контроллера
    val wizardState by viewModel.wizardController.wizardState.collectAsState()

    // Отображаем экран визарда
    WizardScreen(
        state = wizardState,
        onStepComplete = { result -> viewModel.processWizardStep(result) },
        onComplete = { viewModel.completeWizard() },
        onCancel = { viewModel.cancelWizard() },
        modifier = modifier
    )
}