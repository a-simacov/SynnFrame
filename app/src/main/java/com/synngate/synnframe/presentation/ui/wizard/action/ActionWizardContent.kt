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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    // Обработка глобального сканирования
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

    // Запоминаем последний индекс шага для определения направления анимации
    var previousStepIndex by remember { mutableStateOf(wizardState.currentStepIndex) }
    val isForwardTransition = wizardState.currentStepIndex >= previousStepIndex

    // Храним текущий ViewModel для получения данных напрямую
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

        // Используем AnimatedContent для плавного перехода между шагами
        AnimatedContent(
            targetState = wizardState.currentStepIndex,
            transitionSpec = { getContentTransformation(isForwardTransition) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = "step-animation"
        ) { stepIndex ->
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

/**
 * Находит ActionStep по ID шага в визарде
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

/**
 * Экран, отображаемый при отсутствии фабрики для типа объекта
 */
@Composable
private fun NoFactoryFoundScreen(objectType: String) {
    EmptyScreenContent(
        message = "Нет компонента для типа объекта: $objectType",
    )
}

/**
 * Экран, отображаемый при отсутствии шага
 */
@Composable
private fun NoStepFoundScreen(stepId: String) {
    EmptyScreenContent(
        message = "Шаг не найден: $stepId",
    )
}

/**
 * Экран, отображаемый при отсутствии действия
 */
@Composable
private fun NoActionFoundScreen() {
    EmptyScreenContent(
        message = "Действие не найдено",
    )
}

/**
 * Компонент для отображения ошибки инициализации
 */
@Composable
fun InitializationErrorScreen(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Находит наиболее релевантную запись с товаром и количеством.
 * Приоритет отдается результату шага с типом PRODUCT_QUANTITY (ввод количества),
 * если он есть, иначе возвращает первый найденный TaskProduct.
 */
private fun findMostRelevantProductEntry(results: Map<String, Any>, state: ActionWizardState): Pair<String, TaskProduct>? {
    // Для отладки выводим все TaskProduct в результатах
    results.entries.filter { (_, value) -> value is TaskProduct }.forEach { (key, value) ->
        val product = value as TaskProduct
        Timber.d("Available TaskProduct: key=$key, product=${product.product.name}, quantity=${product.quantity}")
    }

    // ИСПРАВЛЕНИЕ: Динамически ищем шаг ввода количества по его типу объекта
    val quantityStepId = findQuantityStepId(state)

    if (quantityStepId != null) {
        Timber.d("Found quantity step ID: $quantityStepId")

        val quantityEntry = results[quantityStepId] as? TaskProduct
        if (quantityEntry != null) {
            Timber.d("Using quantity step with ID=$quantityStepId, quantity=${quantityEntry.quantity}")
            return Pair(quantityStepId, quantityEntry)
        }
    }

    // Если нет записи с ID шага ввода количества, используем lastTaskProduct
    val lastTaskProduct = results["lastTaskProduct"] as? TaskProduct
    if (lastTaskProduct != null) {
        Timber.d("Using lastTaskProduct with quantity=${lastTaskProduct.quantity}")
        return Pair("lastTaskProduct", lastTaskProduct)
    }

    // Находим любой ненулевой TaskProduct для отображения
    val anyTaskProduct = results.entries.find { (_, value) ->
        value is TaskProduct && value.quantity > 0f
    }

    if (anyTaskProduct != null) {
        val taskProduct = anyTaskProduct.value as TaskProduct
        Timber.d("Falling back to any TaskProduct with quantity=${taskProduct.quantity}")
        return Pair(anyTaskProduct.key, taskProduct)
    }

    // В крайнем случае берем любой TaskProduct
    val fallbackProduct = results.entries.find { (_, value) -> value is TaskProduct }

    if (fallbackProduct != null) {
        Timber.d("Using fallback TaskProduct")
        return Pair(fallbackProduct.key, fallbackProduct.value as TaskProduct)
    }

    // Если ничего не нашли, пробуем создать из Product
    val productEntry = results.entries.find { (_, value) -> value is Product }

    if (productEntry != null) {
        val product = productEntry.value as Product
        val createdTaskProduct = WizardUtils.createTaskProductFromProduct(product, 0f)
        Timber.d("Created TaskProduct from Product")
        return Pair(productEntry.key, createdTaskProduct)
    }

    Timber.w("No suitable TaskProduct found!")
    return null
}

/**
 * Находит ID шага ввода количества в визарде по типу объекта
 */
private fun findQuantityStepId(state: ActionWizardState): String? {
    val action = state.action ?: return null

    // Находим шаг с типом объекта PRODUCT_QUANTITY
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
        // Заголовок убран, т.к. он уже отображается в SummaryContainer

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

            // ИСПРАВЛЕНИЕ: Избегаем дублирования объектов
            // Создаем набор уже отображенных ячеек и паллет по их коду
            val displayedBinCodes = mutableSetOf<String>()
            val displayedPalletCodes = mutableSetOf<String>()

            for ((_, value) in state.results.entries) {
                if (value !is TaskProduct && value !is Product) {
                    when (value) {
                        is Pallet -> {
                            // Проверяем, не отображали ли мы уже эту паллету
                            if (!displayedPalletCodes.contains(value.code)) {
                                PalletInfo(value)
                                displayedPalletCodes.add(value.code)
                            }
                        }

                        is BinX -> {
                            // Проверяем, не отображали ли мы уже эту ячейку
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

    // Логируем данные для отладки
    Timber.d("TaskProductInfo: Using product ${taskProduct.product.name} with quantity ${taskProduct.quantity}")
    Timber.d("TaskProductInfo: Found from key ${productEntry.first}")

    if (action.storageProduct != null) {
        Timber.d("TaskProductInfo: Planned product ${action.storageProduct.product.name} with quantity ${action.storageProduct.quantity}")
    }

    @Suppress("UNCHECKED_CAST")
    val relatedFactActions =
        (factActionsInfo[action.id] as? List<FactAction>) ?: emptyList()

    // Получаем количество из предыдущих фактических действий
    val previousCompletedQuantity = relatedFactActions.sumOf {
        val quantity = it.storageProduct?.quantity?.toDouble() ?: 0.0
        Timber.d("  - Fact action quantity: $quantity")
        quantity
    }.toFloat()

    Timber.d("TaskProductInfo: Previous completed quantity: $previousCompletedQuantity")

    // ИСПРАВЛЕНИЕ: Здесь текущее количество берется из TaskProduct, выбранного пользователем
    val currentQuantity = taskProduct.quantity
    Timber.d("TaskProductInfo: Current quantity from taskProduct: $currentQuantity")

    // Итоговое количество - это сумма предыдущих выполненных действий и текущего вводимого значения
    val totalQuantity = previousCompletedQuantity + currentQuantity
    Timber.d("TaskProductInfo: Total quantity: $totalQuantity")

    // Запланированное количество из действия или 0, если не указано
    val plannedQuantity = action.storageProduct?.let {
        if (it.product.id == taskProduct.product.id) it.quantity else 0f
    } ?: 0f
    Timber.d("TaskProductInfo: Planned quantity: $plannedQuantity")

    // Вычисляем оставшееся количество (с ограничением не менее 0)
    val remainingQuantity = (plannedQuantity - totalQuantity).coerceAtLeast(0f)

    // Проверяем, превышает ли общее количество запланированное
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

            // Отображаем все требуемые строки с количествами
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

            // Добавляем строку с оставшимся количеством
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