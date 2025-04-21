package com.synngate.synnframe.domain.common

interface BarcodeScanner {
    suspend fun initialize(): Result<Unit>
    suspend fun dispose()
    suspend fun enable(listener: ScanResultListener): Result<Unit>
    suspend fun disable()
    fun isInitialized(): Boolean
    fun isEnabled(): Boolean
    fun getManufacturer(): ScannerManufacturer

    /**
     * Устанавливает слушатель для сканирования штрихкодов
     * @param listener Функция-обработчик результата сканирования или null для отмены
     */
    fun setOnBarcodeScannedListener(listener: ((String) -> Unit)?) {
        // Пустая реализация по умолчанию для обратной совместимости
        // Подклассы должны переопределить этот метод при необходимости
    }
}

/**
 * Типы производителей сканеров
 */
enum class ScannerManufacturer {
    ZEBRA,
    DEFAULT, // Использует камеру устройства
    UNKNOWN
}

/**
 * Интерфейс слушателя результатов сканирования
 */
interface ScanResultListener {
    /**
     * Вызывается при успешном сканировании
     * @param result Результат сканирования
     */
    fun onScanSuccess(result: ScanResult)

    /**
     * Вызывается при ошибке сканирования
     * @param error Информация об ошибке
     */
    fun onScanError(error: ScanError)
}

/**
 * Модель результата сканирования
 */
data class ScanResult(
    val barcode: String,
    val type: BarcodeType,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Типы штрихкодов
 */
enum class BarcodeType {
    EAN13, EAN8, CODE39, CODE128, QR, DATAMATRIX, PDF417, UNKNOWN
}

/**
 * Модель ошибки сканирования
 */
data class ScanError(
    val code: Int,
    val message: String
)