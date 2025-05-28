package com.synngate.synnframe.presentation.ui.taskx.wizard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.components.ExitConfirmationDialog
import com.synngate.synnframe.presentation.ui.taskx.wizard.components.StepScreen
import com.synngate.synnframe.presentation.ui.taskx.wizard.components.SummaryScreen
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardEvent
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState

@Composable
fun ActionWizardScreen(
    viewModel: ActionWizardViewModel,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Получаем текущий сервис сканера для поддержки аппаратного сканера
    val scannerService = LocalScannerService.current

    // Подключаем слушатель сканера, если он доступен
    if (scannerService?.hasRealScanner() == true) {
        val currentStep = state.steps.getOrNull(state.currentStepIndex)

        ScannerListener(onBarcodeScanned = { barcode ->
            // Получаем тип поля текущего шага
            currentStep?.factActionField?.let { fieldType ->
                // Только для поддерживаемых типов полей обрабатываем сканирование
                when (fieldType) {
                    FactActionField.STORAGE_PRODUCT,
                    FactActionField.STORAGE_PRODUCT_CLASSIFIER,
                    FactActionField.STORAGE_BIN,
                    FactActionField.ALLOCATION_BIN,
                    FactActionField.STORAGE_PALLET,
                    FactActionField.ALLOCATION_PALLET -> {
                        viewModel.searchObjectByBarcode(barcode, fieldType)
                    }
                    else -> {} // Остальные типы полей не обрабатываем
                }
            }
        })
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ActionWizardEvent.NavigateBack -> navigateBack()
                is ActionWizardEvent.NavigateToTaskDetail -> navigateBack()
                is ActionWizardEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }
            }
        }
    }

    if (state.showExitDialog) {
        ExitConfirmationDialog(
            onDismiss = { viewModel.dismissExitDialog() },
            onConfirm = { viewModel.exitWizard() }
        )
    }

    AppScaffold(
        title = if (state.showSummary) "Итог" else getStepTitle(state),
        subtitle = if (!state.showSummary) "Шаг ${state.currentStepIndex + 1} из ${state.steps.size}" else null,
        onNavigateBack = { viewModel.previousStep() },
        actions = {
            IconButton(onClick = { viewModel.showExitDialog() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Выйти"
                )
            }
        },
        snackbarHostState = snackbarHostState,
        notification = state.error?.let { Pair(it, StatusType.ERROR) },
        onDismissNotification = { viewModel.clearError() },
        isLoading = state.isLoading
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (!state.showSummary && state.steps.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { (state.currentStepIndex + 1).toFloat() / state.steps.size },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.showSummary) {
                SummaryScreen(
                    state = state,
                    onComplete = { viewModel.completeAction() },
                    onBack = { viewModel.previousStep() }
                )
            } else if (state.steps.isNotEmpty()) {
                StepScreen(
                    state = state,
                    onConfirm = { viewModel.confirmCurrentStep() },
                    onObjectSelected = { viewModel.setObjectForCurrentStep(it) },
                    onBarcodeSearch = { barcode ->
                        state.getCurrentStep()?.let { currentStep ->
                            viewModel.searchObjectByBarcode(barcode, currentStep.factActionField)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun getStepTitle(state: ActionWizardState): String {
    val currentStep = state.steps.getOrNull(state.currentStepIndex)
    return currentStep?.name ?: "Действие"
}