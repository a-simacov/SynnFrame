package com.synngate.synnframe.presentation.ui.products

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Divider
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.ErrorScreenContent
import com.synngate.synnframe.presentation.common.scaffold.InfoRow
import com.synngate.synnframe.presentation.common.scaffold.LoadingScreenContent
import com.synngate.synnframe.presentation.common.scaffold.SectionHeader
import com.synngate.synnframe.presentation.common.scanner.BarcodeScannerDialog
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.products.components.BarcodeItem
import com.synngate.synnframe.presentation.ui.products.components.ProductUnitItem
import com.synngate.synnframe.presentation.ui.products.model.ProductDetailEvent

/**
 * Экран деталей товара
 */
@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    navigateBack: () -> Unit,
    navigateToProduct: (String) -> Unit,  // Добавляем новый параметр
    modifier: Modifier = Modifier
) {
    // Получаем состояние из ViewModel
    val state by viewModel.uiState.collectAsState()

    // Для отображения Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Обработка событий
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ProductDetailEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }

                is ProductDetailEvent.NavigateBack -> {
                    navigateBack()
                }

                is ProductDetailEvent.CopyBarcodeToClipboard -> {
                    // Обработано через Snackbar
                }

                is ProductDetailEvent.CopyProductInfoToClipboard -> {
                    // Обработано через Snackbar
                }

                is ProductDetailEvent.ToggleBarcodesPanel -> {
                    // Обработано через состояние
                }
            }
        }
    }

    // Добавим диалог сканирования штрихкода
    if (state.showBarcodeScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { barcode ->
                viewModel.handleScannedBarcode(barcode) { foundProduct ->
                    // При нахождении товара, перенаправляем на его экран
                    navigateToProduct(foundProduct.id)
                }
            },
            onClose = { viewModel.closeBarcodeScanner() },
            productName = null
        )
    }

    // Основной интерфейс
    AppScaffold(
        title = state.product?.name ?: stringResource(id = R.string.product_details),
        subtitle = state.product?.articleNumber?.let {
            stringResource(id = R.string.product_article_number, it)
        },
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        actions = {
            // Кнопка копирования информации
            IconButton(
                onClick = { viewModel.copyProductInfoToClipboard() }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(id = R.string.copy_product_info)
                )
            }
        },
        // Добавим кнопку сканирования в AppScaffold
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openBarcodeScanner() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null
                    )
                },
                text = { Text(stringResource(id = R.string.scan_other_product)) }
            )
        },
        isLoading = state.isLoading
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    LoadingScreenContent(
                        message = stringResource(id = R.string.loading_product)
                    )
                }

                state.error != null && state.product == null -> {
                    ErrorScreenContent(
                        message = state.error
                            ?: stringResource(id = R.string.error_loading_product),
                        onRetry = { viewModel.loadProduct() }
                    )
                }

                state.product != null -> {
                    // Отображаем детали товара
                    ProductDetailsContent(
                        product = state.product!!,
                        selectedUnitId = state.selectedUnitId,
                        onUnitSelected = { viewModel.selectUnit(it) },
                        isMainUnit = { viewModel.isMainUnit(it) },
                        showBarcodes = state.showBarcodes,
                        onToggleBarcodesPanel = { viewModel.toggleBarcodesPanel() },
                        showExtendedUnitInfo = state.showExtendedUnitInfo,
                        onToggleExtendedUnitInfo = { viewModel.toggleExtendedUnitInfo() },
                        onCopyBarcode = { viewModel.copyBarcodeToClipboard(it) },
                        getSelectedUnitBarcodes = { viewModel.getSelectedUnitBarcodes() },
                        isMainBarcode = { viewModel.isMainBarcode(it) }
                    )
                }
            }
        }
    }
}

/**
 * Содержимое экрана деталей товара
 */
