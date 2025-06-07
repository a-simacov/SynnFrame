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


class ScannerService(
    private val scannerFactory: BarcodeScannerFactory,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private var scanner: BarcodeScanner? = null
    private val listeners = mutableListOf<ScanResultListener>()
    private val _scannerState = MutableStateFlow<ScannerState>(ScannerState.Uninitialized)
    val scannerState = _scannerState.asStateFlow()

    private val globalScanListener = object : ScanResultListener {
        override fun onScanSuccess(result: ScanResult) {
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

        if (listeners.isNotEmpty() &&
            (_scannerState.value == ScannerState.Initialized ||
                    _scannerState.value == ScannerState.Disabled)) {
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

            if (listeners.isNotEmpty() &&
                hasRealScanner() &&
                (_scannerState.value == ScannerState.Initialized ||
                        _scannerState.value == ScannerState.Disabled)) {
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

            // Если сканер был включен, включаем его снова
            if (wasEnabled) {
                enable()
            }

            Timber.i("Сканер успешно перезапущен")
        }
    }

    // Композабл для простой подписки в Compose-экранах
    @Composable
    fun ScannerEffect(
        onScanResult: (String) -> Unit
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
                addListener(listener)
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

                if (result.isSuccess) {
                    this@ScannerService.scanner = scanner
                    _scannerState.value = ScannerState.Initialized

                    if (listeners.isNotEmpty() && hasRealScanner()) {
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

    fun enable() {
        if (_scannerState.value != ScannerState.Initialized &&
            _scannerState.value != ScannerState.Disabled) return

        coroutineScope.launch {
            try {
                scanner?.let { scanner ->
                        if (scanner is NullBarcodeScanner) {
                        _scannerState.value = ScannerState.Enabled
                        return@launch
                    }

                    _scannerState.value = ScannerState.Enabling

                    val result = scanner.enable(globalScanListener)

                    if (result.isSuccess) {
                        _scannerState.value = ScannerState.Enabled
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

    fun disable() {
        if (_scannerState.value != ScannerState.Enabled) return

        coroutineScope.launch {
            try {
                if (scanner is NullBarcodeScanner) {
                    _scannerState.value = ScannerState.Disabled
                    return@launch
                }

                scanner?.disable()
                _scannerState.value = ScannerState.Disabled
            } catch (e: Exception) {
                _scannerState.value = ScannerState.Error(e.message ?: "Unknown error")
                Timber.e(e, "Exception during scanner disabling")
            }
        }
    }

    fun isEnabled(): Boolean {
        return scannerState.value == ScannerState.Enabled
    }

    fun triggerScan() {
        scanner?.let {
            if (it is DataWedgeBarcodeScanner) {
                it.triggerScan()
            }
        }
    }

    fun dispose() {
        coroutineScope.launch {
            listeners.clear()
            try {
                scanner?.disable()
                scanner?.dispose()
                scanner = null
                _scannerState.value = ScannerState.Uninitialized
            } catch (e: Exception) {
                Timber.e(e, "Exception during scanner disposal")
            }
        }
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