package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.taskx.components.BinItem
import com.synngate.synnframe.presentation.ui.wizard.ActionDataViewModel

/**
 * Фабрика компонентов для шага выбора ячейки с тремя способами ввода
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
        var searchQuery by remember { mutableStateOf("") }

        // Состояния для диалогов и режимов ввода
        var showCameraScannerDialog by remember { mutableStateOf(false) }
        var showScanMethodSelection by remember { mutableStateOf(false) }
        var selectedInputMethod by remember { mutableStateOf(InputMethod.NONE) }

        // Получаем зону из контекста, если указана
        val zoneFilter = action.placementBin?.zone

        // Получение данных из ViewModel
        val bins by wizardViewModel.bins.collectAsState()

        // Получаем сервис сканера для встроенного сканера
        val scannerService = LocalScannerService.current

        // Запланированная ячейка (может быть null)
        val plannedBin = action.placementBin

        // Список запланированных ячеек
        val planBins = remember(action) {
            listOfNotNull(plannedBin)
        }

        // Получаем уже выбранную ячейку из контекста, если есть
        val selectedBin = remember(context.results) {
            context.results[step.id] as? BinX
        }

        // Слушатель событий сканирования
        if (selectedInputMethod == InputMethod.HARDWARE_SCANNER) {
            ScannerListener(
                onBarcodeScanned = { barcode ->
                    processBarcodeForBin(
                        barcode = barcode,
                        expectedBarcode = plannedBin?.code,
                        onBinFound = { bin ->
                            if (bin != null) {
                                context.onComplete(bin)
                                selectedInputMethod = InputMethod.NONE
                            }
                        }
                    )
                }
            )
        }

        // Загрузка ячеек при изменении поискового запроса или зоны
        LaunchedEffect(zoneFilter, searchQuery) {
            wizardViewModel.loadBins(searchQuery, zoneFilter)
        }

        // Показываем диалог сканирования камерой, если выбран этот метод
        if (showCameraScannerDialog) {
            UniversalScannerDialog(
                onBarcodeScanned = { barcode ->
                    processBarcodeForBin(
                        barcode = barcode,
                        expectedBarcode = plannedBin?.code,
                        onBinFound = { bin ->
                            if (bin != null) {
                                context.onComplete(bin)
                            }
                            showCameraScannerDialog = false
                            selectedInputMethod = InputMethod.NONE
                        }
                    )
                },
                onClose = {
                    showCameraScannerDialog = false
                    selectedInputMethod = InputMethod.NONE
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
                text = step.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

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
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Зона: ${selectedBin.zone}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Расположение: ${selectedBin.line}-${selectedBin.rack}-${selectedBin.tier}-${selectedBin.position}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        // Кнопка "Вперёд" для перехода к следующему шагу
                        if (context.hasStepResult) {
                            Button(
                                onClick = { context.onForward() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text("Вперёд")
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Если не выбран способ ввода, показываем кнопки выбора метода
            if (!showScanMethodSelection && selectedInputMethod == InputMethod.NONE && selectedBin == null) {
                Text(
                    text = stringResource(R.string.choose_scan_method),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Кнопки выбора метода ввода
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Кнопка для сканирования камерой
                    Button(
                        onClick = {
                            selectedInputMethod = InputMethod.CAMERA
                            showCameraScannerDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(stringResource(R.string.scan_with_camera))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Кнопка для сканирования встроенным сканером (если доступен)
                    if (scannerService != null) {
                        Button(
                            onClick = { selectedInputMethod = InputMethod.HARDWARE_SCANNER },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(stringResource(R.string.scan_with_scanner))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Кнопка для выбора из списка
                Button(
                    onClick = { showScanMethodSelection = true },
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
                    Text(stringResource(R.string.select_from_list))
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Показываем интерфейс встроенного сканера, если выбран этот метод
            if (selectedInputMethod == InputMethod.HARDWARE_SCANNER) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (plannedBin != null)
                                stringResource(R.string.scan_bin_expected, plannedBin.code)
                            else
                                stringResource(R.string.scan_bin),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Ожидание сканирования...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { selectedInputMethod = InputMethod.NONE },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("Отмена")
                        }
                    }
                }
            }

            // Отображаем запланированные ячейки, если они есть
            if (planBins.isNotEmpty() && (showScanMethodSelection || selectedInputMethod == InputMethod.NONE)) {
                Text(
                    text = "По плану:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f)
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Код: ${bin.code}",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Зона: ${bin.zone}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Расположение: ${bin.line}-${bin.rack}-${bin.tier}-${bin.position}",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Button(
                                    onClick = {
                                        context.onComplete(bin)
                                        showScanMethodSelection = false
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                ) {
                                    Text("Выбрать")
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Поиск и список ячеек (показываются только если выбран режим ручного ввода)
            if (showScanMethodSelection) {
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

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(bins) { bin ->
                        BinItem(
                            bin = bin,
                            onClick = {
                                context.onComplete(bin)
                                showScanMethodSelection = false
                            }
                        )
                    }
                }

                // Кнопка отмены для возврата к выбору способа ввода
                Button(
                    onClick = { showScanMethodSelection = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Отмена")
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
            onBinFound(null)
            return
        }

        // Ищем ячейку по штрихкоду
        wizardViewModel.findBinByCode(barcode, onBinFound)
    }

    // Перечисление для методов ввода
    private enum class InputMethod {
        NONE,               // Режим выбора
        CAMERA,             // Сканирование камерой
        HARDWARE_SCANNER,   // Сканирование встроенным сканером
    }
}