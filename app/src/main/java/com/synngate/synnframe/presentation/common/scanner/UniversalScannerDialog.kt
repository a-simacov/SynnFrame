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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

/**
 * Универсальный диалог сканирования штрихкодов
 *
 * @param onBarcodeScanned Обработчик события сканирования штрихкода
 * @param onClose Обработчик закрытия диалога
 * @param instructionText Текст инструкции (что нужно сканировать)
 * @param expectedBarcode Ожидаемый штрихкод (если задан, будет проверяться соответствие)
 * @param title Заголовок диалога
 * @param allowManualInput Разрешить ручной ввод штрихкода
 * @param modifier Модификатор
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
    // Флаг для предотвращения повторной обработки штрихкода
    var hasProcessedBarcode by remember { mutableStateOf(false) }

    // Состояние ручного ввода
    var isManualInputMode by remember { mutableStateOf(false) }
    var manualBarcode by remember { mutableStateOf("") }

    // Состояние ошибки (если ожидается определенный штрихкод)
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val barcodeMismatchMessage = stringResource(id = R.string.barcode_mismatch)

    // Закрытие диалога при размонтировании
    DisposableEffect(Unit) {
        onDispose {
            if (!hasProcessedBarcode) {
                onClose()
            }
        }
    }

    // Сброс ошибки при изменении ручного ввода
    LaunchedEffect(manualBarcode) {
        errorMessage = null
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
                                onValueChange = { manualBarcode = it },
                                label = { Text(stringResource(R.string.enter_barcode)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                singleLine = true,
                                isError = errorMessage != null
                            )

                            errorMessage?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
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
                                                errorMessage = barcodeMismatchMessage
                                                isProcessing = false
                                            } else {
                                                hasProcessedBarcode = true
                                                onBarcodeScanned(manualBarcode)
                                                isProcessing = false
                                                // Если нет ожидаемого штрихкода, закрываем диалог после сканирования
                                                if (expectedBarcode == null) {
                                                    onClose()
                                                }
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
                        // Режим камеры
                        BarcodeScannerView(
                            onBarcodeDetected = { barcode ->
                                if (!hasProcessedBarcode) {
                                    // Проверяем, соответствует ли отсканированный штрихкод ожидаемому
                                    if (expectedBarcode != null && barcode != expectedBarcode) {
                                        errorMessage = barcodeMismatchMessage
                                    } else {
                                        hasProcessedBarcode = true
                                        onBarcodeScanned(barcode)
                                        // Если нет ожидаемого штрихкода, закрываем диалог после сканирования
                                        if (expectedBarcode == null) {
                                            onClose()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Показываем ошибку поверх камеры
                        errorMessage?.let {
                            Card(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp)
                                    .widthIn(max = 300.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
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
                            onClick = { isManualInputMode = !isManualInputMode },
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