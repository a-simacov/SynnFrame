package com.synngate.synnframe.presentation.common.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synngate.synnframe.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Универсальный диалог сканирования штрихкодов
 */
@Composable
fun UniversalScannerDialog(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit,
    instructionText: String,
    expectedBarcode: String? = null,
    title: String = stringResource(id = R.string.scan_barcode),
    allowManualInput: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Состояния UI
    var isManualInputMode by remember { mutableStateOf(false) }
    var manualBarcode by remember { mutableStateOf("") }
    var scannerKey by remember { mutableStateOf(0) } // Ключ для пересоздания сканера

    // Состояния обработки
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isProcessing = false
        Timber.d("UniversalScannerDialog: Сброс флага isProcessing при инициализации")
    }

    // Состояния ошибок
    var errorState by remember { mutableStateOf<ErrorState?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Функция для показа временного сообщения об ошибке
    val showTemporaryError = { message: String, barcode: String ->
        errorState = ErrorState(message, barcode)
        coroutineScope.launch {
            delay(3000)
            if (errorState?.message == message) {
                errorState = null
            }
        }
    }

    // Функция для обработки успешного сканирования
    val handleSuccessfulScan = { barcode: String ->
        if (!isProcessing) {
            isProcessing = true
            Timber.d("Успешное сканирование: $barcode")
            // 1. Сначала вызываем обработчик
            onBarcodeScanned(barcode)
            // 2. Затем закрываем диалог - это должно вызвать переход к следующему шагу
            onClose()
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Заголовок и кнопка закрытия
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.close)
                        )
                    }
                }

                // Инструкция
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = instructionText,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                // Если ожидается определенный штрихкод, показываем его
                if (expectedBarcode != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.expected_barcode, expectedBarcode),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }

                // Область сканирования или ручного ввода
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val barcodeMismatchMessage = stringResource(id = R.string.barcode_mismatch)
                    if (isManualInputMode) {
                        // Режим ручного ввода
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            OutlinedTextField(
                                value = manualBarcode,
                                onValueChange = {
                                    manualBarcode = it
                                    // Сбрасываем ошибку при изменении ввода
                                    errorState = null
                                },
                                label = { Text(stringResource(R.string.enter_barcode)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                singleLine = true,
                                isError = errorState != null
                            )

                            errorState?.let {
                                Text(
                                    text = "${it.message}\nОтсканирован: ${it.barcode}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = { isManualInputMode = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.use_camera))
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Button(
                                    onClick = {
                                        if (manualBarcode.isNotEmpty()) {
                                            isProcessing = true
                                            // Проверяем, соответствует ли введенный штрихкод ожидаемому
                                            if (expectedBarcode != null && manualBarcode != expectedBarcode) {
                                                showTemporaryError(
                                                    barcodeMismatchMessage,
                                                    manualBarcode
                                                )
                                                isProcessing = false
                                            } else {
                                                // Успешное сканирование
                                                handleSuccessfulScan(manualBarcode)
                                                isProcessing = false
                                            }
                                        }
                                    },
                                    enabled = manualBarcode.isNotEmpty() && !isProcessing,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    } else {
                                        Text(stringResource(R.string.confirm))
                                    }
                                }
                            }
                        }
                    } else {
                        // Режим камеры - используем ключ для пересоздания при ошибке
                        key(scannerKey) {
                            BarcodeScannerView(
                                onBarcodeDetected = { barcode ->
                                    Timber.d("Штрихкод обнаружен: $barcode")
                                    // Проверяем, соответствует ли штрихкод ожидаемому
                                    if (expectedBarcode != null && barcode != expectedBarcode) {
                                        Timber.d("Несоответствие штрихкода: ожидался $expectedBarcode, получен $barcode")
                                        showTemporaryError(
                                            barcodeMismatchMessage,
                                            barcode
                                        )
                                        // Перезагружаем сканер после ошибки
                                        scannerKey++
                                    } else {
                                        // Успешное сканирование
                                        Timber.d("Штрихкод соответствует ожидаемому, вызываем handleSuccessfulScan")
                                        handleSuccessfulScan(barcode)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Показываем ошибку поверх камеры
                        errorState?.let {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp)
                                    .widthIn(max = 300.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${it.message}\nОтсканирован: ${it.barcode}",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Кнопка для обновления сканера вручную
                                    Button(
                                        onClick = {
                                            errorState = null
                                            scannerKey++ // Пересоздаем сканер
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Обновить",
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        Text("Обновить сканер")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Нижние кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Кнопка переключения режима ввода (если разрешен ручной ввод)
                    if (allowManualInput) {
                        Button(
                            onClick = {
                                isManualInputMode = !isManualInputMode
                                errorState = null // Сбросить ошибки при смене режима
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (isManualInputMode)
                                        stringResource(R.string.use_camera)
                                    else
                                        stringResource(R.string.manual_input)
                                )
                                Icon(
                                    imageVector = if (isManualInputMode)
                                        Icons.Default.KeyboardArrowDown
                                    else
                                        Icons.Default.KeyboardArrowUp,
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    // Кнопка закрытия
                    Button(
                        onClick = onClose,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(id = R.string.cancel))
                    }
                }
            }
        }
    }
}

/**
 * Класс для хранения информации об ошибке
 */
private data class ErrorState(
    val message: String,
    val barcode: String
)