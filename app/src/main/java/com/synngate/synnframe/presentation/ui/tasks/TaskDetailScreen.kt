package com.synngate.synnframe.presentation.ui.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.entity.TaskType
import com.synngate.synnframe.presentation.common.dialog.ConfirmationDialog
import com.synngate.synnframe.presentation.common.inputs.BarcodeTextField
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.ErrorScreenContent
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.tasks.components.ScanBarcodeDialog
import com.synngate.synnframe.presentation.ui.tasks.components.TaskFactLineDialog
import com.synngate.synnframe.presentation.ui.tasks.components.TaskLineItemRow
import com.synngate.synnframe.presentation.ui.tasks.model.TaskDetailEvent
import com.synngate.synnframe.presentation.util.formatQuantity
import java.time.format.DateTimeFormatter

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

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    val updateSuccessMessage = stringResource(id = R.string.update_success)

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

    if (state.isScanDialogVisible) {
        ScanBarcodeDialog(
            onBarcodeScanned = { barcode -> viewModel.processScanResult(barcode) },
            onQuantityChange = { factLine, additionalQuantity ->
                viewModel.applyQuantityChange(factLine, additionalQuantity)
            },
            onClose = { viewModel.closeDialog() },
            scannedProduct = state.scannedProduct,
            selectedFactLine = state.selectedFactLine,
            scannedBarcode = state.scannedBarcode,
            dialogState = state.scanBarcodeDialogState,
            onQuantityInputChange = { viewModel.updateScanDialogAdditionalQuantity(it) },
            onQuantityError = { viewModel.setScanDialogInputError(it) },
            onScannerActiveChange = { viewModel.toggleScannerActive(it) }
        )
    }

    if (state.isFactLineDialogVisible && state.selectedFactLine != null) {
        val product = state.taskLines.find { it.factLine?.id == state.selectedFactLine!!.id }?.product
        TaskFactLineDialog(
            factLine = state.selectedFactLine!!,
            product = product,
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
    val screenTitle = task?.let {
        val taskType = when (it.type) {
            TaskType.RECEIPT -> stringResource(id = R.string.task_type_receipt)
            TaskType.PICK -> stringResource(id = R.string.task_type_pick)
        }
        "$taskType (${it.barcode})"
    } ?: ""

    // Обработка возвращаемого значения из экрана выбора товара
    LaunchedEffect(Unit) {
        val savedStateHandle = navBackStackEntry?.savedStateHandle
        savedStateHandle?.get<Product>("selected_product")?.let { product ->
            // Обрабатываем выбранный товар
            viewModel.handleSelectedProduct(product)
            // Удаляем данные, чтобы избежать повторной обработки
            savedStateHandle.remove<Product>("selected_product")
        }
    }

    AppScaffold(
        title = screenTitle,
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
                // Поле для ввода штрихкода товара (активно только если задание в процессе выполнения)
                BarcodeTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    onBarcodeScanned = { viewModel.processScanResult(it) },
                    label = stringResource(id = R.string.scan_or_enter_barcode),
                    enabled = state.isEditable,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(
                            onClick = { viewModel.showScanDialog() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.product),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(0.4f)
                    )

                    Text(
                        text = stringResource(id = R.string.plan_quantity),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.3f)
                    )

                    Text(
                        text = stringResource(id = R.string.fact_quantity),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.3f)
                    )
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
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(
                            items = state.taskLines,
                            key = { it.planLine.id }
                        ) { lineItem ->
                            TaskLineItemRow(
                                lineItem = lineItem,
                                isEditable = state.isEditable,
                                onClick = {
                                    if (state.isEditable) {
                                        lineItem.factLine?.let {
                                            viewModel.showFactLineDialog(it.productId)
                                        } ?: viewModel.showFactLineDialog(lineItem.planLine.productId)
                                    }
                                }
                            )
                        }
                    }
                }

                if (state.taskLines.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.total),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            modifier = Modifier.weight(0.4f)
                        )

                        val totalPlan = task.getTotalPlanQuantity()
                        Text(
                            text = formatQuantity(totalPlan),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.3f)
                        )

                        val totalFact = task.getTotalFactQuantity()
                        Text(
                            text = formatQuantity(totalFact),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (totalFact >= totalPlan)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(0.3f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(
                            id = R.string.task_completion_percentage,
                            task.getCompletionPercentage()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}