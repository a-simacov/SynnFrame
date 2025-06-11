package com.synngate.synnframe.data.barcodescanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.synngate.synnframe.domain.common.BarcodeScanner
import com.synngate.synnframe.domain.common.ScanError
import com.synngate.synnframe.domain.common.ScanResult
import com.synngate.synnframe.domain.common.ScanResultListener
import com.synngate.synnframe.domain.common.ScannerManufacturer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber


class ScannerService(
    private val scannerFactory: BarcodeScannerFactory,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var scanner: BarcodeScanner? = null
    private val listeners = mutableListOf<ScanResultListener>()
    private val _scannerState = MutableStateFlow<ScannerState>(ScannerState.Uninitialized)
    val scannerState = _scannerState.asStateFlow()

    // Флаг, указывающий, является ли сканер камерой устройства
    private var isCameraScanner = false

    // Добавляем поля для механизма дебаунса
    private var lastScannedBarcode: String? = null
    private var lastScanTime: Long = 0
    private val DEBOUNCE_PERIOD_MS = 500 // 500 мс для фильтрации дубликатов

    private val globalScanListener = object : ScanResultListener {
        override fun onScanSuccess(result: ScanResult) {
            // Защита от дублирования - проверяем, не был ли этот штрихкод недавно обработан
            val currentTime = System.currentTimeMillis()
            if (result.barcode == lastScannedBarcode &&
                currentTime - lastScanTime < DEBOUNCE_PERIOD_MS) {
                Timber.d("Skipping duplicate barcode scan: ${result.barcode}")
                return // Пропускаем повторную обработку
            }

            // Обновляем информацию о последнем сканировании
            lastScannedBarcode = result.barcode
            lastScanTime = currentTime

            listeners.forEach { it.onScanSuccess(result) }
        }

        override fun onScanError(error: ScanError) {
            listeners.forEach { it.onScanError(error) }
        }
    }

    fun attachToScreen(lifecycleOwner: LifecycleOwner? = null) {
        if (lifecycleOwner != null && scanner is DefaultBarcodeScanner) {
            (scanner as DefaultBarcodeScanner).setLifecycleOwner(lifecycleOwner)
        }

        // Активируем сканер только если это не камера устройства или явно вызван диалог сканирования
        if (listeners.isNotEmpty() &&
            (_scannerState.value == ScannerState.Initialized ||
                    _scannerState.value == ScannerState.Disabled) &&
            !isCameraScanner) {
            enable()
        }
    }

    fun detachFromScreen() {
        if (listeners.isEmpty() && _scannerState.value == ScannerState.Enabled) {
            disable()
        }
    }

    fun addListener(listener: ScanResultListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)

            // Активируем сканер только если это не камера устройства или явно вызван диалог сканирования
            if (listeners.isNotEmpty() &&
                hasRealScanner() &&
                (_scannerState.value == ScannerState.Initialized ||
                        _scannerState.value == ScannerState.Disabled) &&
                !isCameraScanner) {
                enable()
            }
        }
    }

    fun removeListener(listener: ScanResultListener, disableOnEmpty: Boolean = true) {
        listeners.remove(listener)

        // Отключаем сканер только если это запрошено и нет других слушателей
        if (disableOnEmpty && listeners.isEmpty() && _scannerState.value == ScannerState.Enabled) {
            disable()
        }
    }

    fun hasRealScanner(): Boolean {
        return scanner != null && scanner !is NullBarcodeScanner
    }

    /**
     * Принудительно активирует сканер, даже если это камера.
     * Используется для диалогов явного сканирования.
     */
    fun triggerScan() {
        // Если это камера, показываем диалог сканирования
        if (isCameraScanner) {
            // Отправляем событие для показа диалога (должен быть реализован на уровне UI)
            Timber.d("Запрошен диалог сканирования камерой")
            // Здесь должна быть реализация для уведомления UI о необходимости показать диалог
        }
        // Для других типов сканеров можно добавить специфическую логику
    }

    fun restart() {
        coroutineScope.launch {
            Timber.i("Перезапуск сканера...")
            val oldListeners = listeners.toList()

            // Сохраняем текущее состояние сканера
            val wasEnabled = isEnabled()

            // Отключаем и освобождаем ресурсы текущего сканера
            disable()
            dispose()

            // Инициализируем новый экземпляр сканера
            initialize()

            // Восстанавливаем слушателей
            oldListeners.forEach { addListener(it) }

            // Если сканер был включен и это не камера, включаем его снова
            if (wasEnabled && !isCameraScanner) {
                enable()
            }

            Timber.i("Сканер успешно перезапущен")
        }
    }

    // Композабл для простой подписки в Compose-экранах
    @Composable
    fun ScannerEffect(
        onScanResult: (String) -> Unit,
        forceCameraActivation: Boolean = false
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(lifecycleOwner) {
            val listener = object : ScanResultListener {
                override fun onScanSuccess(result: ScanResult) {
                    onScanResult(result.barcode)
                }

                override fun onScanError(error: ScanError) {
                    Timber.e("Scanner error: ${error.message}")
                }
            }

            if (hasRealScanner()) {
                attachToScreen(lifecycleOwner)

                // Добавляем слушатель, но активируем камеру только если forceCameraActivation=true
                if (!isCameraScanner || forceCameraActivation) {
                    addListener(listener)
                } else {
                    // Только регистрируем слушатель без активации камеры
                    if (!listeners.contains(listener)) {
                        listeners.add(listener)
                    }
                }
            }

            onDispose {
                removeListener(listener)
                if (hasRealScanner()) {
                    detachFromScreen()
                }
            }
        }
    }

    fun initialize(lifecycleOwner: LifecycleOwner? = null) {
        if (_scannerState.value != ScannerState.Uninitialized) return

        _scannerState.value = ScannerState.Initializing

        coroutineScope.launch {
            try {
                val scanner = scannerFactory.createScanner(lifecycleOwner)
                val result = scanner.initialize()

                // Определяем, является ли сканер камерой устройства
                isCameraScanner = scanner.getManufacturer() == ScannerManufacturer.DEFAULT

                if (result.isSuccess) {
                    this@ScannerService.scanner = scanner
                    _scannerState.value = ScannerState.Initialized

                    // Если есть слушатели и это не камера устройства - включаем сканер
                    if (listeners.isNotEmpty() && hasRealScanner() && !isCameraScanner) {
                        enable()
                    }
                } else {
                    _scannerState.value = ScannerState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize scanner")
                _scannerState.value = ScannerState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun dispose() {
        coroutineScope.launch {
            if (scanner != null) {
                try {
                    scanner?.dispose()
                } catch (e: Exception) {
                    Timber.e(e, "Error disposing scanner")
                }
            }
            scanner = null
            _scannerState.value = ScannerState.Uninitialized
        }
    }

    fun enable() {
        if (_scannerState.value == ScannerState.Enabled) return

        _scannerState.value = ScannerState.Enabling

        coroutineScope.launch {
            try {
                if (scanner == null) {
                    Timber.e("Cannot enable scanner: Scanner is null")
                    _scannerState.value = ScannerState.Error("Scanner is not initialized")
                    return@launch
                }

                val result = scanner?.enable(globalScanListener)
                if (result?.isSuccess == true) {
                    _scannerState.value = ScannerState.Enabled
                } else {
                    val errorMessage = result?.exceptionOrNull()?.message ?: "Unknown error"
                    Timber.e("Failed to enable scanner: $errorMessage")
                    _scannerState.value = ScannerState.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error enabling scanner")
                _scannerState.value = ScannerState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun disable() {
        if (_scannerState.value != ScannerState.Enabled) return

        coroutineScope.launch {
            try {
                scanner?.disable()
                _scannerState.value = ScannerState.Disabled
            } catch (e: Exception) {
                Timber.e(e, "Error disabling scanner")
                // При ошибке отключения, все равно устанавливаем состояние "отключен"
                _scannerState.value = ScannerState.Disabled
            }
        }
    }

    fun isEnabled(): Boolean {
        return _scannerState.value == ScannerState.Enabled
    }

    fun isCameraScanner(): Boolean {
        return isCameraScanner
    }
}

sealed class ScannerState {
    object Uninitialized : ScannerState()
    object Initializing : ScannerState()
    object Initialized : ScannerState()
    object Enabling : ScannerState()
    object Enabled : ScannerState()
    object Disabled : ScannerState()
    data class Error(val message: String) : ScannerState()
}