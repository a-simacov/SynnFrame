package com.synngate.synnframe.presentation.ui.dynamicmenu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.operation.DynamicProduct
import com.synngate.synnframe.domain.entity.operation.ScreenElementType
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
import com.synngate.synnframe.presentation.ui.products.ProductsList
import com.synngate.synnframe.presentation.ui.products.components.BatchScannerDialog

@Composable
fun DynamicProductsScreen(
    viewModel: DynamicProductsViewModel,
    navigateToProductDetail: (DynamicProduct) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val uiPresentation = viewModel.getUiPresentation() // Получаем готовое UI-представление

    val snackbarHostState = remember { SnackbarHostState() }

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
                    // Этот функционал будет реализован при интеграции с другими экранами
                    navigateBack()
                }
            }
        }
    }

    val scannerService = LocalScannerService.current

    // Настраиваем обработчик штрихкодов от внешнего сканера
    ScannerListener(
        onBarcodeScanned = { barcode ->
            viewModel.handleScannedBarcode(barcode)
        }
    )

    // Диалоги сканирования
    if (state.showBatchScannerDialog) {
        BatchScannerDialog(
            onBarcodeScanned = { barcode, onProductFound ->
                viewModel.findProductByBarcode(barcode) {
                    // При поиске товара нужно привести DynamicProduct к Product
                    onProductFound(null) // Пока заглушка
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
        title = state.menuItemName.ifEmpty { stringResource(id = R.string.dynamic_products_title) },
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        onDismissNotification = {
            viewModel.clearError()
        },
        actions = {
            // Добавляем статус сканера, если он доступен
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
            IconButton(onClick = { viewModel.onRefresh() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(id = R.string.refresh)
                )
            }
        },
        floatingActionButton = {
            // Показываем кнопку сканирования
            FloatingActionButton(
                onClick = { viewModel.startScanning() }
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = stringResource(id = R.string.scan_barcode)
                )
            }
        },
        isLoading = uiPresentation.isLoading
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Строка поиска, если нужна
            if (state.hasElement(ScreenElementType.SEARCH)) {
                SearchTextField(
                    value = uiPresentation.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = stringResource(id = R.string.search_products),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = stringResource(id = R.string.search_products_hint)
                )
            }

            // Заголовок с количеством товаров
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(
                        id = R.string.products_count,
                        uiPresentation.filteredCount,
                        uiPresentation.totalCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Отображение списка
            if (state.hasElement(ScreenElementType.SHOW_LIST)) {
                if (uiPresentation.products.isEmpty()) {
                    EmptyScreenContent(
                        message = if (uiPresentation.searchQuery.isNotEmpty())
                            stringResource(id = R.string.no_products_with_filter)
                        else
                            stringResource(id = R.string.no_products)
                    )
                } else {
                    // Используем существующий компонент для отображения списка
                    ProductsList(
                        products = uiPresentation.products,
                        onProductClick = { productId ->
                            val product = state.products.firstOrNull { it.id == productId }
                            if (product != null) {
                                viewModel.onProductClick(product)
                            }
                        }
                    )
                }
            }

            // Если нет элементов для отображения
            if (!state.hasElement(ScreenElementType.SEARCH) && !state.hasElement(ScreenElementType.SHOW_LIST)) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Не указаны элементы для отображения на этом экране",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}