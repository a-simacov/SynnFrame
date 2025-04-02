package com.synngate.synnframe.presentation.ui.tasks

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.CreationPlace
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.presentation.common.dialog.ConfirmationDialog
import com.synngate.synnframe.presentation.common.inputs.BarcodeTextField
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.ErrorScreenContent
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.tasks.components.TaskFactLineDialog
import com.synngate.synnframe.presentation.ui.tasks.components.TaskLineItemRow
import com.synngate.synnframe.presentation.ui.tasks.components.TaskNoPlannedItemRow
import com.synngate.synnframe.presentation.ui.tasks.model.EntryStep
import com.synngate.synnframe.presentation.ui.tasks.model.ProductDisplayProperty
import com.synngate.synnframe.presentation.ui.tasks.model.ProductPropertyType
import com.synngate.synnframe.presentation.ui.tasks.model.TaskDetailEvent
import com.synngate.synnframe.presentation.ui.tasks.model.TaskDetailState
import com.synngate.synnframe.presentation.util.formatQuantity

@Composable
fun TaskDetailScreen(
    viewModel: TaskDetailViewModel,
    navigateBack: () -> Unit,
    navigateToProductsList: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val state by viewModel.uiState.collectAsState()
    val task = state.task

    val snackbarHostState = remember { SnackbarHostState() }

    val updateSuccessMessage = stringResource(id = R.string.update_success)

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Обработка событий
    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TaskDetailEvent.NavigateBack -> {
                    navigateBack()
                }

                is TaskDetailEvent.NavigateToProductsList -> {
                    navigateToProductsList()
                }

                is TaskDetailEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is TaskDetailEvent.ShowScanDialog -> {
                    // Диалог отображается через state.isScanDialogVisible
                }

                is TaskDetailEvent.ShowFactLineDialog -> {
                    // Диалог отображается через state.isFactLineDialogVisible
                }

                is TaskDetailEvent.CloseDialog -> {
                    // Закрытие диалогов обрабатывается через ViewMоdel
                }

                is TaskDetailEvent.UpdateSuccess -> {
                    snackbarHostState.showSnackbar(updateSuccessMessage)
                }

                is TaskDetailEvent.ShowDeleteConfirmation -> {
                    showDeleteDialog = true
                }
                is TaskDetailEvent.HideDeleteConfirmation -> {
                    showDeleteDialog = false
                }
            }
        }
    }

    if (state.isCompleteConfirmationVisible) {
        ConfirmationDialog(
            title = stringResource(id = R.string.complete_task),
            message = stringResource(id = R.string.complete_task_confirmation),
            onConfirm = { viewModel.completeTask() },
            onDismiss = { viewModel.closeDialog() }
        )
    }

    if (state.isFactLineDialogVisible && state.selectedFactLine != null) {
        TaskFactLineDialog(
            factLine = state.selectedFactLine!!,
            product = state.entryProduct,
            planQuantity = state.selectedPlanQuantity,
            dialogState = state.factLineDialogState,
            onQuantityChange = { viewModel.onQuantityChange(it) },
            onError = { viewModel.onQuantityError(it) },
            onApply = { factLine, additionalQuantity ->
                viewModel.applyQuantityChange(factLine, additionalQuantity)
            },
            onDismiss = { viewModel.closeDialog() }
        )
    }

    // Определение заголовка экрана и подзаголовка
    val screenTitle = task?.getDisplayHeader() ?: ""

    // Проверяем, является ли задание заданием без плана
    val hasPlan = task?.planLines?.isNotEmpty() == true && task.getTotalPlanQuantity() > 0

    // Обработка возвращаемого значения из экрана выбора товара
    LaunchedEffect(navBackStackEntry) {
        val savedStateHandle = navBackStackEntry?.savedStateHandle

        // Проверяем, есть ли ID выбранного товара
        val productId = savedStateHandle?.get<String>("selected_product_id")

        if (productId != null) {
            viewModel.handleSelectedProductById(productId)
        }
    }

    AppScaffold(
        title = screenTitle,
        subtitle = if (task?.allowProductsNotInPlan == true)
            stringResource(id = R.string.allow_products_not_in_plan) else null,
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        bottomBar = {
            // Кнопки управления заданием
            if (task != null) {
                when (task.status) {
                    TaskStatus.TO_DO -> {
                        // Кнопка "Начать выполнение"
                        Button(
                            onClick = { viewModel.startTask() },
                            enabled = !state.isProcessing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(id = R.string.start_task))
                        }
                    }

                    TaskStatus.IN_PROGRESS -> {
                        // Кнопка "Завершить"
                        Button(
                            onClick = { viewModel.showCompleteConfirmation() },
                            enabled = !state.isProcessing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(id = R.string.complete_task))
                        }
                    }

                    TaskStatus.COMPLETED -> {
                        if (state.task?.canDelete() == true) {
                        } else
                            // Кнопка "Выгрузить"
                            Button(
                                onClick = { viewModel.uploadTask() },
                                enabled = !state.isProcessing && !task.uploaded,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (task.uploaded)
                                        stringResource(id = R.string.task_already_uploaded)
                                    else
                                        stringResource(id = R.string.upload_task)
                                )
                            }
                    }
                }
            }
        },
        isLoading = state.isLoading || state.isProcessing
    ) { paddingValues ->
        if (state.error != null && task == null) {
            ErrorScreenContent(
                message = state.error!!,
                onRetry = { viewModel.loadTask() }
            )
        } else if (task != null) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                BarcodeTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    onBarcodeScanned = { viewModel.processScanResult(it) },
                    label = when(state.entryStep) {
                        EntryStep.ENTER_BIN -> "Введите или отсканируйте ячейку"
                        EntryStep.ENTER_PRODUCT -> "Введите или отсканируйте товар"
                        EntryStep.ENTER_QUANTITY -> "Введите количество"
                        EntryStep.NONE -> "Введите или отсканируйте штрихкод"
                    },
                    enabled = state.isEditable,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Row {
                            // Кнопка отмены ввода
                            if (state.isEntryActive) {
                                IconButton(onClick = { viewModel.cancelFactLineEntry() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Отменить ввод"
                                    )
                                }
                            }
                        }
                    }
                )

                // Отображаем панель процесса ввода только когда активен ввод
                if (state.isEntryActive) {
                    FactEntryPanel(
                        state = state,
                        onCancel = { viewModel.cancelFactLineEntry() },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (state.isEditable) {
                    // Кнопка для начала ввода новой строки
                    Button(
                        onClick = { viewModel.startFactLineEntry() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text("Добавить строку факта")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Отображаем разный заголовок и строки таблицы в зависимости от наличия плана
                if (hasPlan) {
                    // Для заданий с планом - полная таблица с тремя колонками
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.product),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(0.6f)
                        )

                        Text(
                            text = stringResource(id = R.string.plan_quantity),
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.2f)
                        )

                        Text(
                            text = stringResource(id = R.string.fact_quantity),
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.2f)
                        )
                    }
                } else {
                    // Для заданий без плана - упрощенная таблица без колонки плана
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.product),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(0.7f)
                        )

                        Text(
                            text = stringResource(id = R.string.quantity),
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.3f)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                if (state.taskLines.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.no_task_lines),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Список настраиваемых свойств товара для отображения
                    val productProperties = listOf(
                        ProductDisplayProperty(ProductPropertyType.NAME), // Наименование товара
                        ProductDisplayProperty(
                            type = ProductPropertyType.ID,
                            label = "ID"
                        ),
                        ProductDisplayProperty(
                            type = ProductPropertyType.ARTICLE,
                            label = "Артикул"
                        )
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(
                            items = state.taskLines,
                            key = { it.planLine.id }
                        ) { lineItem ->
                            if (hasPlan) {
                                // Для заданий с планом используем стандартную строку
                                TaskLineItemRow(
                                    lineItem = lineItem,
                                    isEditable = state.isEditable,
                                    onClick = {
//                                        if (state.isEditable) {
//                                            lineItem.factLine?.let {
//                                                viewModel.showFactLineDialog(it.productId)
//                                            }
//                                                ?: viewModel.showFactLineDialog(lineItem.planLine.productId)
//                                        }
                                    },
                                    productProperties = productProperties
                                )
                            } else {
                                // Для заданий без плана используем упрощенную строку
                                TaskNoPlannedItemRow(
                                    lineItem = lineItem,
                                    isEditable = state.isEditable,
                                    onClick = {
//                                        if (state.isEditable) {
//                                            lineItem.factLine?.let {
//                                                viewModel.showFactLineDialog(it.productId)
//                                            }
//                                                ?: viewModel.showFactLineDialog(lineItem.planLine.productId)
//                                        }
                                    },
                                    productProperties = productProperties
                                )
                            }
                        }
                    }
                }

                if (state.taskLines.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (hasPlan) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = R.string.total),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(0.6f)
                            )

                            val totalPlan = task.getTotalPlanQuantity()
                            Text(
                                text = formatQuantity(totalPlan),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.2f)
                            )

                            val totalFact = task.getTotalFactQuantity()
                            Text(
                                text = formatQuantity(totalFact),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = if (totalFact >= totalPlan)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(0.2f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val matchingLinesCount = state.taskLines.count {
                            it.factLine != null && it.factLine.quantity == it.planLine.quantity && it.factLine.quantity > 0f
                        }
                        val totalLinesCount = state.taskLines.size

                        Row(
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = stringResource(
                                    id = R.string.task_completion_percentage,
                                    task.getCompletionPercentage()
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    id = R.string.matching_lines_count,
                                    matchingLinesCount,
                                    totalLinesCount
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        // Для заданий без плана показываем только общее количество
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = R.string.total),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(0.7f)
                            )

                            val totalFact = task.getTotalFactQuantity()
                            Text(
                                text = formatQuantity(totalFact),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(0.3f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun FactEntryPanel(
    state: TaskDetailState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Заголовок текущего шага
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when(state.entryStep) {
                        EntryStep.ENTER_BIN -> "Введите ячейку"
                        EntryStep.ENTER_PRODUCT -> "Введите товар"
                        EntryStep.ENTER_QUANTITY -> "Введите количество"
                        EntryStep.NONE -> "Ввод строки факта"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Отменить"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Данные введенной ячейки
            if (state.entryBinCode != null) {
                EntryDataRow(
                    label = "Ячейка:",
                    value = state.entryBinName ?: state.entryBinCode,
                    isHighlighted = state.entryStep == EntryStep.ENTER_BIN
                )
            }

            // Данные выбранного товара
            if (state.entryProduct != null) {
                EntryDataRow(
                    label = "Товар:",
                    value = state.entryProduct.name,
                    isHighlighted = state.entryStep == EntryStep.ENTER_PRODUCT
                )
            }

            // Данные введенного количества
            if (state.entryQuantity != null) {
                EntryDataRow(
                    label = "Количество:",
                    value = formatQuantity(state.entryQuantity),
                    isHighlighted = state.entryStep == EntryStep.ENTER_QUANTITY
                )
            }
        }
    }
}

@Composable
fun EntryDataRow(
    label: String,
    value: String,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent,
                shape = MaterialTheme.shapes.small
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun Task.getDisplayHeader(): String {
    // Используем название типа задания вместо enum
    val taskTypeName = this.taskType?.name ?: this.taskTypeId

    val taskBarcode = if (this.creationPlace == CreationPlace.APP) {
        "${this.barcode.take(5)}...${this.barcode.takeLast(10)}"
    } else this.barcode

    return "$taskTypeName ($taskBarcode)"
}