package com.synngate.synnframe.presentation.ui.wizard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.synngate.synnframe.domain.model.wizard.WizardContextFactory
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import com.synngate.synnframe.presentation.ui.wizard.action.ActionWizardContent
import kotlinx.coroutines.delay
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
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ActionWizardEvent.NavigateBack -> {
                    Timber.d("Выполняем navigateBack")
                    navigateBack()
                }
                is ActionWizardEvent.NavigateBackWithSuccess -> {
                    Timber.d("Выполняем navigateBackWithSuccess с actionId=${event.actionId}")
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
                onBarcodeScanned = { viewModel.processBarcodeFromScanner(it) },
                actionStepFactoryRegistry = viewModel.actionStepFactoryRegistry,
                wizardContextFactory = wizardContextFactory,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}