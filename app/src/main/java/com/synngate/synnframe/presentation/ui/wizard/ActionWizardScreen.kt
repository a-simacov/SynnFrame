// Заменяет com.synngate.synnframe.presentation.ui.wizard.ActionWizardScreen
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
import timber.log.Timber

/**
 * Упрощенный экран визарда действий
 */
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

    // ИСПРАВЛЕНО: Добавлено логирование при первой загрузке экрана
    LaunchedEffect(Unit) {
        Timber.d("ActionWizardScreen: Инициализация")// с taskId=${viewModel.taskId}, actionId=${viewModel.actionId}")
    }

    BackHandler {
        val currentState = wizardState

        if (currentState != null) {
            when {
                currentState.currentStepIndex == 0 -> {
                    Timber.d("BackHandler: отмена визарда (первый шаг)")
                    viewModel.cancelWizard()
                }
                currentState.isCompleted -> {
                    Timber.d("BackHandler: возврат к предыдущему шагу (из итогового экрана)")
                    viewModel.goBackToPreviousStep()
                }
                else -> {
                    Timber.d("BackHandler: возврат к предыдущему шагу (текущий индекс: ${currentState.currentStepIndex})")
                    viewModel.goBackToPreviousStep()
                }
            }
        } else {
            Timber.d("BackHandler: отмена визарда (нет состояния)")
            viewModel.cancelWizard()
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
        Timber.d("Начинаем сбор событий от ActionWizardViewModel")
        viewModel.events.collect { event ->
            Timber.d("Получено событие: $event")
            when (event) {
                is ActionWizardEvent.NavigateBack -> {
                    Timber.d("Выполняем navigateBack")
                    navigateBack()
                }
                is ActionWizardEvent.NavigateBackWithSuccess -> {
                    Timber.d("Выполняем navigateBackWithSuccess с actionId=${event.actionId}")
                    navigateBackWithSuccess(event.actionId)
                }
                is ActionWizardEvent.ShowSnackbar -> {
                    Timber.d("Показываем Snackbar: ${event.message}")
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
            onNavigateBack = {
                Timber.d("AppScaffold: вызов onNavigateBack")
                viewModel.cancelWizard()
            },
            snackbarHostState = snackbarHostState
        ) { paddingValues ->
            ActionWizardContent(
                wizardState = wizardState,
                onProcessStepResult = { result ->
                    Timber.d("ActionWizardContent: вызов onProcessStepResult с результатом: ${result?.javaClass?.simpleName}")
                    viewModel.processStepResult(result)
                },
                onComplete = {
                    Timber.d("ActionWizardContent: вызов onComplete")
                    viewModel.completeWizard()
                },
                onCancel = {
                    Timber.d("ActionWizardContent: вызов onCancel")
                    viewModel.cancelWizard()
                },
                onRetryComplete = {
                    Timber.d("ActionWizardContent: вызов onRetryComplete")
                    viewModel.retryCompleteWizard()
                },
                onBarcodeScanned = {
                    viewModel.processBarcodeFromScanner(it)
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