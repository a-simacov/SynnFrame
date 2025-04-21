package com.synngate.synnframe.presentation.common.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.synngate.synnframe.presentation.common.LocalScannerService
import timber.log.Timber

/**
 * Компонент для унифицированной обработки сканирования штрихкодов
 * с защитой от повторной обработки и возможностью сброса состояния
 */
@Composable
fun BarcodeHandler(
    stepKey: Any,
    stepResult: Any? = null,
    onBarcodeScanned: (String) -> Unit
) {
    // Флаг обработки штрихкода - сбрасывается при изменении шага или результата
    var isProcessingBarcode by remember(stepKey, stepResult) { mutableStateOf(false) }

    // Получаем сканер сервис
    val scannerService = LocalScannerService.current

    // Сброс флага при инициализации компонента
    LaunchedEffect(stepKey) {
        isProcessingBarcode = false
        Timber.d("BarcodeHandler: Сброс флага isProcessingBarcode для шага $stepKey")

        // Проверяем наличие встроенного сканера - NEW!
        scannerService?.let {
            if (it.hasHardwareScanner()) {
                // Включаем сканер только при наличии встроенного сканера
                if (!it.isEnabled()) {
                    it.enable()
                    Timber.d("BarcodeHandler: Включение встроенного сканера для шага $stepKey")
                } else {
                    Timber.d("BarcodeHandler: Сканер уже включен для шага $stepKey")
                }
            } else {
                Timber.d("BarcodeHandler: Встроенный сканер отсутствует, камера не будет активирована автоматически")
            }
        }
    }

    // Освобождение сканера при удалении компонента
    DisposableEffect(stepKey) {
        onDispose {
            if (isProcessingBarcode) {
                isProcessingBarcode = false
                Timber.d("BarcodeHandler: Сброс флага isProcessingBarcode при размонтировании для шага $stepKey")
            }
        }
    }

    // Слушатель сканирования - только для встроенного сканера
    scannerService?.let { service ->
        if (service.hasHardwareScanner()) {
            ScannerListener(
                onBarcodeScanned = { barcode ->
                    if (!isProcessingBarcode) {
                        Timber.d("BarcodeHandler: Получен штрихкод: $barcode для шага $stepKey")
                        isProcessingBarcode = true

                        // Вызываем обработчик
                        onBarcodeScanned(barcode)

                        // Не сбрасываем флаг автоматически, это должен делать вызывающий код
                        // в случае ошибки обработки
                    } else {
                        Timber.d("BarcodeHandler: Игнорирование штрихкода, т.к. текущий штрихкод обрабатывается")
                    }
                }
            )
        }
    }
}

/**
 * Расширенная версия с возможностью управления состоянием обработки
 */
@Composable
fun BarcodeHandlerWithState(
    stepKey: Any,
    stepResult: Any? = null,
    onBarcodeScanned: (String, (Boolean) -> Unit) -> Unit
) {
    // Флаг обработки штрихкода - сбрасывается при изменении шага или результата
    var isProcessingBarcode by remember(stepKey, stepResult) { mutableStateOf(false) }

    // Получаем сканер сервис
    val scannerService = LocalScannerService.current

    // Сброс флага при инициализации компонента
    LaunchedEffect(stepKey) {
        isProcessingBarcode = false
        Timber.d("BarcodeHandlerWithState: Сброс флага isProcessingBarcode для шага $stepKey")

        // Проверяем наличие встроенного сканера - NEW!
        scannerService?.let {
            if (it.hasHardwareScanner()) {
                // Включаем сканер только при наличии встроенного сканера
                if (!it.isEnabled()) {
                    it.enable()
                    Timber.d("BarcodeHandlerWithState: Включение встроенного сканера для шага $stepKey")
                } else {
                    Timber.d("BarcodeHandlerWithState: Сканер уже включен для шага $stepKey")
                }
            } else {
                Timber.d("BarcodeHandlerWithState: Встроенный сканер отсутствует, камера не будет активирована автоматически")
            }
        }
    }

    // Функция для управления состоянием обработки
    val setProcessingState = { newState: Boolean ->
        isProcessingBarcode = newState
    }

    // Слушатель сканирования - только для встроенного сканера
    scannerService?.let { service ->
        if (service.hasHardwareScanner()) {
            ScannerListener(
                onBarcodeScanned = { barcode ->
                    if (!isProcessingBarcode) {
                        Timber.d("BarcodeHandlerWithState: Получен штрихкод: $barcode для шага $stepKey")
                        isProcessingBarcode = true

                        // Вызываем обработчик с функцией обратного вызова для управления состоянием
                        onBarcodeScanned(barcode, setProcessingState)
                    } else {
                        Timber.d("BarcodeHandlerWithState: Игнорирование штрихкода, т.к. текущий штрихкод обрабатывается")
                    }
                }
            )
        }
    }

    // Освобождение сканера при удалении компонента - только отмечаем обработку как завершенную,
    // но не отключаем сканер, так как он может использоваться другими компонентами
    DisposableEffect(stepKey) {
        onDispose {
            if (isProcessingBarcode) {
                isProcessingBarcode = false
                Timber.d("BarcodeHandlerWithState: Сброс флага isProcessingBarcode при размонтировании для шага $stepKey")
            }
        }
    }
}