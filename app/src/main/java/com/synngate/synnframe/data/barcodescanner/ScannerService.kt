package com.synngate.synnframe.data.barcodescanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.synngate.synnframe.domain.common.BarcodeScanner
import com.synngate.synnframe.domain.common.ScanError
import com.synngate.synnframe.domain.common.ScanResult
import com.synngate.synnframe.domain.common.ScanResultListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Сервис для централизованного управления сканером
 */
class ScannerService(
    private val scannerFactory: BarcodeScannerFactory,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var scanner: BarcodeScanner? = null
    private val listeners = mutableListOf<ScanResultListener>()
    private val _scannerState = MutableStateFlow<ScannerState>(ScannerState.Uninitialized)
    val scannerState = _scannerState.asStateFlow()

    // Общий обработчик сканирования
    private val globalScanListener = object : ScanResultListener {
        override fun onScanSuccess(result: ScanResult) {
            // Уведомляем всех слушателей
            listeners.forEach { it.onScanSuccess(result) }
        }

        override fun onScanError(error: ScanError) {
            listeners.forEach { it.onScanError(error) }
        }
    }

    // Привязка к экрану с LifecycleOwner (для камеры)
    fun attachToScreen(lifecycleOwner: LifecycleOwner? = null) {
        if (lifecycleOwner != null && scanner is DefaultBarcodeScanner) {
            (scanner as DefaultBarcodeScanner).setLifecycleOwner(lifecycleOwner)
        }

        if (listeners.isNotEmpty() &&
            (_scannerState.value == ScannerState.Initialized ||
                    _scannerState.value == ScannerState.Disabled)) {
            enable()
        }
    }

    // Отвязка от экрана
    fun detachFromScreen() {
        if (listeners.isEmpty() && _scannerState.value == ScannerState.Enabled) {
            disable()
        }
    }

    // Добавить слушателя
    fun addListener(listener: ScanResultListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)

            // Активируем сканер, если есть хотя бы один слушатель
            if (listeners.isNotEmpty() &&
                (_scannerState.value == ScannerState.Initialized ||
                        _scannerState.value == ScannerState.Disabled)) {
                enable()
            }
        }
    }

    // Удалить слушателя
    fun removeListener(listener: ScanResultListener) {
        listeners.remove(listener)

        // Деактивируем сканер, если не осталось слушателей
        if (listeners.isEmpty() && _scannerState.value == ScannerState.Enabled) {
            disable()
        }
    }

    // Композабл для простой подписки в Compose-экранах
    @Composable
    fun ScannerEffect(
        onScanResult: (String) -> Unit
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(lifecycleOwner) {
            // Создаем слушателя
            val listener = object : ScanResultListener {
                override fun onScanSuccess(result: ScanResult) {
                    onScanResult(result.barcode)
                }

                override fun onScanError(error: ScanError) {
                    Timber.e("Scanner error: ${error.message}")
                }
            }

            // Привязываем сканер к экрану и добавляем слушателя
            attachToScreen(lifecycleOwner)
            addListener(listener)

            // При размонтировании удаляем слушателя и отвязываемся от экрана
            onDispose {
                removeListener(listener)
                detachFromScreen()
            }
        }
    }

    // Обновление LifecycleOwner для DefaultBarcodeScanner
    fun updateLifecycleOwner(lifecycleOwner: LifecycleOwner) {
        if (scanner is DefaultBarcodeScanner) {
            (scanner as DefaultBarcodeScanner).setLifecycleOwner(lifecycleOwner)
        }
    }

    // Инициализация
    fun initialize(lifecycleOwner: LifecycleOwner? = null) {
        if (_scannerState.value != ScannerState.Uninitialized) return

        _scannerState.value = ScannerState.Initializing

        coroutineScope.launch {
            try {
                val scanner = scannerFactory.createScanner(lifecycleOwner)
                val result = scanner.initialize()

                if (result.isSuccess) {
                    this@ScannerService.scanner = scanner
                    _scannerState.value = ScannerState.Initialized
                    Timber.i("Scanner initialized: ${scanner.getManufacturer()}")

                    // Если есть слушатели, активируем сканер
                    if (listeners.isNotEmpty()) {
                        enable()
                    }
                } else {
                    _scannerState.value = ScannerState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                    Timber.e(result.exceptionOrNull(), "Failed to initialize scanner")
                }
            } catch (e: Exception) {
                _scannerState.value = ScannerState.Error(e.message ?: "Unknown error")
                Timber.e(e, "Exception during scanner initialization")
            }
        }
    }

    // Активация сканера
    fun enable() {
        if (_scannerState.value != ScannerState.Initialized &&
            _scannerState.value != ScannerState.Disabled) return

        coroutineScope.launch {
            try {
                scanner?.let { scanner ->
                    _scannerState.value = ScannerState.Enabling

                    val result = scanner.enable(globalScanListener)

                    if (result.isSuccess) {
                        _scannerState.value = ScannerState.Enabled
                        Timber.i("Scanner enabled")
                    } else {
                        _scannerState.value = ScannerState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                        Timber.e(result.exceptionOrNull(), "Failed to enable scanner")
                    }
                } ?: run {
                    _scannerState.value = ScannerState.Error("Scanner not initialized")
                    Timber.e("Tried to enable uninitialized scanner")
                }
            } catch (e: Exception) {
                _scannerState.value = ScannerState.Error(e.message ?: "Unknown error")
                Timber.e(e, "Exception during scanner enabling")
            }
        }
    }

    // Деактивация сканера
    fun disable() {
        if (_scannerState.value != ScannerState.Enabled) return

        coroutineScope.launch {
            try {
                scanner?.disable()
                _scannerState.value = ScannerState.Disabled
                Timber.i("Scanner disabled")
            } catch (e: Exception) {
                _scannerState.value = ScannerState.Error(e.message ?: "Unknown error")
                Timber.e(e, "Exception during scanner disabling")
            }
        }
    }

    // Программно запустить сканирование (только для DataWedge)
    fun triggerScan() {
        scanner?.let {
            if (it is DataWedgeBarcodeScanner) {
                it.triggerScan()
            }
        }
    }

    // Полное освобождение ресурсов
    fun dispose() {
        coroutineScope.launch {
            listeners.clear()
            try {
                scanner?.disable()
                scanner?.dispose()
                scanner = null
                _scannerState.value = ScannerState.Uninitialized
                Timber.i("Scanner disposed")
            } catch (e: Exception) {
                Timber.e(e, "Exception during scanner disposal")
            }
        }
    }
}

/**
 * Состояния сканера
 */
sealed class ScannerState {
    object Uninitialized : ScannerState()
    object Initializing : ScannerState()
    object Initialized : ScannerState()
    object Enabling : ScannerState()
    object Enabled : ScannerState()
    object Disabled : ScannerState()
    data class Error(val message: String) : ScannerState()
}