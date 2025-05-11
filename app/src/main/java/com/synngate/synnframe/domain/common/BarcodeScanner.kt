package com.synngate.synnframe.domain.common

 // Интерфейс для взаимодействия со сканерами различных производителей
interface BarcodeScanner {
    suspend fun initialize(): Result<Unit>

    suspend fun dispose()

    suspend fun enable(listener: ScanResultListener): Result<Unit>

    suspend fun disable()

    fun isInitialized(): Boolean

    fun isEnabled(): Boolean

    fun getManufacturer(): ScannerManufacturer
}

enum class ScannerManufacturer {
    ZEBRA,
    DEFAULT, // Использует камеру устройства
    UNKNOWN
}

interface ScanResultListener {

    fun onScanSuccess(result: ScanResult)

    fun onScanError(error: ScanError)
}

data class ScanResult(
    val barcode: String,
    val type: BarcodeType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class BarcodeType {
    EAN13, EAN8, CODE39, CODE128, QR, DATAMATRIX, PDF417, UNKNOWN
}

data class ScanError(
    val code: Int,
    val message: String
)