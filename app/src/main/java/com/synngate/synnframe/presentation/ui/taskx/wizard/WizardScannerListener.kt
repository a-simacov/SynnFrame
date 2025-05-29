package com.synngate.synnframe.presentation.ui.taskx.wizard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.synngate.synnframe.domain.common.ScanResult
import com.synngate.synnframe.domain.common.ScanResultListener
import com.synngate.synnframe.presentation.common.LocalScannerService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

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

    // Внутренний флаг обработки - не влияет на физическое состояние сканера
    var internalProcessingEnabled by remember { mutableStateOf(isEnabled) }

    val scope = rememberCoroutineScope()
    var debounceJob: Job? = null

    LaunchedEffect(isEnabled) {
        internalProcessingEnabled = isEnabled

        // Только логируем состояние, не отключаем сканер
        if (isEnabled) {
            scannerService.resumeProcessing()
        } else {
            scannerService.pauseProcessing()
        }
    }

    // Функция обработки сканирования с защитой от дублирования
    val handleScan = { barcode: String ->
        // Проверяем условия для обработки сканирования
        val currentTime = System.currentTimeMillis()
        val isSameBarcode = barcode == lastScannedBarcode
        val timeDiff = currentTime - lastScanTime

        // Обрабатываем сканирование только если внутренний флаг разрешает
        if (internalProcessingEnabled) {
            // Если это то же самое значение и прошло мало времени, игнорируем
            if (isSameBarcode && timeDiff < 1000) {
                Timber.d("Игнорируем повторное сканирование того же штрихкода: $barcode")
            } else {
                // Отменяем предыдущую задержку, если она была
                debounceJob?.cancel()

                // Обновляем информацию о последнем сканировании
                lastScannedBarcode = barcode
                lastScanTime = currentTime

                // Запускаем обработку с небольшой задержкой для дебаунсинга
                debounceJob = scope.launch {
                    delay(50) // Небольшая задержка для дебаунсинга
                    Timber.d("Обработка сканирования: $barcode")
                    onBarcodeScanned(barcode)
                }
            }
        } else {
            Timber.d("Сканирование получено, но обработка отключена: $barcode")
        }
    }

    DisposableEffect(Unit) {
        // Регистрируем слушатель сканирования всегда
        val scanListener = object : ScanResultListener {
            override fun onScanSuccess(result: ScanResult) {
                handleScan(result.barcode)
            }

            override fun onScanError(error: com.synngate.synnframe.domain.common.ScanError) {
                Timber.e("Ошибка сканирования: ${error.message}")
            }
        }

        // Всегда добавляем слушатель
        scannerService.addListener(scanListener)

        // Возвращаем действие для очистки при размонтировании
        onDispose {
            // ВАЖНО: используем disableOnEmpty=false, чтобы не отключать сканер
            scannerService.removeListener(scanListener, disableOnEmpty = false)
            debounceJob?.cancel()
        }
    }
}