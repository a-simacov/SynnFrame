package com.synngate.synnframe.presentation.ui.dynamicmenu.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.mapper.DynamicProductMapper
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scanner.BarcodeScannerDialog
import com.synngate.synnframe.presentation.common.scanner.ScanButton
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.ScannerStatusIndicator
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.createComponentGroups
import com.synngate.synnframe.presentation.ui.dynamicmenu.components.rememberGenericScreenComponentRegistry
import com.synngate.synnframe.presentation.ui.dynamicmenu.product.component.initializeProductComponents
import com.synngate.synnframe.presentation.ui.dynamicmenu.product.model.DynamicProductsEvent
import com.synngate.synnframe.presentation.ui.dynamicmenu.product.model.DynamicProductsState
import com.synngate.synnframe.presentation.ui.products.components.BatchScannerDialog

@Composable
fun DynamicProductsScreen(
    viewModel: DynamicProductsViewModel,
    navigateToProductDetail: (DynamicProduct) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Создаем и инициализируем универсальный реестр компонентов
    val componentRegistry = rememberGenericScreenComponentRegistry<DynamicProductsState>()

    // Инициализируем реестр для товаров
    LaunchedEffect(Unit) {
        componentRegistry.initializeProductComponents(
            productsProvider = { it.products },
            isLoadingProvider = { it.isLoading },
            errorProvider = { it.error },
            onProductClickProvider = { { product -> viewModel.onProductClick(product) } },
            searchValueProvider = { it.searchValue },
            onSearchValueChangedProvider = { { value -> viewModel.updateSearchQuery(value) } },
            onSearchProvider = { { viewModel.onSearch() } }
        )
    }

    // Получаем сгруппированные компоненты
    val componentGroups = createComponentGroups(state, componentRegistry)

    // Добавляем обработчики сканирования для диалогов
    val scannerService = LocalScannerService.current

    ScannerListener(
        onBarcodeScanned = { barcode ->
            viewModel.handleScannedBarcode(barcode)
        }
    )

    // Диалоги сканирования
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

    // Обработка событий навигации
    LaunchedEffect(key1 = viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DynamicProductsEvent.NavigateToProductDetail -> {
                    navigateToProductDetail(event.product)
                }
                is DynamicProductsEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                is DynamicProductsEvent.NavigateBack -> {
                    navigateBack()
                }
                is DynamicProductsEvent.ReturnSelectedProductId -> {
                    // Специфичная логика для режима выбора товара
                    navigateBack()
                }
            }
        }
    }

    AppScaffold(
        title = if (state.isSelectionMode)
            stringResource(id = R.string.select_product)
        else state.menuItemName.ifEmpty { stringResource(id = R.string.products) },
        subtitle = if (state.isSelectionMode)
            stringResource(id = R.string.select_product_for_task)
        else null,
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
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
                        scannerService.triggerScan()
                    }
                )
            }

            // Кнопка обновления
            IconButton(
                onClick = { viewModel.onRefresh() },
                enabled = !state.isLoading
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
        isLoading = state.isLoading,
        bottomBar = {
            if (state.isSelectionMode && state.selectedProduct != null) {
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
                .padding(horizontal = 16.dp)
        ) {
            // Отображаем компоненты с фиксированным размером
            componentGroups.fixedComponents.forEach { component ->
                component.Render(Modifier.fillMaxWidth())
            }

            // Отображаем компоненты с весом
            componentGroups.weightedComponents.forEach { component ->
                component.Render(
                    Modifier
                        .fillMaxWidth()
                        .weight(component.getWeight())
                )
            }

            // Оставляем здесь только плавающие кнопки действий
        }
    }
}

// Оставляем имеющуюся функцию для плавающих действий
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