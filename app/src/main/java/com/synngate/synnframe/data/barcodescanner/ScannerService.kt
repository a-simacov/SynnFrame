package com.synngate.synnframe.data.barcodescanner

import androidx.lifecycle.LifecycleOwner
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

    private var lifecycleOwner: LifecycleOwner? = null

    // Обновленная инициализация сканера с поддержкой LifecycleOwner
    fun initialize(lifecycleOwner: LifecycleOwner? = null) {
        if (_scannerState.value != ScannerState.Uninitialized) return

        this.lifecycleOwner = lifecycleOwner
        _scannerState.value = ScannerState.Initializing

        coroutineScope.launch {
            try {
                // Создаем сканер с lifecycleOwner
                val scanner = scannerFactory.createScanner(lifecycleOwner)
                val result = scanner.initialize()

                if (result.isSuccess) {
                    this@ScannerService.scanner = scanner
                    _scannerState.value = ScannerState.Initialized
                    Timber.i("Scanner initialized: ${scanner.getManufacturer()}")

                    // Если есть слушатели, автоматически активируем сканер
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

                    val compositeListener = object : ScanResultListener {
                        override fun onScanSuccess(result: ScanResult) {
                            listeners.forEach { it.onScanSuccess(result) }
                        }

                        override fun onScanError(error: ScanError) {
                            listeners.forEach { it.onScanError(error) }
                        }
                    }

                    val result = scanner.enable(compositeListener)

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

    // Метод для обновления LifecycleOwner (используется при изменении)
    fun updateLifecycleOwner(owner: LifecycleOwner) {
        val currentState = _scannerState.value
        val wasEnabled = currentState == ScannerState.Enabled

        Timber.d("Updating lifecycleOwner, current scanner type: ${scanner?.getManufacturer()}, state: $currentState")

        // Если это DefaultBarcodeScanner, обновляем его LifecycleOwner
        if (scanner is DefaultBarcodeScanner) {
            coroutineScope.launch {
                // Если сканер был активен, сначала деактивируем его
                if (wasEnabled) {
                    disable()
                }

                (scanner as DefaultBarcodeScanner).setLifecycleOwner(owner)
                lifecycleOwner = owner

                // Если сканер был активен, активируем его снова с новым LifecycleOwner
                if (wasEnabled) {
                    enable()
                }
            }
        } else {
            // Для других типов сканеров просто сохраняем LifecycleOwner для будущего использования
            Timber.d("Scanner is not DefaultBarcodeScanner, just storing lifecycleOwner")
            lifecycleOwner = owner
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

    // Добавление слушателя сканирования
    fun addListener(listener: ScanResultListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)

            // Если есть хотя бы один слушатель и сканер находится в состоянии Initialized или Disabled,
            // автоматически активируем сканер
            if (listeners.isNotEmpty() &&
                (_scannerState.value == ScannerState.Initialized ||
                        _scannerState.value == ScannerState.Disabled)) {
                enable()
            }
        }
    }

    // Удаление слушателя сканирования
    fun removeListener(listener: ScanResultListener) {
        listeners.remove(listener)

        // Если не осталось слушателей и сканер активен, деактивируем его
        if (listeners.isEmpty() && _scannerState.value == ScannerState.Enabled) {
            disable()
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

    // Получение текущего состояния
    fun getState(): ScannerState = _scannerState.value
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