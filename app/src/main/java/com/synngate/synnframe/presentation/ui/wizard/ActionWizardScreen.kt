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

/**
 * Экран визарда действий.
 * Обеспечивает интерфейс для выполнения шагов действия и навигацию между ними.
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
    val coroutineScope = rememberCoroutineScope()

    // Используем DisposableEffect для гарантированного освобождения ресурсов при выходе из экрана
    DisposableEffect(viewModel) {
        Timber.d("ActionWizardScreen: создан (taskId=${viewModel.taskId}, actionId=${viewModel.actionId})")

        onDispose {
            Timber.d("ActionWizardScreen: освобождение ресурсов")
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

    // Управление сканером штрихкодов
    val scannerService = LocalScannerService.current

    // Добавляем логику повторной инициализации сканера с задержкой
    // для уверенности, что сканер корректно настроится на новом экране
    LaunchedEffect(scannerService) {
        Timber.d("ActionWizardScreen: Инициализация сканера")

        scannerService?.let {
            if (it.hasRealScanner()) {
                if (!it.isEnabled()) {
                    it.enable()
                    Timber.d("ActionWizardScreen: Сканер включен")
                } else {
                    Timber.d("ActionWizardScreen: Сканер уже был включен")
                }

                // Добавляем небольшую задержку и повторную инициализацию для надежности
                delay(300)
                it.disable()
                delay(100)
                it.enable()
                Timber.d("ActionWizardScreen: Сканер переинициализирован")
            } else {
                Timber.d("ActionWizardScreen: Реальный сканер не обнаружен")
            }
        }
    }

    // Устанавливаем обработчик сканирования с корутиной для предотвращения дублирования
    ScannerListener(onBarcodeScanned = { barcode ->
        Timber.d("ActionWizardScreen: Получен штрихкод: $barcode")
        coroutineScope.launch {
            try {
                // Добавляем задержку перед обработкой штрихкода для избежания дублирования
                delay(50)
                viewModel.processBarcodeFromScanner(barcode)
                Timber.d("ActionWizardScreen: Штрихкод отправлен в визард: $barcode")
            } catch (e: Exception) {
                Timber.e(e, "ActionWizardScreen: Ошибка при обработке штрихкода")
            }
        }
    })

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
                onBarcodeScanned = { barcode ->
                    Timber.d("ActionWizardContent: Штрихкод передан из контента: $barcode")
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