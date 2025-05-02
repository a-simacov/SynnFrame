package com.synngate.synnframe.presentation.common.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.synngate.synnframe.R
import com.synngate.synnframe.SynnFrameApplication
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.presentation.common.inputs.SearchTextField
import com.synngate.synnframe.presentation.common.scanner.ScanButton
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Оптимизированный диалог выбора товара с прямым доступом к репозиторию
 * и эффективной пагинацией для больших списков
 */
@Composable
fun OptimizedProductSelectionDialog(
    onProductSelected: (Product) -> Unit,
    onDismiss: () -> Unit,
    initialFilter: String = "",
    isSelectionMode: Boolean = true,
    title: String = stringResource(id = R.string.select_product),
    planProductIds: Set<String>? = null,
    modifier: Modifier = Modifier
) {
    // Получаем репозиторий продуктов из контекста приложения
    val appContext = LocalContext.current.applicationContext as? SynnFrameApplication
    val productRepository = appContext?.appContainer?.productRepository

    // Если репозиторий не доступен, показываем обычный диалог
    if (productRepository == null) {
        ProductSelectionDialog(
            products = emptyList(),
            onProductSelected = onProductSelected,
            onDismiss = onDismiss,
            initialFilter = initialFilter,
            isLoading = true,
            title = title,
            planProductIds = planProductIds
        )
        return
    }

    // Состояния для оптимизированного диалога
    var filter by remember { mutableStateOf(initialFilter) }
    var isLoading by remember { mutableStateOf(false) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var visibleProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var hasMoreData by remember { mutableStateOf(true) }
    var searchJob: Job? by remember { mutableStateOf(null) }
    var showScanner by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Определяем, находимся ли мы в конце списка
    val endOfListReached by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.isNotEmpty() &&
                    listState.layoutInfo.visibleItemsInfo.last().index >= products.size - 5
        }
    }

    // Функция для обновления отображаемых продуктов в соответствии с planProductIds
    val updateVisibleProducts = { allProducts: List<Product>, planIds: Set<String>? ->
        visibleProducts = if (planIds != null && planIds.isNotEmpty()) {
            allProducts.filter { planIds.contains(it.id) }
        } else {
            allProducts
        }
    }

    // Загружаем больше продуктов при достижении конца списка
    LaunchedEffect(endOfListReached) {
        if (endOfListReached && hasMoreData && !isLoading && products.isNotEmpty()) {
            loadMoreProducts(
                productRepository,
                filter,
                products,
                onDataLoaded = { newData, hasMore ->
                    products = newData
                    updateVisibleProducts(newData, planProductIds)
                    hasMoreData = hasMore
                    isLoading = false
                },
                onLoading = { isLoading = it }
            )
        }
    }

    // Загружаем начальные данные при открытии диалога или изменении фильтра
    LaunchedEffect(filter) {
        searchJob?.cancel()
        searchJob = coroutineScope.launch {
            delay(300) // Небольшая задержка для предотвращения частых запросов

            isLoading = true
            try {
                // Загружаем первую страницу данных
                initialLoadProducts(
                    productRepository,
                    filter,
                    onDataLoaded = { initialData, hasMore ->
                        products = initialData
                        updateVisibleProducts(initialData, planProductIds)
                        hasMoreData = hasMore
                        isLoading = false
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при загрузке продуктов")
                isLoading = false
            }
        }
    }

    // Запрашиваем фокус при открытии диалога
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
            coroutineScope.launch {
                try {
                    val product = productRepository.findProductByBarcode(barcode)
                    if (product != null) {
                        onProductSelected(product)
                        onDismiss()
                    } else {
                        Timber.w("Продукт со штрихкодом $barcode не найден")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Ошибка при поиске продукта по штрихкоду: $barcode")
                }
            }
        }
    )

    // Показываем диалог сканера, если он активирован
    if (showScanner) {
        UniversalScannerDialog(
            onBarcodeScanned = { barcode ->
                coroutineScope.launch {
                    try {
                        val product = productRepository.findProductByBarcode(barcode)
                        if (product != null) {
                            onProductSelected(product)
                            onDismiss()
                        } else {
                            Timber.w("Продукт со штрихкодом $barcode не найден")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка при поиске продукта по штрихкоду: $barcode")
                    }
                }
                showScanner = false
            },
            onClose = {
                showScanner = false
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    ScanButton(
                        onClick = { showScanner = true }
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.close)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Поле поиска по имени, артикулу или штрихкоду
                SearchTextField(
                    value = filter,
                    onValueChange = { newValue -> filter = newValue },
                    label = stringResource(id = R.string.search_by_name_article_barcode),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = stringResource(id = R.string.search_product_hint),
                    onSearch = {
                        if (filter.isNotEmpty()) {
                            coroutineScope.launch {
                                try {
                                    val exactProduct = productRepository.findProductByBarcode(filter)
                                    if (exactProduct != null) {
                                        onProductSelected(exactProduct)
                                        onDismiss()
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e, "Ошибка при поиске продукта: $filter")
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Список товаров
                if (isLoading && products.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (visibleProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.no_products_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        state = listState,
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(
                            items = visibleProducts,
                            key = { it.id }
                        ) { product ->
                            OptimizedProductItem(
                                product = product,
                                onClick = { onProductSelected(product) }
                            )
                        }

                        if (isLoading && products.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }

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
            searchJob?.cancel()
        }
    }
}

@Composable
private fun OptimizedProductItem(
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

        // Отображаем штрихкоды и единицы измерения
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Отображаем основной штрихкод, если есть
            val mainUnit = product.getMainUnit()
            if (mainUnit != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Отображаем единицу измерения
                    Text(
                        text = stringResource(id = R.string.unit_fmt, mainUnit.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Отображаем основной штрихкод
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
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Асинхронная загрузка начальных данных из репозитория
 */
private suspend fun initialLoadProducts(
    repository: ProductRepository,
    filter: String,
    onDataLoaded: (List<Product>, Boolean) -> Unit,
    pageSize: Int = 20
) {
    try {
        val result = if (filter.isEmpty()) {
            repository.getProducts().collectLatest { products ->
                val firstPage = products.take(pageSize)
                val hasMore = products.size > pageSize
                onDataLoaded(firstPage, hasMore)
            }
        } else {
            repository.getProductsByNameFilter(filter).collectLatest { products ->
                val firstPage = products.take(pageSize)
                val hasMore = products.size > pageSize
                onDataLoaded(firstPage, hasMore)
            }
        }
    } catch (e: Exception) {
        Timber.e(e, "Ошибка при загрузке продуктов")
        onDataLoaded(emptyList(), false)
    }
}

/**
 * Асинхронная загрузка дополнительных данных из репозитория
 */
private suspend fun loadMoreProducts(
    repository: ProductRepository,
    filter: String,
    currentProducts: List<Product>,
    onDataLoaded: (List<Product>, Boolean) -> Unit,
    onLoading: (Boolean) -> Unit,
    pageSize: Int = 20
) {
    onLoading(true)
    try {
        val lastId = currentProducts.lastOrNull()?.id
        if (lastId == null) {
            onLoading(false)
            return
        }

        val result = if (filter.isEmpty()) {
            repository.getProducts().collectLatest { allProducts ->
                // Находим индекс последнего загруженного продукта
                val lastIndex = allProducts.indexOfFirst { it.id == lastId }
                if (lastIndex >= 0 && lastIndex < allProducts.size - 1) {
                    // Берем следующую страницу после последнего известного ID
                    val nextProducts = allProducts.subList(lastIndex + 1,
                        minOf(lastIndex + 1 + pageSize, allProducts.size))

                    val updatedList = currentProducts + nextProducts
                    val hasMore = lastIndex + 1 + pageSize < allProducts.size

                    onDataLoaded(updatedList, hasMore)
                } else {
                    onDataLoaded(currentProducts, false)
                }
            }
        } else {
            repository.getProductsByNameFilter(filter).collectLatest { filteredProducts ->
                // Находим индекс последнего загруженного продукта
                val lastIndex = filteredProducts.indexOfFirst { it.id == lastId }
                if (lastIndex >= 0 && lastIndex < filteredProducts.size - 1) {
                    // Берем следующую страницу после последнего известного ID
                    val nextProducts = filteredProducts.subList(lastIndex + 1,
                        minOf(lastIndex + 1 + pageSize, filteredProducts.size))

                    val updatedList = currentProducts + nextProducts
                    val hasMore = lastIndex + 1 + pageSize < filteredProducts.size

                    onDataLoaded(updatedList, hasMore)
                } else {
                    onDataLoaded(currentProducts, false)
                }
            }
        }
    } catch (e: Exception) {
        Timber.e(e, "Ошибка при загрузке продуктов")
        onDataLoaded(currentProducts, false)
    } finally {
        onLoading(false)
    }
}