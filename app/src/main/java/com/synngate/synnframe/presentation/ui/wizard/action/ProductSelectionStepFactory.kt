package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons


import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.repository.ProductRepository
import com.synngate.synnframe.presentation.common.dialog.OptimizedProductSelectionDialog
import com.synngate.synnframe.presentation.common.scanner.BarcodeHandlerWithState
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class ProductSelectionStepFactory(
    private val productRepository: ProductRepository
) : ActionStepFactory {

    @Composable
    override fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        // Состояние UI
        var manualProductCode by remember { mutableStateOf("") }
        var errorMessage by remember(context.validationError) {
            mutableStateOf(context.validationError)
        }
        var showCameraScannerDialog by remember { mutableStateOf(false) }
        var showProductSelectionDialog by remember { mutableStateOf(false) }

        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        // Данные о продуктах из плана
        val plannedProduct = action.storageProduct?.product
        val planProducts = remember(action) {
            listOfNotNull(action.storageProduct)
        }
        val planProductIds = remember(planProducts) {
            planProducts.mapNotNull { it.product.id }.toSet()
        }

        // Текущий выбранный продукт
        val selectedProduct = remember(context.results) {
            context.results[step.id] as? Product ?:
            (context.results[step.id] as? TaskProduct)?.product
        }

        // Вспомогательные функции
        fun createResultFromProduct(product: Product): Any {
            return if (step.objectType == ActionObjectType.TASK_PRODUCT) {
                TaskProduct(product = product)
            } else {
                product
            }
        }

        val showError = { message: String ->
            errorMessage = message
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }

        val searchProduct = { barcode: String ->
            if (barcode.isNotEmpty()) {
                errorMessage = null

                coroutineScope.launch {
                    processBarcodeForProduct(
                        barcode = barcode,
                        action = action,
                        onProductFound = { product ->
                            if (product != null) {
                                if (planProductIds.isEmpty() || planProductIds.contains(product.id)) {
                                    context.onComplete(createResultFromProduct(product))
                                } else {
                                    showError("Продукт не соответствует плану")
                                }
                            } else {
                                showError("Продукт со штрихкодом '$barcode' не найден")
                            }
                        }
                    )
                }

                manualProductCode = ""
            }
        }

        // Обработчик штрихкодов от сканера устройства
        BarcodeHandlerWithState(
            stepKey = step.id,
            stepResult = context.getCurrentStepResult(),
            onBarcodeScanned = { barcode, setProcessingState ->
                errorMessage = null

                coroutineScope.launch {
                    processBarcodeForProduct(
                        barcode = barcode,
                        action = action,
                        onProductFound = { product ->
                            if (product != null) {
                                if (planProductIds.isEmpty() || planProductIds.contains(product.id)) {
                                    context.onComplete(createResultFromProduct(product))
                                } else {
                                    showError("Продукт не соответствует плану")
                                    setProcessingState(false)
                                }
                            } else {
                                showError("Продукт со штрихкодом '$barcode' не найден")
                                setProcessingState(false)
                            }
                        }
                    )
                }

                manualProductCode = ""
            }
        )

        // Обработка штрихкодов из контекста
        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (!barcode.isNullOrEmpty()) {
                searchProduct(barcode)
            }
        }

        // Диалоги сканирования и выбора
        if (showCameraScannerDialog) {
            UniversalScannerDialog(
                onBarcodeScanned = { barcode ->
                    searchProduct(barcode)
                    showCameraScannerDialog = false
                },
                onClose = {
                    showCameraScannerDialog = false
                },
                instructionText = if (plannedProduct != null)
                    stringResource(R.string.scan_product_expected, plannedProduct.name)
                else
                    stringResource(R.string.scan_product)
            )
        }

        if (showProductSelectionDialog) {
            OptimizedProductSelectionDialog(
                onProductSelected = { product ->
                    context.onComplete(createResultFromProduct(product))
                    showProductSelectionDialog = false
                },
                onDismiss = {
                    showProductSelectionDialog = false
                },
                initialFilter = "",
                title = "Выберите товар",
                planProductIds = planProductIds.ifEmpty { null }
            )
        }

        // Основное содержимое UI
        Column(modifier = Modifier.fillMaxWidth()) {
            // Заголовок и описание действия
            Text(
                text = "${step.promptText} (${getWmsActionDescription(action.wmsAction)})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Сообщение об ошибке валидации
            if (context.validationError != null) {
                ValidationErrorMessage(
                    message = context.validationError,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Поле поиска продукта
            ProductSearchBar(
                value = manualProductCode,
                onValueChange = {
                    manualProductCode = it
                    errorMessage = null
                },
                onSearch = { searchProduct(manualProductCode) },
                onScannerClick = { showCameraScannerDialog = true },
                errorMessage = errorMessage,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Отображение выбранного продукта, если есть
            if (selectedProduct != null) {
                Text(
                    text = "Выбранный продукт:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                ProductCard(
                    product = selectedProduct,
                    isSelected = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Список продуктов из плана
            if (planProducts.isNotEmpty()) {
                PlanProductsList(
                    planProducts = planProducts,
                    onProductSelect = { taskProduct ->
                        context.onComplete(
                            if (step.objectType == ActionObjectType.TASK_PRODUCT) {
                                taskProduct
                            } else {
                                taskProduct.product
                            }
                        )
                    }
                )
            }

            // Кнопка выбора из списка
            Button(
                onClick = { showProductSelectionDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Выбрать из списка товаров")
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        return when (step.objectType) {
            ActionObjectType.CLASSIFIER_PRODUCT -> value is Product
            ActionObjectType.TASK_PRODUCT -> value is TaskProduct
            else -> false
        }
    }

    /**
     * Обработка штрихкода для поиска продукта
     */
    private suspend fun processBarcodeForProduct(
        barcode: String,
        action: PlannedAction,
        onProductFound: (Product?) -> Unit
    ) {
        val plannedProduct = action.storageProduct?.product

        // Сначала проверяем, соответствует ли штрихкод планируемому продукту
        if (plannedProduct != null) {
            val plannedBarcodes = plannedProduct.getAllBarcodes()
            if (plannedBarcodes.contains(barcode)) {
                onProductFound(plannedProduct)
                return
            }
        }

        // Если продукт не найден в плане, ищем в репозитории
        try {
            val product = withContext(Dispatchers.IO) {
                productRepository.findProductByBarcode(barcode)
            }

            onProductFound(product)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при поиске продукта по штрихкоду: $barcode")
            onProductFound(null)
        }
    }
}

/**
 * Переиспользуемый компонент поисковой строки для продуктов
 *
 * @param value текущее значение поля ввода
 * @param onValueChange обработчик изменения текста
 * @param onSearch обработчик поискового запроса
 * @param onScannerClick обработчик нажатия на кнопку сканера
 * @param errorMessage сообщение об ошибке (null, если ошибки нет)
 * @param enabled доступность поля ввода
 * @param modifier модификатор для компонента
 */
@Composable
fun ProductSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onScannerClick: () -> Unit,
    errorMessage: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.enter_product_barcode)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(value) }),
            trailingIcon = {
                IconButton(onClick = onScannerClick) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = stringResource(R.string.scan_with_camera)
                    )
                }
            },
            isError = errorMessage != null,
            enabled = enabled
        )

        if (errorMessage != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Ошибка",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Карточка для отображения информации о продукте
 *
 * @param product объект продукта
 * @param isSelected выбран ли продукт
 * @param onClick обработчик нажатия (null, если карточка не кликабельна)
 * @param modifier модификатор для компонента
 */
@Composable
fun ProductCard(
    product: Product,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Card(
        modifier = cardModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Артикул: ${product.articleNumber}",
                style = MaterialTheme.typography.bodySmall
            )

            product.getMainUnit()?.let { unit ->
                Text(
                    text = "Основная ЕИ: ${unit.name}",
                    style = MaterialTheme.typography.bodySmall
                )

                if (unit.mainBarcode.isNotEmpty()) {
                    Text(
                        text = "Штрихкод: ${unit.mainBarcode}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Карточка для отображения информации о продукте задания
 *
 * @param taskProduct объект продукта задания
 * @param isSelected выбран ли продукт
 * @param onClick обработчик нажатия (null, если карточка не кликабельна)
 * @param modifier модификатор для компонента
 */
@Composable
fun TaskProductCard(
    taskProduct: TaskProduct,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = taskProduct.product.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Артикул: ${taskProduct.product.articleNumber}",
                    style = MaterialTheme.typography.bodySmall
                )

                if (taskProduct.quantity > 0) {
                    Text(
                        text = "Количество: ${taskProduct.quantity}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            onClick?.let {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Выбрать",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Компонент для отображения списка продуктов из плана задания
 *
 * @param planProducts список продуктов из плана
 * @param onProductSelect обработчик выбора продукта
 * @param modifier модификатор для компонента
 * @param title заголовок списка
 * @param maxHeight максимальная высота списка
 * @param showDivider показывать ли разделитель после списка
 */
@Composable
fun PlanProductsList(
    planProducts: List<TaskProduct>,
    onProductSelect: (TaskProduct) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "По плану:",
    maxHeight: Int = 150,
    showDivider: Boolean = true
) {
    if (planProducts.isEmpty()) {
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight.dp)
        ) {
            items(planProducts) { taskProduct ->
                TaskProductCard(
                    taskProduct = taskProduct,
                    onClick = { onProductSelect(taskProduct) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

/**
 * Компонент для отображения сообщения об ошибке валидации
 *
 * @param message текст сообщения об ошибке
 * @param modifier модификатор для компонента
 */
@Composable
fun ValidationErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(
                MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Ошибка валидации",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}