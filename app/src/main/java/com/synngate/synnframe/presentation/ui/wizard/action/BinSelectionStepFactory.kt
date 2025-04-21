package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationType
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scanner.BarcodeHandlerWithState
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.taskx.components.BinItem
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import com.synngate.synnframe.presentation.ui.wizard.ActionDataViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Фабрика компонентов для шага выбора ячейки с улучшенным интерфейсом
 */
class BinSelectionStepFactory(
    private val wizardViewModel: ActionDataViewModel
) : ActionStepFactory {

    @Composable
    override fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        // Состояния поля поиска и ввода
        var manualBinCode by remember { mutableStateOf("") }
        var showBinList by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }

        // Состояние для сообщений об ошибках
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Состояние для диалога сканирования камерой
        var showCameraScannerDialog by remember { mutableStateOf(false) }

        // Для отображения сообщений
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        // Получение данных о ячейках из ViewModel
        val bins by wizardViewModel.bins.collectAsState()

        // Получаем зону из контекста, если указана
        val zoneFilter = action.placementBin?.zone

        // Получаем запланированную ячейку
        val plannedBin = action.placementBin

        // Список запланированных ячеек для отображения
        val planBins = remember(action) {
            listOfNotNull(plannedBin)
        }

        // Получаем уже выбранную ячейку из контекста, если есть
        val selectedBin = remember(context.results) {
            context.results[step.id] as? BinX
        }

        // Получаем сервис сканера для встроенного сканера
        val scannerService = LocalScannerService.current

        // Функция для показа сообщения об ошибке
        val showError = { message: String ->
            errorMessage = message
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }

        // Функция поиска ячейки по коду
        val searchBin = { code: String ->
            if (code.isNotEmpty()) {
                Timber.d("Поиск ячейки по коду: $code")
                errorMessage = null // Сбрасываем предыдущую ошибку

                processBarcodeForBin(
                    barcode = code,
                    expectedBarcode = plannedBin?.code,
                    onBinFound = { bin ->
                        if (bin != null) {
                            Timber.d("Ячейка найдена: ${bin.code}")
                            // Только вызываем onComplete, без onForward
                            context.onComplete(bin)
                        } else {
                            Timber.w("Ячейка не найдена: $code")
                            showError("Ячейка с кодом '$code' не найдена")
                        }
                    }
                )

                // Очищаем поле ввода после поиска
                manualBinCode = ""
            }
        }

        // Использование BarcodeHandlerWithState для обработки штрихкодов
        BarcodeHandlerWithState(
            stepKey = step.id,
            stepResult = context.getCurrentStepResult(),
            onBarcodeScanned = { barcode, setProcessingState ->
                Timber.d("Получен штрихкод от сканера: $barcode")
                errorMessage = null // Сбрасываем предыдущую ошибку

                processBarcodeForBin(
                    barcode = barcode,
                    expectedBarcode = plannedBin?.code,
                    onBinFound = { bin ->
                        if (bin != null) {
                            Timber.d("Ячейка найдена: ${bin.code}")
                            // Вызываем onComplete для передачи результата
                            context.onComplete(bin)
                            // Не сбрасываем состояние, т.к. завершили шаг успешно
                        } else {
                            Timber.w("Ячейка не найдена: $barcode")
                            showError("Ячейка с кодом '$barcode' не найдена")
                            // Сбрасываем состояние обработки, чтобы можно было повторить сканирование
                            setProcessingState(false)
                        }
                    }
                )

                // Очищаем поле ввода после поиска
                manualBinCode = ""
            }
        )

        // Эффект для обработки штрихкода от внешнего сканера
        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (!barcode.isNullOrEmpty()) {
                Timber.d("Получен штрихкод от внешнего сканера: $barcode")
                searchBin(barcode)
            }
        }

        // Загрузка ячеек при изменении поискового запроса
        LaunchedEffect(searchQuery, zoneFilter) {
            wizardViewModel.loadBins(searchQuery, zoneFilter)
        }

        // Показываем диалог сканирования камерой, если он активирован
        if (showCameraScannerDialog) {
            UniversalScannerDialog(
                onBarcodeScanned = { barcode ->
                    Timber.d("Получен штрихкод от камеры: $barcode")
                    searchBin(barcode)
                    showCameraScannerDialog = false
                },
                onClose = {
                    showCameraScannerDialog = false
                },
                instructionText = if (plannedBin != null)
                    stringResource(R.string.scan_bin_expected, plannedBin.code)
                else
                    stringResource(R.string.scan_bin),
                expectedBarcode = plannedBin?.code
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Заголовок с описанием действия WMS
            Text(
                text = "${step.promptText} (${getWmsActionDescription(action.wmsAction)})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Поле для ручного ввода кода ячейки (всегда отображается)
            OutlinedTextField(
                value = manualBinCode,
                onValueChange = {
                    manualBinCode = it
                    errorMessage = null // Сбрасываем ошибку при вводе
                },
                label = { Text(stringResource(R.string.enter_bin_code)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { searchBin(manualBinCode) }),
                trailingIcon = {
                    IconButton(onClick = { showCameraScannerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.scan_with_camera)
                        )
                    }
                },
                isError = errorMessage != null,
                supportingText = {
                    if (errorMessage != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Ошибка",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Отображаем выбранную ячейку, если есть
            if (selectedBin != null) {
                Text(
                    text = "Выбранная ячейка:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Код: ${selectedBin.code}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Зона: ${selectedBin.zone}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Расположение: ${selectedBin.line}-${selectedBin.rack}-${selectedBin.tier}-${selectedBin.position}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Отображаем запланированные ячейки, если они есть
            if (planBins.isNotEmpty()) {
                Text(
                    text = "По плану:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    items(planBins) { bin ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Код: ${bin.code}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Зона: ${bin.zone}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Расположение: ${bin.line}-${bin.rack}-${bin.tier}-${bin.position}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                IconButton(
                                    onClick = { context.onComplete(bin) }
                                ) {
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

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Кнопка для выбора из списка (если нет запланированной ячейки или есть, но можно выбрать любую)
            if (plannedBin == null || !step.validationRules.rules.any { it.type == ValidationType.FROM_PLAN }) {
                Button(
                    onClick = {
                        showBinList = !showBinList
                        // Загружаем ячейки при открытии списка
                        if (showBinList) {
                            wizardViewModel.loadBins("", zoneFilter)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewList,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(if (showBinList) "Скрыть список" else "Выбрать из списка")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Список ячеек для выбора (показывается только если активирован)
            if (showBinList) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.search_bins)) },
                    placeholder = { Text(stringResource(R.string.search_bins_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(bins) { bin ->
                        BinItem(
                            bin = bin,
                            onClick = { context.onComplete(bin) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Кнопка Вперед в нижней части экрана (если выбрана ячейка)
            if (context.hasStepResult) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    OutlinedButton(
                        onClick = { context.onForward() },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Вперед")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Вперед"
                        )
                    }
                }
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        return value is BinX
    }

    // Метод для обработки отсканированного штрихкода
    private fun processBarcodeForBin(
        barcode: String,
        expectedBarcode: String?,
        onBinFound: (BinX?) -> Unit
    ) {
        // Если задан ожидаемый штрихкод, проверяем соответствие
        if (expectedBarcode != null && barcode != expectedBarcode) {
            Timber.w("Несоответствие штрихкода: ожидался $expectedBarcode, получен $barcode")
            onBinFound(null)
            return
        }

        // Ищем ячейку по штрихкоду
        wizardViewModel.findBinByCode(barcode, onBinFound)
    }
}