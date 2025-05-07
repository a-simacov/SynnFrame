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
import androidx.compose.material.icons.filled.Error
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.model.wizard.ActionWizardState
import com.synngate.synnframe.domain.service.ActionWizardContextFactory
import com.synngate.synnframe.domain.service.ActionWizardController
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.ui.taskx.components.WmsActionIconWithTooltip
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.format.DateTimeFormatter

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

            // Отображаем информацию о товаре и количестве, если есть
            if (productEntry != null) {
                val (stepId, taskProduct) = productEntry

                // Получаем информацию о фактических действиях из контекста
                val factActionsInfo = state.results["factActions"] as? Map<*, *> ?: emptyMap<String, Any>()

                // Получаем список фактических действий для текущего планового действия
                @Suppress("UNCHECKED_CAST")
                val relatedFactActions = (factActionsInfo[action.id] as? List<FactAction>) ?: emptyList()

                // Рассчитываем общее выполненное количество из предыдущих действий
                val previousCompletedQuantity = relatedFactActions.sumOf {
                    it.storageProduct?.quantity?.toDouble() ?: 0.0
                }.toFloat()

                // Текущее введенное количество
                val currentQuantity = taskProduct.quantity

                // Общее количество (предыдущие действия + текущее)
                val totalQuantity = previousCompletedQuantity + currentQuantity

                // Отображаем карточку товара с количеством
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Товар",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = taskProduct.product.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Text(
                            text = "Артикул: ${taskProduct.product.articleNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        // Отображаем статус товара
                        val statusText = when(taskProduct.status) {
                            ProductStatus.STANDARD -> "Кондиция (стандарт)"
                            ProductStatus.DEFECTIVE -> "Брак"
                            ProductStatus.EXPIRED -> "Просрочен"
                            else -> "Неизвестный статус"
                        }

                        Text(
                            text = "Статус: $statusText",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )

                        // Отображаем срок годности, если он установлен
                        if (taskProduct.hasExpirationDate()) {
                            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                            Text(
                                text = "Срок годности: ${taskProduct.expirationDate.format(formatter)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Получаем плановое количество из действия
                        val plannedQuantity = action.storageProduct?.let {
                            if (it.product.id == taskProduct.product.id) it.quantity else 0f
                        } ?: 0f

                        // Отображаем плановое, текущее и общее количество
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Плановое количество:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            Text(
                                text = plannedQuantity.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Добавляем информацию о предыдущих выполненных действиях
                        if (previousCompletedQuantity > 0f) {
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Ранее выполнено:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )

                                Text(
                                    text = previousCompletedQuantity.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Текущее количество:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            Text(
                                text = currentQuantity.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Добавляем информацию об общем выполненном количестве
                        if (previousCompletedQuantity > 0f) {
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Всего будет выполнено:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = totalQuantity.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Показываем индикацию, если общее количество превысит плановое
                            if (plannedQuantity > 0f && totalQuantity > plannedQuantity) {
                                Spacer(modifier = Modifier.height(4.dp))
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

            // Отображаем другие результаты, не связанные с товаром
            for ((stepId, value) in state.results.entries) {
                if (value !is TaskProduct && value !is Product) { // Исключаем товары, они уже отображены выше
                    when (value) {
                        is Pallet -> {
                            displayPalletInfo(value)
                        }
                        is BinX -> {
                            displayBinInfo(value)
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

/**
 * Отображает информацию о паллете
 */
@Composable
private fun displayPalletInfo(pallet: Pallet) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Паллета",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Код: ${pallet.code}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Text(
                text = "Статус: ${if (pallet.isClosed) "Закрыта" else "Открыта"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/**
 * Отображает информацию о ячейке
 */
@Composable
private fun displayBinInfo(bin: BinX) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Ячейка",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Код: ${bin.code}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Text(
                text = "Зона: ${bin.zone}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            if (bin.line.isNotEmpty() || bin.rack.isNotEmpty() || bin.tier.isNotEmpty() || bin.position.isNotEmpty()) {
                Text(
                    text = "Расположение: ${bin.getFullName()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
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