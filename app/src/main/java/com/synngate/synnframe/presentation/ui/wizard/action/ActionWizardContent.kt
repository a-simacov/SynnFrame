package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionWizardState
import com.synngate.synnframe.domain.model.wizard.WizardContextFactory
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.action.components.QuantityRow
import com.synngate.synnframe.presentation.ui.wizard.action.components.SummaryContainer
import com.synngate.synnframe.presentation.ui.wizard.action.components.formatQuantityDisplay
import com.synngate.synnframe.presentation.ui.wizard.action.utils.WizardUtils
import com.synngate.synnframe.presentation.util.formatDate
import timber.log.Timber

/**
 * Упрощенный основной компонент визарда действий
 */
@Composable
fun ActionWizardContent(
    wizardState: ActionWizardState?,
    onProcessStepResult: (Any?) -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onRetryComplete: () -> Unit,
    onBarcodeScanned: (String) -> Unit,
    actionStepFactoryRegistry: ActionStepFactoryRegistry,
    wizardContextFactory: WizardContextFactory,
    modifier: Modifier = Modifier
) {
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

    if (scannerService?.hasRealScanner() == true) {
        ScannerListener(onBarcodeScanned = { barcode ->
            if (!isProcessingGlobalBarcode) {
                isProcessingGlobalBarcode = true
                Timber.d("Глобальный сканер визарда: обнаружен штрих-код $barcode")
                onBarcodeScanned(barcode)
                isProcessingGlobalBarcode = false
            }
        })
    }

    var previousStepIndex by remember { mutableStateOf(wizardState.currentStepIndex) }
    val isForwardTransition = wizardState.currentStepIndex >= previousStepIndex

    var currentViewModel by remember { mutableStateOf<BaseStepViewModel<*>?>(null) }

    LaunchedEffect(wizardState.currentStepIndex) {
        previousStepIndex = wizardState.currentStepIndex
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

        AnimatedContent(
            targetState = wizardState.currentStepIndex,
            transitionSpec = { getContentTransformation(isForwardTransition) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = "step-animation"
        ) { stepIndex ->
            Timber.i(stepIndex.toString())
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    val currentStep = wizardState.currentStep
                    val action = wizardState.action

                    if (currentStep == null || wizardState.isCompleted) {
                        // Отображаем экран с итогами
                        SummaryContainer(
                            title = "Подтвердите данные",
                            onBack = { onProcessStepResult(null) },
                            onComplete = onComplete,
                            onRetry = onRetryComplete,
                            isSending = wizardState.isSending,
                            hasError = wizardState.sendError != null,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            ActionSummaryScreen(
                                state = wizardState,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else if (action != null) {
                        // Отображаем текущий шаг визарда
                        val actionStep = findActionStepForWizardStep(action, currentStep.id)

                        if (actionStep != null) {
                            val factory = actionStepFactoryRegistry.getFactory(actionStep.objectType)

                            if (factory != null) {
                                val context = wizardContextFactory.createContext(
                                    state = wizardState,
                                    onStepComplete = { result -> onProcessStepResult(result) },
                                    onBack = { onProcessStepResult(null) },
                                    // ИСПРАВЛЕНИЕ: Модифицируем обработчик onForward для получения данных из ViewModel напрямую
                                    onForward = {
                                        if (currentViewModel != null) {
                                            // Получаем данные из текущего ViewModel
                                            val data = currentViewModel?.state?.value?.data
                                            Timber.d("onForward: getting data directly from ViewModel: ${data?.javaClass?.simpleName}")

                                            if (data != null) {
                                                // Если данные есть, передаем их в обработчик результата
                                                onProcessStepResult(data)
                                            } else {
                                                // Логируем отсутствие данных
                                                Timber.w("No data available in current ViewModel for forward action")
                                            }
                                        } else {
                                            Timber.w("No current ViewModel available")
                                        }
                                    },
                                    onSkip = { /* Пустая реализация */ },
                                    onCancel = onCancel,
                                    lastScannedBarcode = wizardState.lastScannedBarcode
                                )

                                // Создаем компонент с шагом
                                factory.createComponent(
                                    step = actionStep,
                                    action = action,
                                    context = context
                                )

                                // Сохраняем ссылку на ViewModel
                                LaunchedEffect(actionStep.id) {
                                    val viewModel = factory.getViewModel(actionStep, action, context)
                                    if (viewModel != null) {
                                        Timber.d("Saved reference to ViewModel: ${viewModel.javaClass.simpleName}")
                                        currentViewModel = viewModel
                                    } else {
                                        Timber.w("Factory returned null ViewModel")
                                    }
                                }
                            } else {
                                NoFactoryFoundScreen(actionStep.objectType.toString())
                            }
                        } else {
                            NoStepFoundScreen(currentStep.id)
                        }
                    } else {
                        NoActionFoundScreen()
                    }
                }
            }
        }
    }
}

/**
 * Создает анимацию перехода между шагами
 */
private fun getContentTransformation(forward: Boolean): ContentTransform {
    return if (forward) {
        // Анимация перехода вперед (слайд вправо)
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth }, // Появляется справа
            animationSpec = tween(durationMillis = 300)
        ) + fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth }, // Исчезает влево
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut(animationSpec = tween(durationMillis = 150))
    } else {
        // Анимация перехода назад (слайд влево)
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth }, // Появляется слева
            animationSpec = tween(durationMillis = 300)
        ) + fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth }, // Исчезает вправо
                    animationSpec = tween(durationMillis = 300)
                ) + fadeOut(animationSpec = tween(durationMillis = 150))
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
private fun NoFactoryFoundScreen(objectType: String) {
    EmptyScreenContent(
        message = "Нет компонента для типа объекта: $objectType",
    )
}

