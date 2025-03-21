package com.synngate.synnframe.presentation.ui.products

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.presentation.common.filter.FilterPanel
import com.synngate.synnframe.presentation.common.inputs.SearchTextField
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.scaffold.LoadingScreenContent
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.products.components.BatchScannerDialog
import com.synngate.synnframe.presentation.ui.products.components.ProductListItem
import com.synngate.synnframe.presentation.ui.products.model.ProductListEvent
import com.synngate.synnframe.presentation.ui.products.model.SortOrder

@Composable
fun ProductListScreen(
    viewModel: ProductListViewModel,
    navigateToProductDetail: (String) -> Unit,
    navigateBack: () -> Unit,
    returnProductToTask: ((Product) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Получаем состояние из ViewModel
    val state by viewModel.uiState.collectAsState()

    // Для отображения Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Состояние диалога сканирования штрихкода
    var showScannerDialog by remember { mutableStateOf(false) }

    // Состояние меню сортировки
    var showSortMenu by remember { mutableStateOf(false) }

    // Обработка событий
    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ProductListEvent.NavigateToProductDetail -> {
                    navigateToProductDetail(event.productId)
                }

                is ProductListEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }

                is ProductListEvent.NavigateBack -> {
                    navigateBack()
                }

                is ProductListEvent.ReturnToTaskWithProduct -> {
                    returnProductToTask?.invoke(event.product)
                }
            }
        }
    }

    // Фрагмент кода с исправленным вызовом BatchScannerDialog
// Добавим диалог пакетного сканирования
    if (state.showBatchScannerDialog) {
        BatchScannerDialog(
            // Теперь функция не должна возвращать значение
            onBarcodeScanned = { barcode ->
                // Запускаем поиск в ViewModel без попытки возвращать результат
                viewModel.findProductByBarcode(barcode)
            },
            onClose = { viewModel.finishBatchScanning() },
            onDone = { results -> viewModel.processBatchScanResults(results) }
        )
    }

    // Основной интерфейс
    AppScaffold(
        title = if (state.isSelectionMode)
            stringResource(id = R.string.select_product)
        else stringResource(id = R.string.products),
        subtitle = if (state.isSelectionMode)
            stringResource(id = R.string.select_product_for_task)
        else null,
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        isSyncing = state.isSyncing,
        lastSyncTime = state.lastSyncTime,
        actions = {
            // Кнопка синхронизации с сервером
            IconButton(
                onClick = { viewModel.syncProducts() },
                enabled = !state.isSyncing
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = stringResource(id = R.string.sync_products)
                )
            }

            // Кнопка сортировки
            IconButton(onClick = { showSortMenu = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(id = R.string.sort)
                )
            }

            // Меню сортировки
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                SortOrder.entries.forEach { sortOption ->
                    DropdownMenuItem(
                        text = { Text(sortOption.getDisplayName()) },
                        onClick = {
                            viewModel.updateSortOrder(sortOption)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (state.sortOrder == sortOption) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            }
        },
        // Добавим в AppScaffold дополнительную кнопку действия
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Кнопка пакетного сканирования
                ExtendedFloatingActionButton(
                    onClick = { viewModel.startBatchScanning() },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = null
                        )
                    },
                    text = { Text(stringResource(id = R.string.batch_scanning)) }
                )

                // Кнопка сканирования одиночного штрихкода
                ExtendedFloatingActionButton(
                    onClick = { showScannerDialog = true },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null
                        )
                    },
                    text = { Text(stringResource(id = R.string.scan_barcode)) }
                )
            }
        },
        isLoading = state.isLoading
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Поле поиска
            SearchTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = stringResource(id = R.string.search_products),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = stringResource(id = R.string.search_products_hint)
            )

            // Кнопка отображения/скрытия фильтров
            OutlinedButton(
                onClick = { viewModel.toggleFilterPanel() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (state.showFilterPanel)
                        stringResource(id = R.string.hide_filters)
                    else stringResource(id = R.string.show_filters)
                )
            }

            // Панель фильтров
            AnimatedVisibility(visible = state.showFilterPanel) {
                FilterPanel(
                    isVisible = true,
                    onVisibilityChange = { viewModel.toggleFilterPanel() }
                ) {
                    // Фильтр по модели учета
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(id = R.string.filter_accounting_model),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Опция "Все модели"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = state.filterByAccountingModel == null,
                                onClick = { viewModel.updateAccountingModelFilter(null) }
                            )
                            Text(
                                text = stringResource(id = R.string.all_models),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Опция "По партиям и количеству"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = state.filterByAccountingModel == AccountingModel.BATCH,
                                onClick = { viewModel.updateAccountingModelFilter(AccountingModel.BATCH) }
                            )
                            Text(
                                text = stringResource(id = R.string.accounting_model_batch),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Опция "Только по количеству"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = state.filterByAccountingModel == AccountingModel.QTY,
                                onClick = { viewModel.updateAccountingModelFilter(AccountingModel.QTY) }
                            )
                            Text(
                                text = stringResource(id = R.string.accounting_model_qty),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Кнопка сброса фильтров
                        Button(
                            onClick = { viewModel.resetFilters() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(id = R.string.reset_filters))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            // Отображение количества товаров
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(
                        id = R.string.products_count,
                        state.products.size,
                        state.productsCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Список товаров
            if (state.isLoading) {
                LoadingScreenContent(message = stringResource(id = R.string.loading_products))
            } else if (state.products.isEmpty()) {
                EmptyScreenContent(
                    message = if (state.searchQuery.isNotEmpty() || state.filterByAccountingModel != null)
                        stringResource(id = R.string.no_products_with_filter)
                    else
                        stringResource(id = R.string.no_products)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.products,
                        key = { it.id } // Добавляем ключ для оптимизации
                    ) { product ->
                        ProductListItem(
                            product = product,
                            onClick = { viewModel.onProductClick(product) },
                            isSelected = state.selectedProduct?.id == product.id && state.isSelectionMode,
                            isSelectionMode = state.isSelectionMode
                        )
                    }

                }
            }
        }
        if (state.isSelectionMode && state.selectedProduct != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { viewModel.confirmProductSelection() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.confirm_selection))
                }
            }
        }
    }
}