package com.synngate.synnframe.data.barcodescanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.synngate.synnframe.domain.common.BarcodeScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Сервис для управления сканером штрихкодов
 */
class ScannerService(
    private val scannerFactory: BarcodeScannerFactory
) : LifecycleObserver, DefaultLifecycleObserver {

    // Состояние сканера
    private val _scannerState = MutableStateFlow<ScannerState>(ScannerState.Uninitialized)
    val scannerState: StateFlow<ScannerState> = _scannerState.asStateFlow()

    // Текущий сканер
    private var scanner: BarcodeScanner? = null

    // Хранит последний отсканированный штрихкод для предотвращения дублирования
    private var lastScannedBarcode: String? = null
    private var lastScanTime: Long = 0

    // Детектор встроенного сканера - NEW!
    private val hardwareScannerDetector = HardwareScannerDetector(scannerFactory.context)

    /**
     * Инициализация сканера
     */
    fun initialize() {
        Timber.d("Инициализация сканера")
        _scannerState.value = ScannerState.Initializing

        try {
            // Проверяем наличие встроенного сканера - NEW!
            val hasHardwareScanner = hardwareScannerDetector.isHardwareScannerAvailable()
            Timber.d("Наличие встроенного сканера: $hasHardwareScanner")

            // Создаем сканер только если встроенный сканер доступен - NEW!
            if (hasHardwareScanner) {
                // Создаем сканер через фабрику (предпочтительно аппаратный)
                scanner = scannerFactory.createScanner(preferHardware = true)

                if (scanner != null) {
                    Timber.d("Сканер успешно создан: ${scanner!!.javaClass.simpleName}")
                    _scannerState.value = ScannerState.Initialized
                } else {
                    // Если сканер не создан, устанавливаем статус ошибки
                    Timber.e("Не удалось создать сканер")
                    _scannerState.value = ScannerState.Error("Не удалось создать сканер")
                }
            } else {
                // Если встроенного сканера нет, НЕ используем камеру автоматически
                Timber.d("Встроенный сканер не обнаружен, камера не будет активирована автоматически")
                _scannerState.value = ScannerState.Disabled
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при инициализации сканера")
            _scannerState.value = ScannerState.Error("Ошибка инициализации: ${e.message}")
        }
    }

    /**
     * Включение сканера
     */
    fun enable() {
        Timber.d("Включение сканера")

        try {
            if (_scannerState.value is ScannerState.Uninitialized) {
                // Если сканер еще не инициализирован, сначала инициализируем его
                initialize()
            }

            if (_scannerState.value is ScannerState.Initialized) {
                _scannerState.value = ScannerState.Enabling

                // Включаем сканер, если он не null
                scanner?.let {
                    it.enable()
                    _scannerState.value = ScannerState.Enabled
                    Timber.d("Сканер успешно включен")
                } ?: run {
                    Timber.w("Сканер не инициализирован, невозможно включить")
                    _scannerState.value = ScannerState.Error("Сканер не инициализирован")
                }
            } else if (_scannerState.value is ScannerState.Disabled) {
                // Если сканер отключен (нет встроенного сканера), просто сообщаем об этом - NEW!
                Timber.d("Сканер отключен, автоматическое включение невозможно")
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при включении сканера")
            _scannerState.value = ScannerState.Error("Ошибка включения: ${e.message}")
        }
    }

    /**
     * Выключение сканера
     */
    fun disable() {
        Timber.d("Выключение сканера")

        try {
            // Выключаем сканер, если он не null
            scanner?.let {
                it.disable()
                _scannerState.value = ScannerState.Disabled
                Timber.d("Сканер успешно выключен")
            } ?: run {
                // Если сканер null, просто устанавливаем состояние Disabled
                _scannerState.value = ScannerState.Disabled
                Timber.d("Сканер уже был выключен")
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при выключении сканера")
            _scannerState.value = ScannerState.Error("Ошибка выключения: ${e.message}")
        }
    }

    /**
     * Проверка включен ли сканер
     */
    fun isEnabled(): Boolean {
        return _scannerState.value == ScannerState.Enabled
    }

    /**
     * Проверка наличия встроенного сканера - NEW!
     */
    fun hasHardwareScanner(): Boolean {
        return hardwareScannerDetector.isHardwareScannerAvailable()
    }

    /**
     * Принудительно создает сканер, использующий камеру - NEW!
     * Используется для явного запроса сканирования камерой
     */
    fun createCameraScanner() {
        Timber.d("Создание сканера на основе камеры (по запросу)")

        try {
            // Если уже есть сканер, сначала выключаем его
            scanner?.disable()

            // Создаем новый сканер, использующий камеру
            scanner = scannerFactory.createCameraScanner()

            if (scanner != null) {
                _scannerState.value = ScannerState.Initialized
                Timber.d("Сканер на основе камеры успешно создан")
            } else {
                _scannerState.value = ScannerState.Error("Не удалось создать сканер на основе камеры")
                Timber.e("Не удалось создать сканер на основе камеры")
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при создании сканера на основе камеры: ${e.message}")
            _scannerState.value = ScannerState.Error("Ошибка создания: ${e.message}")
        }
    }

    /**
     * Возвращает к использованию предпочтительного сканера - NEW!
     * Возвращается к встроенному сканеру, если он доступен
     */
    fun restorePreferredScanner() {
        Timber.d("Восстановление предпочтительного сканера")

        // Выключаем текущий сканер
        scanner?.disable()
        scanner = null

        // Инициализируем сканер заново
        initialize()
    }

    /**
     * Обработка штрихкода
     * @param barcode Штрихкод
     * @param callback Функция обратного вызова для обработки штрихкода
     */
    fun processBarcode(barcode: String, callback: (String) -> Unit) {
        val currentTime = System.currentTimeMillis()

        // Предотвращаем дублирование сканирования
        if (barcode == lastScannedBarcode && currentTime - lastScanTime < 2000) {
            // Игнорируем повторное сканирование в течение 2 секунд
            Timber.d("Игнорирование повторного сканирования: $barcode")
            return
        }

        // Сохраняем последний отсканированный штрихкод и время сканирования
        lastScannedBarcode = barcode
        lastScanTime = currentTime

        // Вызываем callback
        callback(barcode)
    }

    /**
     * Освобождение ресурсов
     */
    fun dispose() {
        Timber.d("Освобождение ресурсов ScannerService")

        try {
            // Выключаем сканер, если он не null
            scanner?.let {
                it.disable()
                Timber.d("Сканер выключен")
            }

            // Освобождаем ресурсы сканера
            scanner?.dispose()
            scanner = null

            // Устанавливаем состояние
            _scannerState.value = ScannerState.Uninitialized

        } catch (e: Exception) {
            Timber.e(e, "Ошибка при освобождении ресурсов сканера")
        }
    }

    // Реализация методов LifecycleObserver
    override fun onPause(owner: LifecycleOwner) {
        Timber.d("onPause: отключение сканера")
        disable()
    }

    override fun onResume(owner: LifecycleOwner) {
        Timber.d("onResume: включение сканера")

        // Проверяем, был ли инициализирован сканер и есть ли встроенный сканер - NEW!
        if (_scannerState.value !is ScannerState.Uninitialized && hasHardwareScanner()) {
            enable()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Timber.d("onDestroy: освобождение ресурсов")
        dispose()
    }

    /**
     * Composable функция для эффекта сканера
     * @param onScanResult Функция обратного вызова для обработки результата сканирования
     */
    @Composable
    fun ScannerEffect(onScanResult: (String) -> Unit) {
        val barcodeScannerState by scannerState.collectAsState(initial = ScannerState.Uninitialized)

        // Эффект подписки на штрихкоды
        LaunchedEffect(Unit) {
            // Подписываемся на сканирование, только если есть встроенный сканер - NEW!
            if (hasHardwareScanner()) {
                scanner?.setOnBarcodeScannedListener { barcode ->
                    processBarcode(barcode, onScanResult)
                }
            }
        }

        // Эффект очистки при удалении composable
        DisposableEffect(Unit) {
            onDispose {
                // Отписываемся от сканирования
                scanner?.setOnBarcodeScannedListener(null)
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