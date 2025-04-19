package com.synngate.synnframe.presentation.ui.products

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.inputs.SearchTextField
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.scanner.BarcodeScannerDialog
import com.synngate.synnframe.presentation.common.scanner.ScanButton
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.ScannerStatusIndicator
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.products.components.BatchScannerDialog
import com.synngate.synnframe.presentation.ui.products.model.ProductListEvent
import com.synngate.synnframe.presentation.ui.products.model.ProductListItemUiModel
import com.synngate.synnframe.presentation.ui.products.model.SortOrder
import timber.log.Timber

@Composable
fun ProductListScreen(
    viewModel: ProductListViewModel,
    navigateToProductDetail: (String) -> Unit,
    navigateBack: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val uiPresentation = viewModel.getUiPresentation() // Получаем готовые UI-данные

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
                is ProductListEvent.ReturnSelectedProductId -> {
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "selected_product_id", event.productId
                    )
                    Timber.d("Saving selected product ID to savedStateHandle: ${event.productId}")
                    navigateBack()
                }
            }
        }
    }

    val scannerService = LocalScannerService.current

    ScannerListener(
        onBarcodeScanned = { barcode ->
            viewModel.findProductByBarcode(barcode) { product ->
                if (product != null) {
                    navigateToProductDetail(product.id)
                }
            }
        }
    )


    // Диалоги без изменений
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
                viewModel.handleScannedBarcode(barcode)
            },
            onClose = { viewModel.finishScanning() },
            productName = null
        )
    }

    AppScaffold(
        title = if (uiPresentation.isSelectionMode)
            stringResource(id = R.string.select_product)
        else stringResource(id = R.string.products),
        subtitle = if (uiPresentation.isSelectionMode)
            stringResource(id = R.string.select_product_for_task)
        else null,
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = uiPresentation.errorMessage?.let {
            Pair(it, StatusType.ERROR)
        },
        actions = {
            scannerService?.let { service ->
                ScannerStatusIndicator(
                    scannerService = service,
                    showText = false,
                    showIcon = true
                )
                ScanButton(
                    onClick = {
                        // Программно запускаем сканирование
                        scannerService.triggerScan()
                    }
                )
            }

            // Заменяем простую кнопку на кнопку с индикатором прогресса
            IconButton(
                onClick = { viewModel.syncProducts() },
                enabled = !uiPresentation.isLoading && !state.isSyncing // Отключаем во время загрузки и синхронизации
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = stringResource(id = R.string.sync_products)
                        )
                    }
                }
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
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            ProductListFloatingActions(
                onBatchScanClick = { viewModel.startBatchScanning() },
                onScanClick = { viewModel.startScanning() }
            )
        },
        isLoading = uiPresentation.isLoading,
        isSyncing = state.isSyncing,  // Передаем флаг синхронизации в AppScaffold
        lastSyncTime = state.lastSyncTime,  // Передаем время последней синхронизации
        bottomBar = {
            if (uiPresentation.showConfirmButton) {
                Button(
                    onClick = { viewModel.confirmProductSelection() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(stringResource(id = R.string.confirm_selection))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchTextField(
                value = uiPresentation.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = stringResource(id = R.string.search_products),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = stringResource(id = R.string.search_products_hint)
            )

            ProductsCountHeader(
                totalCount = uiPresentation.totalCount,
                filteredCount = uiPresentation.filteredCount
            )

            if (uiPresentation.products.isEmpty()) {
                EmptyProductsList(
                    hasSearchQuery = uiPresentation.searchQuery.isNotEmpty()
                )
            } else {
                ProductsList(
                    products = uiPresentation.products,
                    onProductClick = { productId ->
                        viewModel.onProductClick(state.products.first { it.id == productId })
                    }
                )
            }
        }
    }
}

@Composable
fun ProductListFloatingActions(
    onBatchScanClick: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        // Кнопка для пакетного сканирования
        FloatingActionButton(
            onClick = onBatchScanClick
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ViewList,
                contentDescription = stringResource(id = R.string.batch_scanning)
            )
        }

        // Кнопка для обычного сканирования
        FloatingActionButton(
            onClick = onScanClick
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = stringResource(id = R.string.scan_barcode)
            )
        }
    }
}

@Composable
fun ProductsCountHeader(
    totalCount: Int,
    filteredCount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(
                id = R.string.products_count,
                filteredCount,
                totalCount
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyProductsList(
    hasSearchQuery: Boolean,
    modifier: Modifier = Modifier
) {
    EmptyScreenContent(
        message = if (hasSearchQuery)
            stringResource(id = R.string.no_products_with_filter)
        else
            stringResource(id = R.string.no_products),
        modifier = modifier
    )
}

@Composable
fun ProductsList(
    products: List<ProductListItemUiModel>,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = products,
            key = { it.id }
        ) { product ->
            ProductListItem(
                name = product.name,
                articleText = product.articleText,
                mainUnitText = product.mainUnitText,
                isSelected = product.isSelected,
                onClick = { onProductClick(product.id) }
            )
        }
    }
}

@Composable
fun ProductListItem(
    name: String,
    articleText: String,
    mainUnitText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isSelected)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = articleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = mainUnitText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}