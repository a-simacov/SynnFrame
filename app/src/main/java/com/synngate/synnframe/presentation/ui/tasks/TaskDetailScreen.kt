package com.synngate.synnframe.presentation.ui.tasks

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.CreationPlace
import com.synngate.synnframe.domain.entity.FactLineActionType
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.presentation.common.buttons.ActionButton
import com.synngate.synnframe.presentation.common.dialog.ConfirmationDialog
import com.synngate.synnframe.presentation.common.inputs.BarcodeTextField
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.ErrorScreenContent
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.tasks.components.ScanBarcodeDialog
import com.synngate.synnframe.presentation.ui.tasks.components.TaskFactLineDialog
import com.synngate.synnframe.presentation.ui.tasks.components.TaskLineItemRow
import com.synngate.synnframe.presentation.ui.tasks.components.TaskNoPlannedItemRow
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

    val showScanDialog by rememberSaveable { mutableStateOf(state.isScanDialogVisible) }
    val showFactLineDialog by rememberSaveable { mutableStateOf(state.isFactLineDialogVisible) }
    val showCompleteConfirmation by rememberSaveable { mutableStateOf(state.isCompleteConfirmationVisible) }

    LaunchedEffect(showScanDialog, showFactLineDialog, showCompleteConfirmation) {
        if (showScanDialog != state.isScanDialogVisible) {
            if (showScanDialog) viewModel.showScanDialog() else viewModel.closeDialog()
        }

        if (showFactLineDialog != state.isFactLineDialogVisible) {
            if (!showFactLineDialog) viewModel.closeDialog()
        }

        if (showCompleteConfirmation != state.isCompleteConfirmationVisible) {
            if (showCompleteConfirmation) viewModel.showCompleteConfirmation() else viewModel.closeDialog()
        }
    }

    // Используем либо состояние из viewModel, либо локальное состояние
    val showDeleteConfirmationDialog = state.showDeleteConfirmation || showDeleteDialog

    if (showDeleteConfirmationDialog) {
        ConfirmationDialog(
            title = stringResource(id = R.string.delete_task_title),
            message = stringResource(id = R.string.delete_task_message),
            onConfirm = { viewModel.deleteTask() },
            onDismiss = { viewModel.hideDeleteConfirmation() }
        )
    }

    if (state.isScanDialogVisible) {
        ScanBarcodeDialog(
            onBarcodeScanned = { barcode -> viewModel.processScanResultForCurrentAction(barcode) },
            onClose = { viewModel.closeDialog() },
            scannerMessage = state.scanBarcodeDialogState.scannerMessage,
            isScannerActive = state.scanBarcodeDialogState.isScannerActive,
            onScannerActiveChange = { viewModel.toggleScannerActive(it) }
        )
    }

    if (state.isFactLineDialogVisible && state.selectedFactLine != null) {
        val product =
            state.taskLines.find { it.planLine.productId == state.selectedFactLine!!.productId }?.product
        TaskFactLineDialog(
            factLine = state.selectedFactLine!!,
            product = product,
            planQuantity = state.selectedPlanQuantity,
            dialogState = state.factLineDialogState,
            onQuantityChange = { viewModel.updateFactLineAdditionalQuantity(it) },
            onError = { viewModel.setFactLineInputError(it) },
            onApply = { factLine, additionalQuantity ->
                viewModel.applyQuantityChange(factLine, additionalQuantity)
            },
            onDismiss = { viewModel.closeDialog() }
        )
    }

    if (state.isCompleteConfirmationVisible) {
        ConfirmationDialog(
            title = stringResource(id = R.string.complete_task),
            message = stringResource(id = R.string.complete_task_confirmation),
            onConfirm = { viewModel.completeTask() },
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
        savedStateHandle?.get<String>("selected_product_id")?.let { productId ->
            // Загружаем продукт по ID и обрабатываем его
            viewModel.handleSelectedProductById(productId)
            // Удаляем данные, чтобы избежать повторной обработки
            savedStateHandle.remove<String>("selected_product_id")
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
                            // Кнопка повторной выгрузки
                            ActionButton(
                                text = stringResource(id = R.string.reupload_task),
                                onClick = { viewModel.reuploadTask() },
                                enabled = !state.isReuploading,
                                isLoading = state.isReuploading,
                                icon = Icons.Default.Sync,
                                contentDescription = stringResource(id = R.string.reupload_task)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Кнопка удаления
                            ActionButton(
                                text = stringResource(id = R.string.delete_task),
                                onClick = { viewModel.showDeleteConfirmation() },
                                enabled = !state.isDeleting,
                                isLoading = state.isDeleting,
                                icon = Icons.Default.Delete,
                                contentDescription = stringResource(id = R.string.delete_task),
                                buttonColors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            )
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
                // Подсказка для текущего действия ввода
                val currentAction = state.currentFactLineAction
                if (currentAction != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when(currentAction.type) {
                                FactLineActionType.ENTER_PRODUCT_ANY,
                                FactLineActionType.ENTER_PRODUCT_FROM_PLAN -> MaterialTheme.colorScheme.primaryContainer

                                FactLineActionType.ENTER_BIN_ANY,
                                FactLineActionType.ENTER_BIN_FROM_PLAN -> MaterialTheme.colorScheme.secondaryContainer

                                FactLineActionType.ENTER_QUANTITY -> MaterialTheme.colorScheme.tertiaryContainer
                            }
                        )
                    ) {
                        Text(
                            text = currentAction.promptText,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }

                // Поле ввода штрихкода
                BarcodeTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    onBarcodeScanned = { viewModel.processScanResultForCurrentAction(it) },
                    label = when {
                        currentAction?.type == FactLineActionType.ENTER_PRODUCT_ANY ||
                                currentAction?.type == FactLineActionType.ENTER_PRODUCT_FROM_PLAN ->
                            stringResource(id = R.string.scan_or_enter_product)

                        currentAction?.type == FactLineActionType.ENTER_BIN_ANY ||
                                currentAction?.type == FactLineActionType.ENTER_BIN_FROM_PLAN ->
                            stringResource(id = R.string.scan_or_enter_bin)

                        else -> stringResource(id = R.string.scan_or_enter_barcode)
                    },
                    enabled = state.isEditable,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Row {
                            // Кнопка отмены текущего ввода, если он активен
                            if (currentAction != null) {
                                IconButton(onClick = { viewModel.resetFactLineInputState() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(id = R.string.cancel)
                                    )
                                }
                            }

                            // Кнопка сканирования
                            IconButton(onClick = { viewModel.showScanDialog() }) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                )

                if (state.isEditable) {
                    CurrentInputStatus(state = state)
                }

                if (state.currentFactLineAction != null) {
                    FactLineActionsPanel(
                        state = state,
                        onComplete = { viewModel.completeFactLineInput() },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                                        if (state.isEditable) {
                                            lineItem.factLine?.let {
                                                viewModel.showFactLineDialog(it.productId)
                                            }
                                                ?: viewModel.showFactLineDialog(lineItem.planLine.productId)
                                        }
                                    },
                                    productProperties = productProperties
                                )
                            } else {
                                // Для заданий без плана используем упрощенную строку
                                TaskNoPlannedItemRow(
                                    lineItem = lineItem,
                                    isEditable = state.isEditable,
                                    onClick = {
                                        if (state.isEditable) {
                                            lineItem.factLine?.let {
                                                viewModel.showFactLineDialog(it.productId)
                                            }
                                                ?: viewModel.showFactLineDialog(lineItem.planLine.productId)
                                        }
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
fun FactLineActionsPanel(
    state: TaskDetailState,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            val actionColor = when(state.currentFactLineAction?.type) {
                FactLineActionType.ENTER_PRODUCT_ANY,
                FactLineActionType.ENTER_PRODUCT_FROM_PLAN ->
                    MaterialTheme.colorScheme.primaryContainer

                FactLineActionType.ENTER_BIN_ANY,
                FactLineActionType.ENTER_BIN_FROM_PLAN ->
                    MaterialTheme.colorScheme.secondaryContainer

                FactLineActionType.ENTER_QUANTITY ->
                    MaterialTheme.colorScheme.tertiaryContainer

                else -> MaterialTheme.colorScheme.surfaceVariant
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = actionColor)
            ) {
                Text(
                    text = state.currentFactLineAction?.promptText ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            // Отображение текущего действия и подсказки
            state.currentFactLineAction?.let { action ->
                Text(
                    text = action.promptText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Отображение введенных данных

            // 1. Товар
            if (state.temporaryProduct != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = R.string.product) + ":",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.temporaryProduct.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Индикатор проблемы
                    if (!state.isValidProduct) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 2. Ячейка
            if (state.temporaryBinCode != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = R.string.bin) + ":",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.formattedBinName ?: state.temporaryBinCode,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Индикатор проблемы
                    if (!state.isValidBin) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 3. Количество
            if (state.temporaryQuantity != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = R.string.quantity) + ":",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatQuantity(state.temporaryQuantity),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Кнопка завершения ввода
            val canComplete = state.temporaryProductId != null &&
                    state.temporaryQuantity != null &&
                    state.temporaryQuantity > 0f

            if (canComplete) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(stringResource(id = R.string.complete))
                }
            }
        }
    }
}

@Composable
fun CurrentInputStatus(
    state: TaskDetailState,
    modifier: Modifier = Modifier
) {
    val hasTemporaryData = state.temporaryBinCode != null || state.temporaryProduct != null

    if (!hasTemporaryData && state.currentScanHint.isEmpty()) {
        return // Ничего не отображаем, если нет данных и подсказки
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Отображаем подсказку
            if (state.currentScanHint.isNotEmpty()) {
                Text(
                    text = state.currentScanHint,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )

                if (hasTemporaryData) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Отображаем введенные данные
            if (state.temporaryBinCode != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ячейка:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = state.formattedBinName ?: state.temporaryBinCode,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (state.temporaryProduct != null) {
                if (state.temporaryBinCode != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Товар:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = state.temporaryProduct.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
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