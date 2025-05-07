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
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.ui.wizard.action.ActionWizardContent
import timber.log.Timber

/**
 * Экран визарда действий
 *
 * @param viewModel ViewModel для экрана
 * @param navigateBack Функция навигации назад
 * @param navigateBackWithSuccess Функция навигации назад с результатом
 * @param modifier Модификатор для компонента
 */
@Composable
fun ActionWizardScreen(
    viewModel: ActionWizardViewModel,
    navigateBack: () -> Unit,
    navigateBackWithSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val wizardState by viewModel.actionWizardController.wizardState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Обработка нажатия кнопки "Назад"
    BackHandler {
        viewModel.cancelWizard()
    }

    // Получаем сервис сканера
    val scannerService = LocalScannerService.current

    // Включаем сканер при открытии экрана
    LaunchedEffect(scannerService) {
        scannerService?.let {
            if (it.hasRealScanner()) {
                if (!it.isEnabled()) {
                    Timber.d("Включение сканера для экрана визарда")
                    it.enable()
                }
            }
        }
    }

    // Обработка событий
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ActionWizardEvent.NavigateBack -> {
                    Timber.d("Навигация назад (действие отменено)")
                    navigateBack()
                }
                is ActionWizardEvent.NavigateBackWithSuccess -> {
                    Timber.d("Навигация назад с результатом: ${event.actionId}")
                    navigateBackWithSuccess(event.actionId)
                }
                is ActionWizardEvent.ShowSnackbar -> {
                    Timber.d("Показ сообщения: ${event.message}")
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
            title = "Выполнение действия",
            onNavigateBack = { viewModel.cancelWizard() },
            snackbarHostState = snackbarHostState
        ) { paddingValues ->
            // Используем существующий контент визарда, но теперь в контексте отдельного экрана
            ActionWizardContent(
                wizardState = wizardState,
                actionWizardController = viewModel.actionWizardController,
                actionWizardContextFactory = viewModel.actionWizardContextFactory,
                actionStepFactoryRegistry = viewModel.actionStepFactoryRegistry,
                onComplete = { viewModel.completeWizard() },
                onCancel = { viewModel.cancelWizard() },
                onRetryComplete = { viewModel.retryCompleteWizard() },
                onBarcodeScanned = { viewModel.processBarcodeFromScanner(it) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}