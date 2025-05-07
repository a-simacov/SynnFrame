package com.synngate.synnframe.presentation.ui.wizard.action

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.model.wizard.ActionWizardState
import com.synngate.synnframe.domain.service.ActionWizardContextFactory
import com.synngate.synnframe.domain.service.ActionWizardController
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Содержимое экрана визарда действий
 */
@Composable
fun ActionWizardContent(
    wizardState: ActionWizardState?,
    actionWizardController: ActionWizardController,
    actionWizardContextFactory: ActionWizardContextFactory,
    actionStepFactoryRegistry: ActionStepFactoryRegistry,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onRetryComplete: () -> Unit,
    onBarcodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    if (wizardState == null) {
        EmptyScreenContent(
            message = "Визард не инициализирован",
            modifier = modifier
        )
        return
    }

    // Получаем сервис сканера
    val scannerService = LocalScannerService.current

    var isProcessingGlobalBarcode by remember { mutableStateOf(false) }

    LaunchedEffect(wizardState.currentStepIndex) {
        isProcessingGlobalBarcode = false
    }

    // Глобальный слушатель сканера для итогового экрана
    if (wizardState.isCompleted && scannerService?.hasRealScanner() == true) {
        ScannerListener(onBarcodeScanned = { barcode ->
            if (!isProcessingGlobalBarcode) {
                isProcessingGlobalBarcode = true
                Timber.d("Штрихкод от встроенного сканера (глобальный): $barcode")
                onBarcodeScanned(barcode)
                isProcessingGlobalBarcode = false
            }
        })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Заголовок и описание типа действия
        WizardHeader(wizardState)

        // Индикатор прогресса
        LinearProgressIndicator(
            progress = { wizardState.progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )

        // Основное содержимое в зависимости от текущего шага
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            val currentStep = wizardState.currentStep
            val action = wizardState.action

            if (currentStep == null || wizardState.isCompleted) {
                ActionSummaryScreenNew(
                    state = wizardState,
                    modifier = Modifier.fillMaxSize()
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
                            onSkip = { /* Пустая реализация */ },
                            onCancel = onCancel,
                            lastScannedBarcode = wizardState.lastScannedBarcode
                        )

                        factory.createComponent(
                            step = actionStep,
                            action = action,
                            context = context
                        )
                    } else {
                        // Фабрика не найдена для типа объекта
                        EmptyScreenContent(
                            message = "Нет компонента для типа объекта: ${actionStep.objectType}",
                        )
                    }
                } else {
                    // Шаг не найден
                    EmptyScreenContent(
                        message = "Шаг не найден: ${currentStep.id}",
                    )
                }
            } else {
                // Действие не найдено
                EmptyScreenContent(
                    message = "Действие не найдено",
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопки навигации и действий
        WizardActions(
            wizardState = wizardState,
            coroutineScope = coroutineScope,
            actionWizardController = actionWizardController,
            onComplete = onComplete,
            onRetryComplete = onRetryComplete,
        )
    }
}

/**
 * Заголовок визарда с информацией о типе действия
 */
@Composable
private fun WizardHeader(wizardState: ActionWizardState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = wizardState.action?.wmsAction?.let {
                getWmsActionDescription(it)
            } ?: "Выполнение действия",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Кнопки навигации и действий визарда
 */
@Composable
private fun WizardActions(
    wizardState: ActionWizardState,
    coroutineScope: CoroutineScope,
    actionWizardController: ActionWizardController,
    onComplete: () -> Unit,
    onRetryComplete: () -> Unit,
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
            // Пустой Spacer для сохранения выравнивания
            Spacer(modifier = Modifier.weight(1f))
        }

        if (wizardState.isCompleted) {
            // Если есть ошибка отправки, показываем кнопку "Повторить"
            if (wizardState.sendError != null) {
                Button(
                    onClick = onRetryComplete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Повторить",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Повторить")
                }
            } else {
                // Кнопка "Завершить" для обычного завершения
                Button(
                    onClick = onComplete,
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
        } else if (!wizardState.isCompleted && wizardState.currentStep != null &&
            wizardState.hasResultForStep(wizardState.currentStep!!.id)) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        actionWizardController.processForwardStep()
                    }
                },
                modifier = Modifier.weight(1f)
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

/**
 * Экран сводки по выполненному действию
 */
@Composable
fun ActionSummaryScreenNew(
    state: ActionWizardState,
    modifier: Modifier = Modifier
) {
    // Используем существующую реализацию из ActionWizardScreen.kt
    // Этот код можно скопировать из текущей реализации
    // Для краткости здесь опущено содержимое метода

    // В реальном приложении стоит скопировать полную реализацию
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Проверьте информацию перед завершением",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Индикатор отправки данных
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

        // Блок с информацией об ошибке
        if (state.sendError != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Ошибка",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = "Ошибка отправки данных",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = state.sendError ?: "Неизвестная ошибка",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Здесь должно быть отображение информации о выполненном действии
        // Включая тип действия, выбранный товар, количество, ячейки и т.д.
        // Код опущен для краткости
    }
}

/**
 * Находит объект ActionStep для заданного ID шага в визарде
 */
private fun findActionStepForWizardStep(
    action: com.synngate.synnframe.domain.entity.taskx.action.PlannedAction,
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