package com.synngate.synnframe.presentation.common.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synngate.synnframe.R
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.presentation.common.inputs.SearchTextField
import com.synngate.synnframe.presentation.common.scanner.ScanButton
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Оптимизированный диалог выбора товара с использованием ViewModel для управления данными
 */
@Composable
fun OptimizedProductSelectionDialog(
    onProductSelected: (Product) -> Unit,
    onDismiss: () -> Unit,
    initialFilter: String = "",
    title: String = stringResource(id = R.string.select_product),
    planProductIds: Set<String>? = null,
    modifier: Modifier = Modifier,
    // Параметр для инъекции репозитория (для тестирования)
    productRepository: ProductRepository? = null
) {
    // Получаем репозиторий продуктов
    val repo = productRepository ?: run {
        val appContext = LocalContext.current.applicationContext as? SynnFrameApplication
        appContext?.appContainer?.productRepository
    }

    // Если репозиторий не доступен, показываем сообщение об ошибке
    if (repo == null) {
        ErrorDialog(
            message = "Не удалось инициализировать диалог: репозиторий продуктов недоступен",
            onDismiss = onDismiss
        )
        return
    }

    // Создаем ViewModel с фабрикой
    val viewModel: ProductSelectionDialogViewModel = viewModel(
        factory = ProductSelectionDialogViewModel.Factory(repo, planProductIds)
    )

    // Получаем состояние UI из ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Обрабатываем входной фильтр
    LaunchedEffect(initialFilter) {
        if (initialFilter.isNotEmpty()) {
            viewModel.updateSearchQuery(initialFilter)
        }
    }

    // Запрашиваем фокус для поля поиска
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(100) // Небольшая задержка для правильной работы
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при запросе фокуса")
        }
    }

    // Слушатель сканера штрихкодов
    ScannerListener(
        onBarcodeScanned = { barcode ->
            viewModel.findProductByBarcode(barcode) { product ->
                if (product != null) {
                    onProductSelected(product)
                    onDismiss()
                }
            }
        }
    )

    // Показываем диалог сканера, если он активирован
    if (uiState.showScanner) {
        UniversalScannerDialog(
            onBarcodeScanned = { barcode ->
                viewModel.findProductByBarcode(barcode) { product ->
                    if (product != null) {
                        onProductSelected(product)
                        onDismiss()
                    }
                }
                viewModel.setShowScanner(false)
            },
            onClose = {
                viewModel.setShowScanner(false)
            },
            instructionText = stringResource(id = R.string.scan_product)
        )
    }

    // Основной диалог выбора продукта
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Заголовок диалога
                DialogHeader(
                    title = title,
                    onScanClick = { viewModel.setShowScanner(true) },
                    onClose = onDismiss
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Поле поиска
                SearchTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = stringResource(id = R.string.search_by_name_article_barcode),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = stringResource(id = R.string.search_product_hint),
                    onSearch = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            viewModel.findProductByBarcode(uiState.searchQuery) { product ->
                                if (product != null) {
                                    onProductSelected(product)
                                    onDismiss()
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Основное содержимое диалога
                DialogContent(
                    uiState = uiState,
                    onProductClick = { product ->
                        onProductSelected(product)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопка закрытия
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        }
    }

    // Очистка ресурсов при закрытии диалога
    DisposableEffect(Unit) {
        onDispose {
            // В будущем здесь могут быть дополнительные действия по очистке ресурсов
        }
    }
}

@Composable
private fun DialogHeader(
    title: String,
    onScanClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f)
        )

        ScanButton(
            onClick = onScanClick
        )

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(id = R.string.close)
            )
        }
    }
}

@Composable
private fun DialogContent(
    uiState: ProductSelectionUiState,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            uiState.products.isEmpty() && uiState.searchQuery.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Введите текст для поиска",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            uiState.products.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.no_products_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                ProductsList(
                    products = uiState.products,
                    onProductClick = onProductClick
                )
            }
        }
    }
}

@Composable
private fun ProductsList(
    products: List<Product>,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(
            items = products,
            key = { it.id }
        ) { product ->
            ProductItem(
                product = product,
                onClick = { onProductClick(product) }
            )
        }
    }
}

@Composable
private fun ProductItem(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
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
                text = stringResource(id = R.string.product_id_fmt, product.id),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(id = R.string.article_fmt, product.articleNumber),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Отображаем основную единицу измерения и штрихкод
        product.getMainUnit()?.let { mainUnit ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.unit_fmt, mainUnit.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                if (mainUnit.mainBarcode.isNotEmpty()) {
                    Card(
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = mainUnit.mainBarcode,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    CustomAlertDialog(
        title = "Ошибка",
        text = message,
        onDismiss = onDismiss
    )
}