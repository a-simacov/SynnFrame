package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import com.synngate.synnframe.presentation.common.scanner.BarcodeHandlerWithState
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.taskx.components.BinItem
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import com.synngate.synnframe.presentation.ui.wizard.ActionDataViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

class BinSelectionStepFactory(
    private val wizardViewModel: ActionDataViewModel
) : ActionStepFactory {

    @Composable
    override fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        var manualBinCode by remember { mutableStateOf("") }
        var showBinList by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }

        var errorMessage by remember(context.validationError) {
            mutableStateOf<String?>(context.validationError)
        }

        var showCameraScannerDialog by remember { mutableStateOf(false) }

        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        val bins by wizardViewModel.bins.collectAsState()

        val zoneFilter = action.placementBin?.zone

        val plannedBin = action.placementBin

        val planBins = remember(action) {
            listOfNotNull(plannedBin)
        }

        val selectedBin = remember(context.results) {
            context.results[step.id] as? BinX
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

        val searchBin = { code: String ->
            if (code.isNotEmpty()) {
                Timber.d("Поиск ячейки по коду: $code")
                errorMessage = null

                processBarcodeForBin(
                    barcode = code,
                    expectedBarcode = plannedBin?.code,
                    onBinFound = { bin ->
                        if (bin != null) {
                            Timber.d("Ячейка найдена: ${bin.code}")
                            context.onComplete(bin)
                        } else {
                            Timber.w("Ячейка не найдена: $code")
                            showError("Ячейка с кодом '$code' не найдена")
                        }
                    }
                )

                manualBinCode = ""
            }
        }

        BarcodeHandlerWithState(
            stepKey = step.id,
            stepResult = context.getCurrentStepResult(),
            onBarcodeScanned = { barcode, setProcessingState ->
                Timber.d("Получен штрихкод от сканера: $barcode")
                errorMessage = null

                processBarcodeForBin(
                    barcode = barcode,
                    expectedBarcode = plannedBin?.code,
                    onBinFound = { bin ->
                        if (bin != null) {
                            Timber.d("Ячейка найдена: ${bin.code}")
                            context.onComplete(bin)
                        } else {
                            Timber.w("Ячейка не найдена: $barcode")
                            showError("Ячейка с кодом '$barcode' не найдена")
                            setProcessingState(false)
                        }
                    }
                )

                manualBinCode = ""
            }
        )

        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (!barcode.isNullOrEmpty()) {
                Timber.d("Получен штрихкод от внешнего сканера: $barcode")
                searchBin(barcode)
            }
        }

        LaunchedEffect(searchQuery, zoneFilter) {
            wizardViewModel.loadBins(searchQuery, zoneFilter)
        }

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
            Text(
                text = "${step.promptText} (${getWmsActionDescription(action.wmsAction)})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            if (context.validationError != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Ошибка валидации",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = context.validationError,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

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
                            .padding(4.dp)
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

                Spacer(modifier = Modifier.height(4.dp))

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

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        return value is BinX
    }

    // Изменяем метод обработки штрихкода для ячейки
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

        // Создаем объект ячейки с введенным кодом (без обращения к репозиторию)
        val bin = BinX(
            code = barcode,
            zone = "Неизвестная зона",
            line = "",
            rack = "",
            tier = "",
            position = ""
        )

        onBinFound(bin)
    }
}