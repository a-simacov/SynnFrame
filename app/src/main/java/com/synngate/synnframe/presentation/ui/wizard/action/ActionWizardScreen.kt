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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
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
    onComplete: (Boolean) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by actionWizardController.wizardState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Локальное состояние для выбора между полным и частичным выполнением
    var showCompletionOptions by remember { mutableStateOf(false) }

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
            if (it.hasRealScanner()) {
                if (!it.isEnabled()) {
                    it.enable()
                }
            }
        }
    }

    var isProcessingGlobalBarcode by remember { mutableStateOf(false) }

    LaunchedEffect(state?.currentStepIndex) {
        isProcessingGlobalBarcode = false
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
                        state = wizardState,
                        // Отображаем кнопки выбора режима завершения, если это возможно
                        showCompletionOptions = showCompletionOptions && wizardState.canPartialExecution(),
                        onContinuePartial = {
                            showCompletionOptions = false
                            coroutineScope.launch {
                                val result = actionWizardController.complete(false)
                                if (result.isSuccess) {
                                    onComplete(false)
                                }
                            }
                        },
                        onCompleteFull = {
                            showCompletionOptions = false
                            coroutineScope.launch {
                                val result = actionWizardController.complete(true)
                                if (result.isSuccess) {
                                    onComplete(true)
                                }
                            }
                        }
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

            // Выводим разные кнопки в зависимости от состояния визарда
            if (wizardState.isCompleted) {
                CompletionButtons(
                    wizardState = wizardState,
                    coroutineScope = coroutineScope,
                    actionWizardController = actionWizardController,
                    onComplete = onComplete, // Теперь он принимает Boolean
                    onShowOptions = { showCompletionOptions = true }
                )
            } else {
                NavigationButtons(
                    wizardState = wizardState,
                    coroutineScope = coroutineScope,
                    actionWizardController = actionWizardController
                )
            }

            Spacer(modifier = Modifier.weight(0.1f))
        }
    }
}

@Composable
private fun CompletionButtons(
    wizardState: ActionWizardState,
    coroutineScope: CoroutineScope,
    actionWizardController: ActionWizardController,
    onComplete: (Boolean) -> Unit,
    onShowOptions: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (wizardState.canGoBack) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        actionWizardController.processStepResult(null)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !wizardState.isSending
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Назад")
            }
        } else {
            // Пустой Spacer для сохранения выравнивания, если кнопка "Назад" отсутствует
            Spacer(modifier = Modifier.weight(1f))
        }

        // Проверяем, можно ли выполнить действие частично
        if (wizardState.canPartialExecution()) {
            Button(
                onClick = onShowOptions,
                modifier = Modifier.weight(1f),
                enabled = !wizardState.isSending
            ) {
                if (wizardState.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Отправка...")
                } else {
                    Text("Завершить")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Завершить"
                    )
                }
            }
        } else {
            // Стандартная кнопка Завершить, если нельзя выполнить частично
            Button(
                onClick = {
                    coroutineScope.launch {
                        val result = actionWizardController.complete(true)
                        if (result.isSuccess) {
                            onComplete(true) // Передаем true - полное завершение
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !wizardState.isSending
            ) {
                if (wizardState.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Отправка...")
                } else {
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
}

@Composable
private fun NavigationButtons(
    wizardState: ActionWizardState,
    coroutineScope: CoroutineScope,
    actionWizardController: ActionWizardController
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (wizardState.canGoBack && wizardState.currentStepIndex > 0) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        actionWizardController.processStepResult(null)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !wizardState.isProcessingStep
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Назад")
            }
        } else {
            // Пустой Spacer для сохранения выравнивания, если кнопка "Назад" отсутствует
            Spacer(modifier = Modifier.weight(1f))
        }

        if (!wizardState.isCompleted && wizardState.currentStep != null &&
            wizardState.hasResultForStep(wizardState.currentStep!!.id)) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        actionWizardController.processForwardStep()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !wizardState.isProcessingStep
            ) {
                Text("Вперед")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Вперед"
                )
            }
        } else {
            // Пустой Spacer для сохранения выравнивания
            Spacer(modifier = Modifier.weight(1f))
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
        // Используем getWmsActionDescription вместо name для строкового представления действия
        Text(
            text = wizardState.action?.wmsAction?.let {
                getWmsActionDescription(it)
            } ?: "Выполнение действия",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )

        // Отображаем индикатор для частично выполненного действия
        if (wizardState.isPartiallyCompleted) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Text(
                    text = "Частичное выполнение",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

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
    modifier: Modifier = Modifier,
    showCompletionOptions: Boolean = false,
    onContinuePartial: () -> Unit = {},
    onCompleteFull: () -> Unit = {}
) {
    val action = state.action

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Проверьте информацию перед завершением",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (state.isSending) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Отправка данных на сервер...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (state.sendError != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Ошибка",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = state.sendError,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Отображение текущих связанных фактических действий, если это частичное выполнение
        if (state.isPartiallyCompleted && state.relatedFactActions.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Уже выполнено:",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    state.relatedFactActions.forEach { factAction ->
                        val quantity = factAction.storageProduct?.quantity ?: 0f
                        Text(
                            text = "✓ ${factAction.storageProduct?.product?.name ?: "Товар"}: $quantity шт",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Показываем общее выполненное количество
                    val totalCompleted = state.relatedFactActions.sumOf {
                        (it.storageProduct?.quantity ?: 0f).toDouble()
                    }.toFloat()

                    val planned = action?.plannedQuantity ?: 0f

                    if (planned > 0f) {
                        Text(
                            text = "Всего выполнено: $totalCompleted из $planned (${(totalCompleted/planned*100).toInt()}%)",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }

        // Отображение опций для завершения частичного действия
        if (showCompletionOptions) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Выберите способ завершения:",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onContinuePartial,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Добавить еще",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Добавить еще действие")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onCompleteFull,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Завершить",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Завершить и перейти дальше")
                    }
                }
            }
        }

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