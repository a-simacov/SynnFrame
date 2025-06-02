package com.synngate.synnframe.presentation.ui.taskx.wizard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.synngate.synnframe.presentation.ui.taskx.wizard.components.ExitConfirmationDialog
import com.synngate.synnframe.presentation.ui.taskx.wizard.components.StepScreen
import com.synngate.synnframe.presentation.ui.taskx.wizard.components.SummaryScreen
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardEvent
import timber.log.Timber
import java.util.UUID

@Composable
fun ActionWizardScreen(
    viewModel: ActionWizardViewModel,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleToken = remember { UUID.randomUUID().toString() }
    DisposableEffect(lifecycleToken) {
        Timber.d("ActionWizardScreen с токеном $lifecycleToken создан")
        onDispose {
            Timber.d("ActionWizardScreen с токеном $lifecycleToken уничтожается")
        }
    }

    LaunchedEffect(viewModel, lifecycleToken) {
        Timber.d("ActionWizardScreen: установка callback навигации для токена $lifecycleToken")
        viewModel.setNavigateBackCallback {
            Timber.d("ActionWizardScreen: вызов navigateBack() для токена $lifecycleToken")
            navigateBack()
        }
    }

    BackHandler {
        when {
            state.showSummary && state.sendingFailed -> {
                // При ошибке отправки сразу выходим из визарда
                viewModel.exitWizard()
            }
            state.isLoading && state.sendingFailed -> {
                // Если произошла ошибка при загрузке, но флаг isLoading не сбросился
                viewModel.clearErrorAndLoading()
            }
            state.showExitDialog -> {
                // Закрываем диалог выхода
                viewModel.dismissExitDialog()
            }
            else -> {
                // Обычный возврат назад
                viewModel.previousStep()
            }
        }
    }

    // Используем стандартный ScannerListener вместо WizardScannerListener
    val scannerService = LocalScannerService.current
    if (scannerService?.hasRealScanner() == true) {
        ScannerListener(onBarcodeScanned = { barcode ->
            val currentStep = state.getCurrentStep()
            Timber.d("Сканирование штрихкода: $barcode на шаге с типом: ${currentStep?.factActionField}")
            viewModel.handleBarcode(barcode)
        })
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ActionWizardEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }
                // Убираем обработку событий навигации, так как теперь используем callback
                else -> { /* ignore */ }
            }
        }
    }

    DisposableEffect(Unit) {
        Timber.d("ActionWizardScreen: создан")
        onDispose {
            Timber.d("ActionWizardScreen: уничтожен")
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { (state.currentStepIndex + 1).toFloat() / state.steps.size },
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "(${state.currentStepIndex + 1}/${state.steps.size})",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (state.showSummary) {
                SummaryScreen(
                    state = state,
                    onComplete = { viewModel.completeAction() },
                    onBack = { viewModel.previousStep() }
                )
            } else if (state.steps.isNotEmpty()) {
                StepScreen(
                    state = state,
                    onConfirm = { viewModel.confirmCurrentStep() },
                    onObjectSelected = { obj, autoAdvance ->
                        viewModel.setObjectForCurrentStep(obj, autoAdvance)
                    },
                    handleBarcode = { barcode -> viewModel.handleBarcode(barcode) }
                )
            }
        }
    }
}

@Composable
private fun getStepTitle(state: com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState): String {
    val currentStep = state.steps.getOrNull(state.currentStepIndex)
    return currentStep?.name ?: "Действие"
}