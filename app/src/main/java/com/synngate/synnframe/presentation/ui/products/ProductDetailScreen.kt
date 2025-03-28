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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.ProductUnit
import com.synngate.synnframe.presentation.common.scaffold.AppScaffold
import com.synngate.synnframe.presentation.common.scaffold.ErrorScreenContent
import com.synngate.synnframe.presentation.common.scaffold.InfoRow
import com.synngate.synnframe.presentation.common.scaffold.LoadingScreenContent
import com.synngate.synnframe.presentation.common.scaffold.SectionHeader
import com.synngate.synnframe.presentation.common.status.StatusType
import com.synngate.synnframe.presentation.ui.products.components.BarcodeItem
import com.synngate.synnframe.presentation.ui.products.components.ProductUnitItem
import com.synngate.synnframe.presentation.ui.products.model.ProductDetailEvent

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

                state.product != null -> {
                    ProductDetailsContent(
                        product = state.product!!,
                        isMainUnit = { viewModel.isMainUnit(it) },
                        onCopyBarcode = { viewModel.copyBarcodeToClipboard(it) },
                        isMainBarcode = { viewModel.isMainBarcode(it) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductDetailPreview() {
    ProductDetailsContent(
        product = Product(
            "0000000003309",
            "Promo Мыло жидкое Clea Ocena 400 гр + Влажная салфетка 15 buc Antibacterial",
            AccountingModel.QTY,
            "Promo5",
            "000000001",
            listOf(
                ProductUnit(
                    "000000001",
                    "0000000003309",
                    "шт",
                    1.0f,
                    mainBarcode = "2009010005966",
                    barcodes = listOf("2009010005966", "2009010005966")
                ),
                ProductUnit(
                    "000000002",
                    "0000000003309",
                    "кор",
                    10f,
                    mainBarcode = "2009010005967",
                    barcodes = listOf("2009010005967")
                )
            )
        ),
        isMainUnit = { it == "000000001" },
        onCopyBarcode = {},
        isMainBarcode = { true }
    )
}

@Composable
private fun ProductDetailsContent(
    product: Product,
    isMainUnit: (String) -> Boolean,
    onCopyBarcode: (String) -> Unit,
    isMainBarcode: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ProductBasicInfo(product = product)

        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = stringResource(id = R.string.units_of_measure))

        product.units.forEach { unit ->
            ProductUnitItem(
                unit = unit,
                isMainUnit = isMainUnit(unit.id)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SectionHeader(title = stringResource(id = R.string.barcodes))

        val barcodes = product.getAllBarcodes()//units.flatMap { it.barcodes }

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

@Composable
private fun ProductBasicInfo(
    product: Product,
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
            label = stringResource(id = R.string.product_id),
            value = product.id
        )

        InfoRow(
            label = stringResource(id = R.string.article_number),
            value = product.articleNumber
        )

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