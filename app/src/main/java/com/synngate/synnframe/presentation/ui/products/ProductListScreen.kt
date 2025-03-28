package com.synngate.synnframe.presentation.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.presentation.common.inputs.SearchTextField
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.scanner.BarcodeScannerDialog
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
    val state by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var showSortMenu by remember { mutableStateOf(false) }

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

    if (state.showBatchScannerDialog) {
        BatchScannerDialog(
            onBarcodeScanned = { barcode, onProductFound ->
                viewModel.findProductByBarcode(barcode, onProductFound)
            },
            onClose = { viewModel.finishBatchScanning() },
            onDone = { results -> viewModel.processBatchScanResults(results) }
        )
    }

    if (state.showScannerDialog) {
        BarcodeScannerDialog(
            onBarcodeScanned = { barcode ->
                // Сначала закрываем диалог и затем обрабатываем штрих-код в одном методе
                viewModel.handleScannedBarcode(barcode)
            },
            onClose = { viewModel.finishScanning() },
            productName = null
        )
    }

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
            IconButton(
                onClick = { viewModel.syncProducts() },
                enabled = !state.isSyncing
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = stringResource(id = R.string.sync_products)
                )
            }

            IconButton(onClick = { showSortMenu = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = stringResource(id = R.string.sort)
                )
            }

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
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { viewModel.startBatchScanning() },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = null
                    )
                }

                FloatingActionButton(
                    onClick = { viewModel.startScanning() },
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null
                    )
                }
            }
        },
        isLoading = state.isLoading
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = stringResource(id = R.string.search_products),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = stringResource(id = R.string.search_products_hint)
            )

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

            if (state.products.isEmpty()) {
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