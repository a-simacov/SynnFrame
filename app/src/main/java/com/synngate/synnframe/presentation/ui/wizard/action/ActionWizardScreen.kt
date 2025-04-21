package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.data.barcodescanner.ScannerService
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionWizardState
import com.synngate.synnframe.domain.service.ActionWizardContextFactory
import com.synngate.synnframe.domain.service.ActionWizardController
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.ScannerStatusIndicator
import com.synngate.synnframe.presentation.ui.taskx.components.WmsActionIconWithTooltip
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun ActionWizardScreen(
    actionWizardController: ActionWizardController,
    actionWizardContextFactory: ActionWizardContextFactory,
    actionStepFactoryRegistry: ActionStepFactoryRegistry,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by actionWizardController.wizardState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    if (state == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Ошибка: Визард не инициализирован",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    // Получаем сервис сканера
    val scannerService = LocalScannerService.current

    // Проверка и включение сканера при открытии визарда
    LaunchedEffect(scannerService) {
        scannerService?.let {
            // Проверяем, есть ли реальный сканер перед его активацией
            if (it.hasRealScanner()) {
                if (!it.isEnabled()) {
                    it.enable()
                    Timber.d("ActionWizardScreen: Сканер был отключен, выполняем принудительное включение")
                } else {
                    Timber.d("ActionWizardScreen: Сканер уже включен")
                }
            } else {
                Timber.d("ActionWizardScreen: Пропуск включения сканера (пустой сканер)")
            }
        }
    }

    // Переменная для отслеживания обработки штрихкода
    var isProcessingGlobalBarcode by remember { mutableStateOf(false) }

    LaunchedEffect(state?.currentStepIndex) {
        isProcessingGlobalBarcode = false
        Timber.d("ActionWizardScreen: Сброс флага isProcessingGlobalBarcode при смене шага")
    }

    // Глобальный слушатель сканера нужен только для итогового экрана
    // Для остальных шагов будем использовать локальные слушатели
    if (state?.isCompleted == true && scannerService?.hasRealScanner() == true) {
        ScannerListener(onBarcodeScanned = { barcode ->
            if (!isProcessingGlobalBarcode) {
                isProcessingGlobalBarcode = true
                Timber.d("Штрихкод от встроенного сканера (глобальный): $barcode")
                coroutineScope.launch {
                    actionWizardController.processBarcodeFromScanner(barcode)
                    isProcessingGlobalBarcode = false
                }
            }
        })
    }

    val wizardState = state!!

    BackHandler {
        if (wizardState.isCompleted || wizardState.currentStepIndex > 0) {
            coroutineScope.launch {
                actionWizardController.processStepResult(null)
            }
        } else {
            onCancel()
        }
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            WizardHeader(wizardState, scannerService, onCancel)

            LinearProgressIndicator(
                progress = { wizardState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                val currentStep = wizardState.currentStep
                val action = wizardState.action

                if (currentStep == null || wizardState.isCompleted) {
                    ActionSummaryScreen(
                        state = wizardState
                    )
                } else if (action != null) {
                    val actionStep = findActionStepForWizardStep(action, currentStep.id)

                    if (actionStep != null) {
                        val factory = actionStepFactoryRegistry.getFactory(actionStep.objectType)

                        if (factory != null) {
                            val context = actionWizardContextFactory.createContext(
                                state = wizardState,
                                onStepComplete = { result ->
                                    coroutineScope.launch {
                                        actionWizardController.processStepResult(result)
                                    }
                                },
                                onBack = {
                                    coroutineScope.launch {
                                        actionWizardController.processStepResult(null)
                                    }
                                },
                                onForward = {
                                    coroutineScope.launch {
                                        actionWizardController.processForwardStep()
                                    }
                                },
                                onSkip = { },
                                onCancel = onCancel,
                                lastScannedBarcode = wizardState.lastScannedBarcode
                            )

                            factory.createComponent(
                                step = actionStep,
                                action = action,
                                context = context
                            )
                        } else {
                            Text(
                                text = "Ошибка: Нет компонента для типа объекта: ${actionStep.objectType}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Text(
                            text = "Ошибка: Шаг не найден: ${currentStep.id}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Text(
                        text = "Ошибка: Действие не найдено",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            WizardActions(wizardState, coroutineScope, actionWizardController, onComplete)

            Spacer(modifier = Modifier.weight(0.1f))
        }
    }
}

@Composable
private fun WizardActions(
    wizardState: ActionWizardState,
    coroutineScope: CoroutineScope,
    actionWizardController: ActionWizardController,
    onComplete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if ((wizardState.canGoBack && wizardState.currentStepIndex > 0) || wizardState.isCompleted) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        actionWizardController.processStepResult(null)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Назад"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Назад")
            }
        } else {
            // Пустой Spacer для сохранения выравнивания, если кнопка "Назад" отсутствует
            Spacer(modifier = Modifier.weight(1f))
        }

        if (wizardState.isCompleted) {
            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f)
            ) {
                Text("Завершить")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Завершить"
                )
            }
        }
    }
}

@Composable
private fun WizardHeader(
    wizardState: ActionWizardState,
    scannerService: ScannerService?,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = wizardState.action?.wmsAction?.name ?: "Выполнение действия",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )

        scannerService?.let { scanner ->
            ScannerStatusIndicator(
                scannerService = scanner,
                showText = false,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        IconButton(onClick = onCancel) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Закрыть"
            )
        }
    }
}

private fun findActionStepForWizardStep(
    action: PlannedAction,
    stepId: String
): ActionStep? {
    action.actionTemplate.storageSteps.find { it.id == stepId }?.let {
        return it
    }

    action.actionTemplate.placementSteps.find { it.id == stepId }?.let {
        return it
    }

    return null
}

@Composable
fun ActionSummaryScreen(
    state: ActionWizardState,
    modifier: Modifier = Modifier
) {
    val action = state.action

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Проверьте информацию перед завершением",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (action != null) {
            Text(
                text = "Действие: ${action.actionTemplate.name}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                WmsActionIconWithTooltip(
                    wmsAction = action.wmsAction,
                    iconSize = 24
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Тип действия: ${getWmsActionDescription(action.wmsAction)}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            for ((stepId, value) in state.results.entries) {
                when (value) {
                    is TaskProduct -> {
                        Text(
                            text = "Товар: ${value.product.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Количество: ${value.quantity}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    is Pallet -> {
                        Text(
                            text = "Паллета: ${value.code}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Статус: ${if (value.isClosed) "Закрыта" else "Открыта"}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    is BinX -> {
                        Text(
                            text = "Ячейка: ${value.code}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Зона: ${value.zone}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    is Product -> {
                        Text(
                            text = "Товар: ${value.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Артикул: ${value.articleNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }
}