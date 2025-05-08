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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionWizardState
import com.synngate.synnframe.domain.service.ActionWizardContextFactory
import com.synngate.synnframe.domain.service.ActionWizardController
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.util.formatDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
                onBarcodeScanned(barcode)
                isProcessingGlobalBarcode = false
            }
        })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        LinearProgressIndicator(
            progress = { wizardState.progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp)
        ) {
            val currentStep = wizardState.currentStep
            val action = wizardState.action

            if (currentStep == null || wizardState.isCompleted) {
                ActionSummaryScreen(
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
                        EmptyScreenContent(
                            message = "Нет компонента для типа объекта: ${actionStep.objectType}",
                        )
                    }
                } else {
                    EmptyScreenContent(
                        message = "Шаг не найден: ${currentStep.id}",
                    )
                }
            } else {
                EmptyScreenContent(
                    message = "Действие не найдено",
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        WizardActions(
            wizardState = wizardState,
            coroutineScope = coroutineScope,
            actionWizardController = actionWizardController,
            onComplete = onComplete,
            onRetryComplete = onRetryComplete,
        )
    }
}

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
            PreviousButton(
                coroutineScope = coroutineScope,
                actionWizardController = actionWizardController,
                enabled = !wizardState.isSending,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        if (wizardState.isCompleted) {
            if (wizardState.sendError != null) {
                RetryButton(
                    onRetryComplete = onRetryComplete,
                    modifier = Modifier.weight(1f),
                )
            } else {
                CompleteButton(
                    onComplete = onComplete,
                    enabled = !wizardState.isSending,
                    modifier = Modifier.weight(1f)
                )
            }
        } else if (!wizardState.isCompleted && wizardState.currentStep != null &&
            wizardState.hasResultForStep(wizardState.currentStep!!.id)
        ) {
            NextButton(
                coroutineScope = coroutineScope,
                actionWizardController = actionWizardController,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun RetryButton(
    onRetryComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onRetryComplete,
        modifier = modifier,
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
}

@Composable
private fun NextButton(
    coroutineScope: CoroutineScope,
    actionWizardController: ActionWizardController,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = {
            coroutineScope.launch {
                actionWizardController.processForwardStep()
            }
        },
        modifier = modifier
    ) {
        Text("Вперед")
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Вперед"
        )
    }
}

@Composable
private fun PreviousButton(
    coroutineScope: CoroutineScope,
    actionWizardController: ActionWizardController,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = {
            coroutineScope.launch {
                actionWizardController.processStepResult(null)
            }
        },
        modifier = modifier,
        enabled = enabled
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Назад"
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("Назад")
    }
}

@Composable
private fun CompleteButton(
    onComplete: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onComplete,
        modifier = modifier,
        enabled = enabled
    ) {
        if (!enabled) {
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

@Composable
fun ActionSummaryScreen(
    state: ActionWizardState,
    modifier: Modifier = Modifier
) {
    val action = state.action

    // Находим последний результат с товаром и количеством
    val productEntry = findMostRelevantProductEntry(state.results)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Проверьте информацию перед завершением",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (action != null) {
            Text(
                text = "Действие: ${action.actionTemplate.name}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.surfaceTint,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            if (productEntry != null) {
                val factActionsInfo =
                    state.results["factActions"] as Map<*, *>
                TaskProductInfo(
                    action = action,
                    factActionsInfo = factActionsInfo,
                    productEntry = productEntry
                )
            }

            for ((_, value) in state.results.entries) {
                if (value !is TaskProduct && value !is Product) {
                    when (value) {
                        is Pallet -> {
                            PalletInfo(value)
                        }

                        is BinX -> {
                            BinInfo(value)
                        }
                    }
                }
            }
        } else {
            // Если действие не определено, показываем сообщение
            Text(
                text = "Информация о действии недоступна",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun PalletInfo(pallet: Pallet) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Text(
            text = "Паллета: ${pallet.code}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Composable
private fun BinInfo(bin: BinX) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Text(
            text = "Ячейка: ${bin.code}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Composable
private fun TaskProductInfo(
    action: PlannedAction,
    factActionsInfo: Map<out Any?, Any?>,
    productEntry: Pair<String, TaskProduct>
) {
    val taskProduct = productEntry.second

    @Suppress("UNCHECKED_CAST")
    val relatedFactActions =
        (factActionsInfo[action.id] as? List<FactAction>) ?: emptyList()

    val previousCompletedQuantity = relatedFactActions.sumOf {
        it.storageProduct?.quantity?.toDouble() ?: 0.0
    }.toFloat()

    val currentQuantity = taskProduct.quantity
    val totalQuantity = previousCompletedQuantity + currentQuantity

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Text(
                text = taskProduct.product.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            DefaultSpacer(8.dp)

            Text(
                text = "Артикул: ${taskProduct.product.articleNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Text(
                text = "Статус: ${taskProduct.status.format()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )

            if (taskProduct.hasExpirationDate()) {
                Text(
                    text = "Срок годности: ${formatDate(taskProduct.expirationDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            DefaultSpacer()
            HorizontalDivider()
            DefaultSpacer()

            val plannedQuantity = action.storageProduct?.let {
                if (it.product.id == taskProduct.product.id) it.quantity else 0f
            } ?: 0f

            QuantityRow(textName = "Запланировано:", textQuantity = plannedQuantity.toString())

            if (previousCompletedQuantity > 0f) {
                DefaultSpacer()
                QuantityRow(
                    textName = "Выполнено",
                    textQuantity = previousCompletedQuantity.toString()
                )
            }

            DefaultSpacer()
            QuantityRow(textName = "Текущее", textQuantity = currentQuantity.toString())

            if (previousCompletedQuantity > 0f) {
                DefaultSpacer()
                HorizontalDivider()
                DefaultSpacer()

                QuantityRow(textName = "Текущий итог", textQuantity = totalQuantity.toString())

                if (plannedQuantity > 0f && totalQuantity > plannedQuantity) {
                    DefaultSpacer()
                    Text(
                        text = "Внимание: общее количество превышает плановое!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun QuantityRow(
    textName: String,
    textQuantity: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = textName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = textQuantity,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DefaultSpacer(height: Dp = 4.dp) {
    Spacer(modifier = Modifier.height(height))
}

/**
 * Находит наиболее релевантную запись с товаром и количеством.
 * Приоритет отдается результату шага с типом PRODUCT_QUANTITY (ввод количества),
 * если он есть, иначе возвращает первый найденный TaskProduct.
 */
private fun findMostRelevantProductEntry(results: Map<String, Any>): Pair<String, TaskProduct>? {
    // Сначала ищем результаты шага ввода количества (они будут иметь ненулевое количество)
    val quantityProductEntry = results.entries.find { (_, value) ->
        value is TaskProduct && value.quantity > 0
    }

    if (quantityProductEntry != null) {
        return Pair(quantityProductEntry.key, quantityProductEntry.value as TaskProduct)
    }

    // Если не нашли, возвращаем первый найденный TaskProduct
    val productEntry = results.entries.find { (_, value) ->
        value is TaskProduct
    }

    return if (productEntry != null) {
        Pair(productEntry.key, productEntry.value as TaskProduct)
    } else {
        null
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