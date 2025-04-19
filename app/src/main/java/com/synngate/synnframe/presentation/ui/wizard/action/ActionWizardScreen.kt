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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Экран визарда действий
 */
@Composable
fun ActionWizardScreen(
    actionWizardController: ActionWizardController,
    actionWizardContextFactory: ActionWizardContextFactory,
    actionStepFactoryRegistry: ActionStepFactoryRegistry,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Получаем текущее состояние визарда
    val state by actionWizardController.wizardState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Timber.d("ActionWizardScreen: state=$state")

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

    // Получаем сервис сканера из CompositionLocal
    val scannerService = LocalScannerService.current

    // Слушатель штрихкодов от встроенного сканера
    ScannerListener(onBarcodeScanned = { barcode ->
        Timber.d("Штрихкод от встроенного сканера: $barcode")
        coroutineScope.launch {
            actionWizardController.processBarcodeFromScanner(barcode)
        }
    })

    val wizardState = state!!
    Timber.d("ActionWizardScreen: currentStep=${wizardState.currentStep}, action=${wizardState.action != null}")

    // Обработка системной кнопки "Назад"
    BackHandler {
        if (wizardState.isCompleted || wizardState.currentStepIndex > 0) {
            // На итоговом экране или не на первом шаге - возврат к предыдущему шагу
            coroutineScope.launch {
                actionWizardController.processStepResult(null)
            }
        } else {
            // На первом шаге - закрытие мастера
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
            // Заголовок и кнопка закрытия
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = wizardState.action?.actionTemplate?.name ?: "Выполнение действия",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )

                // Добавляем индикатор статуса сканера
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

            // Прогресс
            LinearProgressIndicator(
                progress = { wizardState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )

            // Содержимое текущего шага
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
                    // Итоговый экран
                    ActionSummaryScreen(
                        state = wizardState
                    )
                } else if (action != null) {
                    // Находим ActionStep для текущего шага визарда
                    val actionStep = findActionStepForWizardStep(action, currentStep.id)

                    Timber.d("Step: ${currentStep.id}, found actionStep: ${actionStep?.id}")

                    if (actionStep != null) {
                        // Находим фабрику для типа объекта
                        val factory = actionStepFactoryRegistry.getFactory(actionStep.objectType)

                        Timber.d("Object type: ${actionStep.objectType}, factory: ${factory != null}")

                        if (factory != null) {
                            // Создаем контекст для шага
                            val context = actionWizardContextFactory.createContext(
                                state = wizardState,
                                onStepComplete = { result ->
                                    Timber.d("Step completed with result: $result")
                                    coroutineScope.launch {
                                        actionWizardController.processStepResult(result)
                                    }
                                },
                                onBack = {
                                    Timber.d("Back navigation requested")
                                    coroutineScope.launch {
                                        actionWizardController.processStepResult(null)
                                    }
                                },
                                onForward = {
                                    Timber.d("Forward navigation requested")
                                    coroutineScope.launch {
                                        actionWizardController.processForwardStep()
                                    }
                                },
                                onSkip = { result ->
                                    Timber.d("Skip requested with result: $result")
                                },
                                onCancel = onCancel,
                                lastScannedBarcode = wizardState.lastScannedBarcode
                            )

                            // Создаем компонент шага
                            factory.createComponent(
                                step = actionStep,
                                action = action,
                                context = context
                            )
                        } else {
                            // Фабрика не найдена
                            Text(
                                text = "Ошибка: Нет компонента для типа объекта: ${actionStep.objectType}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        // ActionStep не найден
                        Text(
                            text = "Ошибка: Шаг не найден: ${currentStep.id}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    // Действие не найдено
                    Text(
                        text = "Ошибка: Действие не найдено",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопки действий с возможностью возврата назад
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

                // Кнопка "Завершить" - видна только на итоговом экране
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

            Spacer(modifier = Modifier.weight(0.1f))
        }
    }
}

// Функция для поиска ActionStep
private fun findActionStepForWizardStep(
    action: PlannedAction,
    stepId: String
): ActionStep? {
    Timber.d("Finding action step for wizard step: $stepId")
    Timber.d("Storage steps: ${action.actionTemplate.storageSteps.map { it.id }}")
    Timber.d("Placement steps: ${action.actionTemplate.placementSteps.map { it.id }}")

    // Ищем в шагах хранения
    action.actionTemplate.storageSteps.find { it.id == stepId }?.let {
        Timber.d("Found in storage steps: ${it.id}")
        return it
    }

    // Ищем в шагах размещения
    action.actionTemplate.placementSteps.find { it.id == stepId }?.let {
        Timber.d("Found in placement steps: ${it.id}")
        return it
    }

    Timber.w("Action step not found for wizard step: $stepId")
    return null
}

/**
 * Экран сводки действия
 */
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

            // Заменяем текстовое представление действия на строку с иконкой и подсказкой
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

            // Отображение результатов шагов
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