package com.synngate.synnframe.presentation.ui.dynamicmenu

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
import com.synngate.synnframe.domain.mapper.DynamicProductMapper
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.inputs.SearchTextField
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.EmptyScreenContent
import com.synngate.synnframe.presentation.common.scanner.BarcodeScannerDialog
import com.synngate.synnframe.presentation.common.scanner.ScanButton
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.ScannerStatusIndicator
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.dynamicmenu.model.DynamicProductsEvent
import com.synngate.synnframe.presentation.ui.products.components.BatchScannerDialog

@Composable
fun DynamicProductsScreen(
    viewModel: DynamicProductsViewModel,
    navigateToProductDetail: (DynamicProduct) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val uiPresentation = viewModel.getUiPresentation() // Получаем готовые UI-данные

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DynamicProductsEvent.NavigateToProductDetail -> {
                    navigateToProductDetail(event.product)
                }
                is DynamicProductsEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is DynamicProductsEvent.NavigateBack -> {
                    navigateBack()
                }
                is DynamicProductsEvent.ReturnSelectedProductId -> {
                    // Обработка возврата выбранного товара (для режима выбора)
                    navigateBack()
                }
            }
        }
    }

    val scannerService = LocalScannerService.current

    ScannerListener(
        onBarcodeScanned = { barcode ->
            viewModel.handleScannedBarcode(barcode)
        }
    )

    // Диалоги
    if (state.showBatchScannerDialog) {
        BatchScannerDialog(
            onBarcodeScanned = { barcode, onProductFound ->
                viewModel.findProductByBarcode(barcode) { product ->
                    onProductFound(product?.let { DynamicProductMapper.toProduct(it) })
                }
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
        else state.menuItemName.ifEmpty { stringResource(id = R.string.products) },
        subtitle = if (uiPresentation.isSelectionMode)
            stringResource(id = R.string.select_product_for_task)
        else null,
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = uiPresentation.errorMessage?.let {
            Pair(it, StatusType.ERROR)
        },
        onDismissNotification = {
            viewModel.clearError()
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

            // Кнопка обновления
            IconButton(
                onClick = { viewModel.onRefresh() },
                enabled = !uiPresentation.isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(id = R.string.refresh)
                )
            }
        },
        floatingActionButton = {
            DynamicProductsFloatingActions(
                onBatchScanClick = { viewModel.startBatchScanning() },
                onScanClick = { viewModel.startScanning() }
            )
        },
        isLoading = uiPresentation.isLoading,
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
            // Поле поиска отображается только если оно задано в настройках экрана
            if (state.hasElement(ScreenElementType.SEARCH)) {
                SearchTextField(
                    value = uiPresentation.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = stringResource(id = R.string.search_products),
                    onSearch = { viewModel.onSearch() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = stringResource(id = R.string.search_products_hint)
                )
            }

            // Отображение количества товаров
            ProductsCountHeader(
                totalCount = uiPresentation.totalCount,
                filteredCount = uiPresentation.filteredCount
            )

            if (state.hasElement(ScreenElementType.SHOW_LIST)) {
                if (uiPresentation.products.isEmpty()) {
                    EmptyProductsList(
                        hasSearchQuery = uiPresentation.searchQuery.isNotEmpty()
                    )
                } else {
                    DynamicProductsList(
                        products = state.products,
                        onProductClick = { product ->
                            viewModel.onProductClick(product)
                        }
                    )
                }
            } else if (!state.hasElement(ScreenElementType.SEARCH)) {
                // Если нет ни поиска, ни списка
                EmptyScreenContent(
                    message = "На этом экране не указаны элементы для отображения"
                )
            }
        }
    }
}

@Composable
fun DynamicProductsFloatingActions(
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
fun DynamicProductsList(
    products: List<DynamicProduct>,
    onProductClick: (DynamicProduct) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = products,
            key = { it.id }
        ) { product ->
            DynamicProductListItem(
                product = product,
                onClick = { onProductClick(product) }
            )
        }
    }
}

@Composable
fun DynamicProductListItem(
    product: DynamicProduct,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.product_article, product.articleNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                // Основная единица измерения
                product.getMainUnit()?.let { unit ->
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stringResource(id = R.string.product_main_unit, unit.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}