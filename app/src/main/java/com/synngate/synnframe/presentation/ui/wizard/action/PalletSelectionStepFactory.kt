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
import androidx.compose.ui.unit.dp
import com.synngate.synnframe.R
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.taskx.components.PalletItem
import com.synngate.synnframe.presentation.ui.wizard.ActionDataViewModel
import timber.log.Timber

/**
 * Фабрика компонентов для шага выбора паллеты с тремя способами ввода
 */
class PalletSelectionStepFactory(
    private val wizardViewModel: ActionDataViewModel
) : ActionStepFactory {

    @Composable
    override fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        var searchQuery by remember { mutableStateOf("") }
        var manualPalletCode by remember { mutableStateOf("") }
        var manualInputError by remember { mutableStateOf<String?>(null) }

        var showCameraScannerDialog by remember { mutableStateOf(false) }
        var showScanMethodSelection by remember { mutableStateOf(false) }
        var showManualInputForm by remember { mutableStateOf(false) }
        var selectedInputMethod by remember { mutableStateOf(InputMethod.NONE) }

        val pallets by wizardViewModel.pallets.collectAsState()

        val isStorageStep = step.id in action.actionTemplate.storageSteps.map { it.id }

        val plannedPallet = if (isStorageStep) action.storagePallet else action.placementPallet

        val planPallets = remember(action, isStorageStep) {
            listOfNotNull(plannedPallet)
        }

        val selectedPallet = remember(context.results) {
            context.results[step.id] as? Pallet
        }

        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (!barcode.isNullOrEmpty()) {
                Timber.d("Получен штрихкод от внешнего сканера: $barcode")
                processBarcodeForPallet(
                    barcode = barcode,
                    expectedBarcode = plannedPallet?.code,
                    onPalletFound = { pallet ->
                        if (pallet != null) {
                            context.onComplete(pallet)
                            context.onForward()
                            selectedInputMethod = InputMethod.NONE
                        }
                    }
                )
            }
        }

        LaunchedEffect(searchQuery) {
            wizardViewModel.loadPallets(searchQuery)
        }

        // Показываем диалог сканирования камерой, если выбран этот метод
        if (showCameraScannerDialog) {
            UniversalScannerDialog(
                onBarcodeScanned = { barcode ->
                    processBarcodeForPallet(
                        barcode = barcode,
                        expectedBarcode = plannedPallet?.code,
                        onPalletFound = { pallet ->
                            if (pallet != null) {
                                context.onComplete(pallet)
                                // Добавляем вызов onForward() для автоматического перехода к следующему шагу
                                context.onForward()
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
                instructionText = if (plannedPallet != null)
                    stringResource(R.string.scan_pallet_expected, plannedPallet.code)
                else
                    stringResource(R.string.scan_pallet),
                expectedBarcode = plannedPallet?.code
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = step.promptText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Отображаем выбранную паллету, если есть
            if (selectedPallet != null) {
                Text(
                    text = "Выбранная паллета:",
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
                            text = "Код: ${selectedPallet.code}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Статус: ${if (selectedPallet.isClosed) "Закрыта" else "Открыта"}",
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
            if (!showScanMethodSelection && !showManualInputForm && selectedInputMethod == InputMethod.NONE && selectedPallet == null) {
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
            }

            // Показываем форму для ручного ввода кода паллеты
            if (showManualInputForm) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Ввод кода паллеты",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = manualPalletCode,
                            onValueChange = {
                                manualPalletCode = it
                                manualInputError =
                                    if (it.isNotEmpty()) {
                                        "Неверный формат кода паллеты. Ожидается формат IN000000000"
                                    } else {
                                        null
                                    }
                            },
                            label = { Text("Код паллеты") },
                            placeholder = { Text("IN000000000") },
                            isError = manualInputError != null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        manualInputError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { showManualInputForm = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Text("Отмена")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (manualPalletCode.isNotEmpty()) {
                                        findPallet(manualPalletCode) { pallet ->
                                            if (pallet != null) {
                                                context.onComplete(pallet)
                                                showManualInputForm = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = manualPalletCode.isNotEmpty() && manualInputError == null
                            ) {
                                Text("Создать/Найти")
                            }
                        }
                    }
                }
            }

            // Отображаем запланированные паллеты, если они есть
            if (planPallets.isNotEmpty() && (showScanMethodSelection || selectedInputMethod == InputMethod.NONE)) {
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
                    items(planPallets) { pallet ->
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
                                    text = "Код: ${pallet.code}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Статус: ${if (pallet.isClosed) "Закрыта" else "Открыта"}",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Button(
                                    onClick = {
                                        context.onComplete(pallet)
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

            // Поиск и список паллет (показываются только если выбран режим ручного ввода)
            if (showScanMethodSelection) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.search_pallets)) },
                    placeholder = { Text(stringResource(R.string.search_pallets_hint)) },
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
                    items(pallets) { pallet ->
                        PalletItem(
                            pallet = pallet,
                            onClick = {
                                context.onComplete(pallet)
                                showScanMethodSelection = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
        return value is Pallet
    }

    private fun processBarcodeForPallet(
        barcode: String,
        expectedBarcode: String?,
        onPalletFound: (Pallet?) -> Unit
    ) {
        if (expectedBarcode != null && barcode != expectedBarcode) {
            onPalletFound(null)
            return
        }

        findPallet(barcode, onPalletFound)
    }

    private fun findPallet(code: String, onResult: (Pallet?) -> Unit) {
        wizardViewModel.findPalletByCode(code) { pallet ->
            onResult(pallet)
        }
    }
}

private enum class InputMethod {
    NONE,               // Режим выбора
    CAMERA,             // Сканирование камерой
    HARDWARE_SCANNER,   // Сканирование встроенным сканером
}