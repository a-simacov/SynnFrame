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
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationType
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.common.scanner.BarcodeHandlerWithState
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.taskx.components.PalletItem
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import com.synngate.synnframe.presentation.ui.wizard.ActionDataViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

class PalletSelectionStepFactory(
    private val wizardViewModel: ActionDataViewModel
) : ActionStepFactory {

    @Composable
    override fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ) {
        var manualPalletCode by remember { mutableStateOf("") }
        var showPalletList by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }

        var errorMessage by remember { mutableStateOf<String?>(null) }

        var showCameraScannerDialog by remember { mutableStateOf(false) }

        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        val pallets by wizardViewModel.pallets.collectAsState()

        val isStorageStep = step.id in action.actionTemplate.storageSteps.map { it.id }

        val plannedPallet = if (isStorageStep) action.storagePallet else action.placementPallet

        val planPallets = remember(action, isStorageStep) {
            listOfNotNull(plannedPallet)
        }

        val selectedPallet = remember(context.results) {
            context.results[step.id] as? Pallet
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

        val searchPallet = { code: String ->
            if (code.isNotEmpty()) {
                Timber.d("Поиск паллеты по коду: $code")
                errorMessage = null

                processBarcodeForPallet(
                    barcode = code,
                    expectedBarcode = plannedPallet?.code,
                    onPalletFound = { pallet ->
                        if (pallet != null) {
                            Timber.d("Паллета найдена: ${pallet.code}")
                            context.onComplete(pallet)
                        } else {
                            Timber.w("Паллета не найдена: $code")
                            showError("Паллета с кодом '$code' не найдена")
                        }
                    }
                )

                manualPalletCode = ""
            }
        }

        BarcodeHandlerWithState(
            stepKey = step.id,
            stepResult = context.getCurrentStepResult(),
            onBarcodeScanned = { barcode, setProcessingState ->
                Timber.d("Получен штрихкод от сканера: $barcode")
                errorMessage = null

                processBarcodeForPallet(
                    barcode = barcode,
                    expectedBarcode = plannedPallet?.code,
                    onPalletFound = { pallet ->
                        if (pallet != null) {
                            Timber.d("Паллета найдена: ${pallet.code}")
                            context.onComplete(pallet)
                        } else {
                            Timber.w("Паллета не найдена: $barcode")
                            showError("Паллета с кодом '$barcode' не найдена")
                            setProcessingState(false)
                        }
                    }
                )

                manualPalletCode = ""
            }
        )

        // Обработка внешнего штрихкода из контекста
        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (!barcode.isNullOrEmpty()) {
                Timber.d("Получен штрихкод от внешнего сканера: $barcode")

                // Просто перенаправляем в функцию поиска паллеты
                searchPallet(barcode)
            }
        }

        LaunchedEffect(searchQuery) {
            wizardViewModel.loadPallets(searchQuery)
        }

        if (showCameraScannerDialog) {
            UniversalScannerDialog(
                onBarcodeScanned = { barcode ->
                    Timber.d("Получен штрихкод от камеры: $barcode")
                    searchPallet(barcode)
                    showCameraScannerDialog = false
                },
                onClose = {
                    showCameraScannerDialog = false
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
                text = "${step.promptText} (${getWmsActionDescription(action.wmsAction)})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            OutlinedTextField(
                value = manualPalletCode,
                onValueChange = {
                    manualPalletCode = it
                    errorMessage = null // Сбрасываем ошибку при вводе
                },
                label = { Text(stringResource(R.string.enter_pallet_code)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { searchPallet(manualPalletCode) }),
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

            //Spacer(modifier = Modifier.height(16.dp))

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
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Отображаем запланированные паллеты, если они есть
            if (planPallets.isNotEmpty()) {
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
                    items(planPallets) { pallet ->
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
                                        text = "Код: ${pallet.code}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Статус: ${if (pallet.isClosed) "Закрыта" else "Открыта"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                IconButton(
                                    onClick = { context.onComplete(pallet) }
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

            // Кнопка для выбора из списка (если нет запланированной паллеты или есть, но можно выбрать любую)
            if (plannedPallet == null || !step.validationRules.rules.any { it.type == ValidationType.FROM_PLAN }) {
                Button(
                    onClick = {
                        showPalletList = !showPalletList
                        // Загружаем паллеты при открытии списка
                        if (showPalletList) {
                            wizardViewModel.loadPallets("")
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
                    Text(if (showPalletList) "Скрыть список" else "Выбрать из списка")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Список паллет для выбора (показывается только если активирован)
            if (showPalletList) {
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

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(pallets) { pallet ->
                        PalletItem(
                            pallet = pallet,
                            onClick = { context.onComplete(pallet) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Кнопка Вперед в нижней части экрана (если выбрана паллета)
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
        return value is Pallet
    }

    // Метод для обработки отсканированного штрихкода
    private fun processBarcodeForPallet(
        barcode: String,
        expectedBarcode: String?,
        onPalletFound: (Pallet?) -> Unit
    ) {
        // Если задан ожидаемый штрихкод, проверяем соответствие
        if (expectedBarcode != null && barcode != expectedBarcode) {
            Timber.w("Несоответствие штрихкода: ожидался $expectedBarcode, получен $barcode")
            onPalletFound(null)
            return
        }

        // Ищем паллету по штрихкоду
        wizardViewModel.findPalletByCode(barcode, onPalletFound)
    }
}