@Composable
private fun NoStepFoundScreen(stepId: String) {
    EmptyScreenContent(
        message = "Шаг не найден: $stepId",
    )
}

@Composable
private fun NoActionFoundScreen() {
    EmptyScreenContent(
        message = "Действие не найдено",
    )
}

/**
 * Находит наиболее релевантную запись с товаром и количеством.
 * Приоритет отдается результату шага с типом PRODUCT_QUANTITY (ввод количества),
 * если он есть, иначе возвращает первый найденный TaskProduct.
 */
private fun findMostRelevantProductEntry(results: Map<String, Any>, state: ActionWizardState): Pair<String, TaskProduct>? {
    val quantityStepId = findQuantityStepId(state)

    if (quantityStepId != null) {

        val quantityEntry = results[quantityStepId] as? TaskProduct
        if (quantityEntry != null) {
            return Pair(quantityStepId, quantityEntry)
        }
    }

    val lastTaskProduct = results["lastTaskProduct"] as? TaskProduct
    if (lastTaskProduct != null) {
        return Pair("lastTaskProduct", lastTaskProduct)
    }

    val anyTaskProduct = results.entries.find { (_, value) ->
        value is TaskProduct && value.quantity > 0f
    }

    if (anyTaskProduct != null) {
        val taskProduct = anyTaskProduct.value as TaskProduct
        return Pair(anyTaskProduct.key, taskProduct)
    }

    val fallbackProduct = results.entries.find { (_, value) -> value is TaskProduct }

    if (fallbackProduct != null) {
        return Pair(fallbackProduct.key, fallbackProduct.value as TaskProduct)
    }

    val productEntry = results.entries.find { (_, value) -> value is Product }

    if (productEntry != null) {
        val product = productEntry.value as Product
        val createdTaskProduct = WizardUtils.createTaskProductFromProduct(product, 0f)
        return Pair(productEntry.key, createdTaskProduct)
    }

    return null
}

private fun findQuantityStepId(state: ActionWizardState): String? {
    val action = state.action ?: return null

    for (step in state.steps) {
        val actionStep = findActionStepForWizardStep(action, step.id)

        if (actionStep?.objectType == ActionObjectType.PRODUCT_QUANTITY) {
            return step.id
        }
    }

    return null
}

@Composable
fun ActionSummaryScreen(
    state: ActionWizardState,
    modifier: Modifier = Modifier
) {
    val action = state.action

    val productEntry = findMostRelevantProductEntry(state.results, state)

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        if (action != null) {
            Text(
                text = action.actionTemplate.name,
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

            val displayedBinCodes = mutableSetOf<String>()
            val displayedPalletCodes = mutableSetOf<String>()

            for ((_, value) in state.results.entries) {
                if (value !is TaskProduct && value !is Product) {
                    when (value) {
                        is Pallet -> {
                            if (!displayedPalletCodes.contains(value.code)) {
                                PalletInfo(value)
                                displayedPalletCodes.add(value.code)
                            }
                        }

                        is BinX -> {
                            if (!displayedBinCodes.contains(value.code)) {
                                BinInfo(value)
                                displayedBinCodes.add(value.code)
                            }
                        }
                    }
                }
            }
        } else {
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
        val quantity = it.storageProduct?.quantity?.toDouble() ?: 0.0
        quantity
    }.toFloat()

    val currentQuantity = taskProduct.quantity

    val totalQuantity = previousCompletedQuantity + currentQuantity

    val plannedQuantity = action.storageProduct?.let {
        if (it.product.id == taskProduct.product.id) it.quantity else 0f
    } ?: 0f

    val remainingQuantity = (plannedQuantity - totalQuantity).coerceAtLeast(0f)

    val isOverLimit = plannedQuantity > 0f && totalQuantity > plannedQuantity

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

            QuantityRow(label = "Запланировано:", value = formatQuantityDisplay(plannedQuantity))

            if (previousCompletedQuantity > 0f) {
                DefaultSpacer()
                QuantityRow(
                    label = "Выполнено:",
                    value = formatQuantityDisplay(previousCompletedQuantity)
                )
            }

            DefaultSpacer()
            QuantityRow(label = "Текущее:", value = formatQuantityDisplay(currentQuantity))

            if (previousCompletedQuantity > 0f) {
                DefaultSpacer()
                HorizontalDivider()
                DefaultSpacer()

                QuantityRow(label = "Текущий итог:", value = formatQuantityDisplay(totalQuantity))

                if (plannedQuantity > 0f && totalQuantity > plannedQuantity) {
                    DefaultSpacer()
                    Text(
                        text = "Внимание: общее количество превышает плановое!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            DefaultSpacer()
            QuantityRow(
                label = "Осталось:",
                value = formatQuantityDisplay(remainingQuantity),
                warning = isOverLimit
            )
        }
    }
}

@Composable
fun DefaultSpacer(height: Dp = 4.dp) {
    Spacer(modifier = Modifier.height(height))
}