package com.synngate.synnframe.presentation.ui.wizard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.synngate.synnframe.domain.model.wizard.WizardContextFactory
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import com.synngate.synnframe.presentation.ui.wizard.action.ActionWizardContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun ActionWizardScreen(
    viewModel: ActionWizardViewModel,
    navigateBack: () -> Unit,
    navigateBackWithSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val wizardState by viewModel.wizardStateMachine.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val wizardContextFactory = remember { WizardContextFactory() }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.onDispose()
        }
    }

    BackHandler {
        val currentState = wizardState

        when {
            currentState.currentStepIndex == 0 -> {
                viewModel.cancelWizard()
            }
            currentState.isCompleted -> {
                viewModel.goBackToPreviousStep()
            }
            else -> {
                viewModel.goBackToPreviousStep()
            }
        }
    }

    val scannerService = LocalScannerService.current

    LaunchedEffect(scannerService) {
        scannerService?.let {
            if (it.hasRealScanner()) {
                if (!it.isEnabled()) {
                    it.enable()
                }

                // Добавляем небольшую задержку и повторную инициализацию для надежности
                delay(300)
                it.disable()
                delay(100)
                it.enable()
            }
        }
    }

    // Устанавливаем обработчик сканирования с корутиной для предотвращения дублирования
    ScannerListener(onBarcodeScanned = { barcode ->
        coroutineScope.launch {
            try {
                // Добавляем задержку перед обработкой штрихкода для избежания дублирования
                delay(50)
                viewModel.processBarcodeFromScanner(barcode)
            } catch (e: Exception) {
                Timber.e(e, "ActionWizardScreen: Ошибка при обработке штрихкода")
            }
        }
    })

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ActionWizardEvent.NavigateBack -> {
                    navigateBack()
                }
                is ActionWizardEvent.NavigateBackWithSuccess -> {
                    delay(100)
                    navigateBackWithSuccess(event.actionId)
                }
                is ActionWizardEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize()
    ) {
        AppScaffold(
            showTopBar = false,
            title = wizardState?.action?.wmsAction?.let {
                getWmsActionDescription(it)
            } ?: "Выполнение действия",
            notification = wizardState?.sendError?.let {
                Pair(it, StatusType.ERROR)
            },
            onNavigateBack = { viewModel.cancelWizard() },
            snackbarHostState = snackbarHostState
        ) { paddingValues ->
            ActionWizardContent(
                wizardState = wizardState,
                onProcessStepResult = { result -> viewModel.processStepResult(result) },
                onComplete = {
                    viewModel.completeWizard()
                },
                onCancel = { viewModel.cancelWizard() },
                onRetryComplete = { viewModel.retryCompleteWizard() },
                onBarcodeScanned = { barcode ->
                    viewModel.processBarcodeFromScanner(barcode)
                },
                actionStepFactoryRegistry = viewModel.actionStepFactoryRegistry,
                wizardContextFactory = wizardContextFactory,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}