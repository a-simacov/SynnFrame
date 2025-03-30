package com.synngate.synnframe.presentation.ui.products

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.ErrorScreenContent
import com.synngate.synnframe.presentation.common.scaffold.InfoRow
import com.synngate.synnframe.presentation.common.scaffold.LoadingScreenContent
import com.synngate.synnframe.presentation.common.scaffold.SectionHeader
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.products.components.BarcodeItem
import com.synngate.synnframe.presentation.ui.products.components.ProductUnitItem
import com.synngate.synnframe.presentation.ui.products.model.BarcodeUiModel
import com.synngate.synnframe.presentation.ui.products.model.ProductDetailEvent
import com.synngate.synnframe.presentation.ui.products.model.ProductDetailUiModel
import com.synngate.synnframe.presentation.ui.products.model.ProductUnitUiModel

@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = viewModel) {
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

                else -> { /* обработка других событий */
                }
            }
        }
    }

    AppScaffold(
        title = stringResource(id = R.string.product_details),
        onNavigateBack = navigateBack,
        snackbarHostState = snackbarHostState,
        notification = state.error?.let {
            Pair(it, StatusType.ERROR)
        },
        actions = {
            IconButton(
                onClick = { viewModel.copyProductInfoToClipboard() }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(id = R.string.copy_product_info)
                )
            }
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

                else -> {
                    val productUiModel = viewModel.getProductUiModel()
                    if (productUiModel != null) {
                        ProductDetailsContent(
                            productUiModel = productUiModel,
                            selectedUnitUiModels = viewModel.getSelectedUnitUiModels(),
                            unitBarcodes = viewModel.getAllBarcodesUiModels(),
                            onUnitSelected = { viewModel.selectUnit(it) },
                            onCopyBarcode = { viewModel.copyBarcodeToClipboard(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductDetailsContent(
    productUiModel: ProductDetailUiModel,
    selectedUnitUiModels: List<ProductUnitUiModel>,
    unitBarcodes: List<BarcodeUiModel>,
    onUnitSelected: (String) -> Unit,
    onCopyBarcode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ProductBasicInfoSection(product = productUiModel)

        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = stringResource(id = R.string.units_of_measure))

        productUiModel.units.forEach { unit ->
            ProductUnitItem(
                unit = unit,
                isSelected = selectedUnitUiModels.any { it.id == unit.id },
                onClick = { onUnitSelected(unit.id) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = stringResource(id = R.string.barcodes))

        BarcodesList(
            barcodes = unitBarcodes,
            onCopyBarcode = onCopyBarcode
        )
    }
}

@Composable
private fun ProductBasicInfoSection(
    product: ProductDetailUiModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(title = stringResource(id = R.string.basic_info))

        Text(
            text = product.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        HorizontalDivider()

        InfoRow(
            label = stringResource(id = R.string.product_id_title),
            value = product.id
        )

        InfoRow(
            label = stringResource(id = R.string.article_number),
            value = product.articleText
        )

        InfoRow(
            label = stringResource(id = R.string.accounting_model),
            value = product.accountingModelText
        )
    }
}

@Composable
private fun BarcodesList(
    barcodes: List<BarcodeUiModel>,
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
            barcodes.forEach { barcodeModel ->
                BarcodeItem(
                    barcode = barcodeModel.barcode,
                    isMainBarcode = barcodeModel.isMainBarcode,
                    onCopyClick = onCopyBarcode
                )
            }
        }
    }
}