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
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionWizardState
import com.synngate.synnframe.domain.service.ActionWizardContextFactory
import com.synngate.synnframe.domain.service.ActionWizardController
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.ui.wizard.action.components.QuantityRow
import com.synngate.synnframe.presentation.ui.wizard.action.components.SummaryContainer
import com.synngate.synnframe.presentation.util.formatDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

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
    val coroutineScope = CoroutineScope(Dispatchers.Main)

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

    // Параметры анимации для перехода между шагами
    fun getContentTransformation(forward: Boolean): ContentTransform {
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

    // Запоминаем последний индекс шага для определения направления анимации
    var previousStepIndex by remember { mutableStateOf(wizardState.currentStepIndex) }
    val isForwardTransition = wizardState.currentStepIndex >= previousStepIndex

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

        // Используем AnimatedContent для плавного перехода между шагами
        AnimatedContent(
            targetState = wizardState.currentStepIndex,
            transitionSpec = { getContentTransformation(isForwardTransition) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), label = ""
        ) { _ ->
            // Контейнер для содержимого текущего шага
            Card(
                modifier = Modifier
                    .fillMaxSize(),
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
                        // Отображаем экран с итогами с использованием SummaryContainer
                        SummaryContainer(
                            title = "Подтвердите данные",
                            onBack = {
                                coroutineScope.launch {
                                    actionWizardController.processStepResult(null)
                                }
                            },
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

                                // Проверяем, первый ли это шаг
                                val isFirstStep = wizardState.currentStepIndex == 0

                                Timber.d("Шаг ${wizardState.currentStepIndex + 1}/${wizardState.steps.size}, первый: $isFirstStep")

                                factory.createComponent(
                                    step = actionStep,
                                    action = action,
                                    context = context.copy(isFirstStep = isFirstStep)  // Передаем флаг первого шага
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
            }
        }
    }
}

// Вспомогательная функция для поиска ActionStep по ID шага
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

@Composable
fun ActionSummaryScreen(
    state: ActionWizardState,
    modifier: Modifier = Modifier
) {
    val action = state.action

    val productEntry = findMostRelevantProductEntry(state.results)

    Column(
        modifier = modifier
            .fillMaxWidth()
        // Удаляем .verticalScroll(rememberScrollState()) чтобы избежать вложенных скроллов
    ) {
        // Заголовок убран, т.к. он уже отображается в SummaryContainer

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

            QuantityRow(label = "Запланировано:", value = plannedQuantity.toString())

            if (previousCompletedQuantity > 0f) {
                DefaultSpacer()
                QuantityRow(
                    label = "Выполнено:",
                    value = previousCompletedQuantity.toString()
                )
            }

            DefaultSpacer()
            QuantityRow(label = "Текущее:", value = currentQuantity.toString())

            if (previousCompletedQuantity > 0f) {
                DefaultSpacer()
                HorizontalDivider()
                DefaultSpacer()

                QuantityRow(label = "Текущий итог:", value = totalQuantity.toString())

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
fun DefaultSpacer(height: Dp = 4.dp) {
    Spacer(modifier = Modifier.height(height))
}