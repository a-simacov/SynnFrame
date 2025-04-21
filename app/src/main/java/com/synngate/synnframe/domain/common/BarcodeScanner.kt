package com.synngate.synnframe.domain.common

/**
 * Интерфейс для взаимодействия со сканерами различных производителей
 */
interface BarcodeScanner {
    /**
     * Инициализация сканера
     * @return Результат инициализации
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Освобождение ресурсов сканера
     */
    suspend fun dispose()

    /**
     * Активация сканера для получения событий сканирования
     * @param listener Слушатель событий сканирования
     * @return Результат активации
     */
    suspend fun enable(listener: ScanResultListener): Result<Unit>

    /**
     * Деактивация сканера
     */
    suspend fun disable()

    /**
     * Проверка, инициализирован ли сканер
     */
    fun isInitialized(): Boolean

    /**
     * Проверка, активирован ли сканер
     */
    fun isEnabled(): Boolean

    /**
     * Получение типа производителя сканера
     */
    fun getManufacturer(): ScannerManufacturer
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