package com.synngate.synnframe.presentation.ui.taskx.wizard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.synngate.synnframe.presentation.common.LocalScannerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Улучшенный компонент для прослушивания сканирования штрихкодов с защитой от дублирования
 */
@Composable
fun WizardScannerListener(
    onBarcodeScanned: (String) -> Unit,
    isEnabled: Boolean = true
) {
    val scannerService = LocalScannerService.current ?: return

    // Если сканер недоступен, просто выходим
    if (!scannerService.hasRealScanner()) return

    // Запоминаем последний отсканированный штрихкод
    var lastScannedBarcode by remember { mutableStateOf("") }

    // Запоминаем время последнего сканирования
    var lastScanTime by remember { mutableLongStateOf(0L) }

    // Флаг, указывающий, что обработка сканирования в процессе
    val isProcessing = remember { AtomicBoolean(false) }

    // Используем coroutineScope для управления задачами
    val scope = rememberCoroutineScope()
    var resetJob: Job? = null

    // Функция обработки сканирования
    val handleScan = { barcode: String ->
        // Проверяем условия для обработки сканирования
        val currentTime = System.currentTimeMillis()
        val shouldProcess = isEnabled &&
                !isProcessing.get() &&
                (barcode != lastScannedBarcode || currentTime - lastScanTime >= 1000)

        if (shouldProcess) {
            // Устанавливаем флаг обработки
            isProcessing.set(true)

            // Обновляем информацию о последнем сканировании
            lastScannedBarcode = barcode
            lastScanTime = currentTime

            // Вызываем обработчик
            Timber.d("Обработка сканирования: $barcode")
            onBarcodeScanned(barcode)

            // Запускаем таймер для сброса флага обработки
            resetJob?.cancel()
            resetJob = scope.launch {
                delay(2000) // Ждем 2 секунды
                isProcessing.set(false)
                Timber.d("Сброс флага обработки сканирования")
            }
        } else {
            Timber.d("Сканирование игнорируется: enabled=$isEnabled, processing=${isProcessing.get()}, " +
                    "same=${barcode == lastScannedBarcode}, timeDiff=${currentTime - lastScanTime}")
        }
    }

    // Правильно используем DisposableEffect для очистки ресурсов
    DisposableEffect(isEnabled) {
        // Регистрируем слушатель сканирования
        val scanListener = object : com.synngate.synnframe.domain.common.ScanResultListener {
            override fun onScanSuccess(result: com.synngate.synnframe.domain.common.ScanResult) {
                handleScan(result.barcode)
            }

            override fun onScanError(error: com.synngate.synnframe.domain.common.ScanError) {
                Timber.e("Ошибка сканирования: ${error.message}")
            }
        }

        if (isEnabled) {
            scannerService.addListener(scanListener)
        }

        // Возвращаем действие для очистки при размонтировании
        onDispose {
            if (isEnabled) {
                scannerService.removeListener(scanListener)
            }
            resetJob?.cancel()
        }
    }
}