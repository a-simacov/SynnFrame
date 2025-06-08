package com.synngate.synnframe.presentation.common.scanner

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import timber.log.Timber

@Composable
fun UniversalScannerDialog(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit,
    instructionText: String = stringResource(id = R.string.scan_barcode_instruction),
    expectedBarcode: String? = null,
    title: String = stringResource(id = R.string.scan_barcode),
    allowManualInput: Boolean = false
) {
    // Состояние ошибки сканирования
    var errorState by remember { mutableStateOf<Pair<String, String>?>(null) }
    // Генерируем уникальный ключ для пересоздания сканера при ошибке
    var scannerKey by remember { mutableIntStateOf(0) }
    // Текст ручного ввода штрихкода
    var manualBarcode by remember { mutableStateOf("") }
    // Состояние режима ввода (камера или ручной)
    var inputMode by remember { mutableStateOf(if (allowManualInput) "camera" else "camera") }
    // Индикатор обработки
    var isProcessing by remember { mutableStateOf(false) }

    // Сообщение об ошибке при несоответствии штрихкода
    val barcodeMismatchMessage = expectedBarcode?.let {
        stringResource(id = R.string.barcode_expected_fmt, it)
    } ?: stringResource(id = R.string.barcode_mismatch)

    // Автоматически скрывать сообщение об ошибке через 3 секунды
    LaunchedEffect(errorState) {
        if (errorState != null) {
            delay(3000)
            errorState = null
        }
    }

    // Временно показать сообщение об ошибке
    fun showTemporaryError(message: String, scannedBarcode: String) {
        errorState = Pair(message, scannedBarcode)
    }

    // Обработка успешного сканирования
    fun handleSuccessfulScan(barcode: String) {
        onBarcodeScanned(barcode)
    }

    // Явно указываем, что это диалог сканирования, поэтому forceCameraActivation=true
    ScannerListener(
        onBarcodeScanned = { barcode ->
            if (inputMode == "external_scanner") {
                // Проверяем, соответствует ли штрихкод ожидаемому
                if (expectedBarcode != null && barcode != expectedBarcode) {
                    showTemporaryError(barcodeMismatchMessage, barcode)
                } else {
                    // Успешное сканирование
                    handleSuccessfulScan(barcode)
                }
            }
        },
        forceCameraActivation = true // Явно активируем камеру в диалоге сканирования
    )

    // Эффект для закрытия ресурсов при закрытии диалога
    DisposableEffect(Unit) {
        onDispose {
            // Освобождаем ресурсы при закрытии диалога
            Timber.d("Освобождение ресурсов сканера при закрытии диалога")
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
            ) {
                // Заголовок и кнопка закрытия
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = instructionText,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(onClick = onClose) {
                        Text(stringResource(R.string.close))
                    }
                }

                // Переключатель режима ввода (если разрешен ручной ввод)
                if (allowManualInput) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Button(
                            onClick = { inputMode = "camera" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(stringResource(R.string.use_camera))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = { inputMode = "manual" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(stringResource(R.string.manual_input))
                        }
                    }
                }

                // Основная область для сканирования или ручного ввода
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    if (inputMode == "manual") {
                        // Ручной ввод
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                value = manualBarcode,
                                onValueChange = { manualBarcode = it },
                                label = { Text(stringResource(R.string.enter_barcode)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { inputMode = "camera" },
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
                                        text = it.first,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = it.second,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}