@Composable
private fun ProductDetailsContent(
    product: Product,
    selectedUnitId: String?,
    onUnitSelected: (String) -> Unit,
    isMainUnit: (String) -> Boolean,
    showBarcodes: Boolean,
    onToggleBarcodesPanel: () -> Unit,
    showExtendedUnitInfo: Boolean,
    onToggleExtendedUnitInfo: () -> Unit,
    onCopyBarcode: (String) -> Unit,
    getSelectedUnitBarcodes: () -> List<String>,
    isMainBarcode: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Секция основной информации
        SectionHeader(title = stringResource(id = R.string.basic_info))

        // Название товара
        InfoRow(
            label = stringResource(id = R.string.product_name),
            value = product.name
        )

        // Артикул
        InfoRow(
            label = stringResource(id = R.string.article_number),
            value = product.articleNumber
        )

        // Модель учета
        val accountingModelText = when (product.accountingModel) {
            com.synngate.synnframe.domain.entity.AccountingModel.BATCH ->
                stringResource(id = R.string.accounting_model_batch)

            com.synngate.synnframe.domain.entity.AccountingModel.QTY ->
                stringResource(id = R.string.accounting_model_qty)
        }
        InfoRow(
            label = stringResource(id = R.string.accounting_model),
            value = accountingModelText
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Секция единиц измерения
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.units_of_measure),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            // Кнопка переключения расширенной информации
            IconButton(onClick = onToggleExtendedUnitInfo) {
                Icon(
                    imageVector = if (showExtendedUnitInfo)
                        Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        id = if (showExtendedUnitInfo)
                            R.string.hide_extended_info else R.string.show_extended_info
                    )
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

        // Список единиц измерения
        product.units.forEach { unit ->
            ProductUnitItem(
                unit = unit,
                isSelected = unit.id == selectedUnitId,
                isMainUnit = isMainUnit(unit.id),
                onClick = { onUnitSelected(unit.id) },
                showExtendedInfo = showExtendedUnitInfo
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка отображения штрихкодов
        OutlinedButton(
            onClick = onToggleBarcodesPanel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (showBarcodes)
                    stringResource(id = R.string.hide_barcodes)
                else stringResource(id = R.string.show_barcodes)
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = if (showBarcodes)
                    Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }

        // Секция штрихкодов
        AnimatedVisibility(
            visible = showBarcodes,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionHeader(title = stringResource(id = R.string.barcodes))

                // Штрихкоды выбранной единицы измерения
                val barcodes = getSelectedUnitBarcodes()

                if (barcodes.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.no_barcodes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    barcodes.forEach { barcode ->
                        BarcodeItem(
                            barcode = barcode,
                            isMainBarcode = isMainBarcode(barcode),
                            onCopyClick = onCopyBarcode
                        )
                    }
                }
            }
        }
    }
}

// Выносим компоненты, которые не меняются при обновлении состояния, в отдельные @Composable функции
@Composable
private fun ProductBasicInfo(
    product: Product,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Секция основной информации
        SectionHeader(title = stringResource(id = R.string.basic_info))

        // Название товара
        InfoRow(
            label = stringResource(id = R.string.product_name),
            value = product.name
        )

        // Артикул
        InfoRow(
            label = stringResource(id = R.string.article_number),
            value = product.articleNumber
        )

        // Модель учета
        val accountingModelText = when (product.accountingModel) {
            com.synngate.synnframe.domain.entity.AccountingModel.BATCH ->
                stringResource(id = R.string.accounting_model_batch)
            com.synngate.synnframe.domain.entity.AccountingModel.QTY ->
                stringResource(id = R.string.accounting_model_qty)
        }
        InfoRow(
            label = stringResource(id = R.string.accounting_model),
            value = accountingModelText
        )
    }
}

// В ProductDetailScreen
@Composable
private fun BarcodesList(
    barcodes: List<String>,
    isMainBarcode: (String) -> Boolean,
    onCopyBarcode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (barcodes.isEmpty()) {
            Text(
                text = stringResource(id = R.string.no_barcodes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            barcodes.forEach { barcode ->
                BarcodeItem(
                    barcode = barcode,
                    isMainBarcode = isMainBarcode(barcode),
                    onCopyClick = onCopyBarcode
                )
            }
        }
    }
}