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
import com.synngate.synnframe.presentation.common.LocalScannerService
import com.synngate.synnframe.presentation.common.scanner.ScannerListener
import com.synngate.synnframe.presentation.common.scanner.UniversalScannerDialog
import com.synngate.synnframe.presentation.ui.taskx.components.PalletItem
import com.synngate.synnframe.presentation.ui.taskx.utils.getWmsActionDescription
import com.synngate.synnframe.presentation.ui.wizard.ActionDataViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Фабрика компонентов для шага выбора паллеты с улучшенным интерфейсом
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
        // Состояния поля поиска и ввода
        var manualPalletCode by remember { mutableStateOf("") }
        var showPalletList by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }

        // Состояние для сообщений об ошибках
        var errorMessage by remember { mutableStateOf<String?>(null) }

        // Состояние для диалога сканирования камерой
        var showCameraScannerDialog by remember { mutableStateOf(false) }

        // Для отображения сообщений
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        // Получение данных о паллетах из ViewModel
        val pallets by wizardViewModel.pallets.collectAsState()

        // Определяем, является ли шаг шагом хранения или размещения
        val isStorageStep = step.id in action.actionTemplate.storageSteps.map { it.id }

        // Получаем запланированную паллету в зависимости от типа шага
        val plannedPallet = if (isStorageStep) action.storagePallet else action.placementPallet

        // Список запланированных паллет для отображения
        val planPallets = remember(action, isStorageStep) {
            listOfNotNull(plannedPallet)
        }

        // Получаем уже выбранную паллету из контекста, если есть
        val selectedPallet = remember(context.results) {
            context.results[step.id] as? Pallet
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

        var isProcessingBarcode by remember { mutableStateOf(false) }
        var isScanProcessed by remember { mutableStateOf(false) }

        // Функция поиска паллеты по коду
        val searchPallet = { code: String ->
            if (!isProcessingBarcode && code.isNotEmpty() && !isScanProcessed) {
                Timber.d("Поиск паллеты по коду: $code")
                errorMessage = null // Сбрасываем предыдущую ошибку

                isProcessingBarcode = true

                // Установка флага обработки
                isScanProcessed = true

                processBarcodeForPallet(
                    barcode = code,
                    expectedBarcode = plannedPallet?.code,
                    onPalletFound = { pallet ->
                        if (pallet != null) {
                            Timber.d("Паллета найдена: ${pallet.code}")
                            // Только вызываем onComplete, без onForward
                            context.onComplete(pallet)
                        } else {
                            Timber.w("Паллета не найдена: $code")
                            showError("Паллета с кодом '$code' не найдена")
                            // Сбрасываем флаг, так как ошибка - можно повторить сканирование
                            isScanProcessed = false
                        }
                    }
                )

                isProcessingBarcode = false
                // Очищаем поле ввода после поиска
                manualPalletCode = ""
            }
        }

        // Эффект для обработки штрихкода от внешнего сканера
        LaunchedEffect(context.lastScannedBarcode) {
            val barcode = context.lastScannedBarcode
            if (!barcode.isNullOrEmpty() && !isProcessingBarcode && !isScanProcessed) {
                isProcessingBarcode = true
                Timber.d("Получен штрихкод от внешнего сканера: $barcode")

                // Установка флага обработки
                isScanProcessed = true

                processBarcodeForPallet(
                    barcode = barcode,
                    expectedBarcode = plannedPallet?.code,
                    onPalletFound = { pallet ->
                        if (pallet != null) {
                            Timber.d("Паллета найдена: ${pallet.code}")
                            // Только вызываем onComplete, без onForward
                            context.onComplete(pallet)
                        } else {
                            Timber.w("Паллета не найдена: $barcode")
                            showError("Паллета с кодом '$barcode' не найдена")
                            // Сбрасываем флаг, так как ошибка - можно повторить сканирование
                            isScanProcessed = false
                        }
                    }
                )

                isProcessingBarcode = false
            }
        }

        // Слушатель событий сканирования от встроенного сканера - всегда активен
        if (!isScanProcessed) {
            ScannerListener(
                onBarcodeScanned = { barcode ->
                    if (!isProcessingBarcode && !isScanProcessed) {
                        Timber.d("Получен штрихкод от встроенного сканера: $barcode")
                        searchPallet(barcode)
                    }
                }
            )
        }

        // Загрузка паллет при изменении поискового запроса
        LaunchedEffect(searchQuery) {
            wizardViewModel.loadPallets(searchQuery)
        }

        // Показываем диалог сканирования камерой, если он активирован
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
            // Заголовок с описанием действия WMS
            Text(
                text = "${step.promptText} (${getWmsActionDescription(action.wmsAction)})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Поле для ручного ввода кода паллеты (всегда отображается)
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

            Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(16.dp))
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
                        .height(120.dp)
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

                Spacer(modifier = Modifier.height(16.dp))
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