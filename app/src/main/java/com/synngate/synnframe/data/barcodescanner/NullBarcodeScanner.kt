package com.synngate.synnframe.data.barcodescanner

import android.content.Context
import com.synngate.synnframe.domain.common.BarcodeScanner
import com.synngate.synnframe.domain.common.ScanResultListener
import com.synngate.synnframe.domain.common.ScannerManufacturer
import timber.log.Timber

/**
 * "Пустая" реализация сканера для устройств, где не используется
 * автоматическое сканирование через камеру
 */
class NullBarcodeScanner(
    private val context: Context
) : BarcodeScanner {
    private var isInitializedState = false
    private var isEnabledState = false

    override suspend fun initialize(): Result<Unit> {
        Timber.d("NullBarcodeScanner initialized")
        isInitializedState = true
        return Result.success(Unit)
    }

    override suspend fun dispose() {
        isInitializedState = false
        isEnabledState = false
        Timber.d("NullBarcodeScanner disposed")
    }

    override suspend fun enable(listener: ScanResultListener): Result<Unit> {
        isEnabledState = true
        Timber.d("NullBarcodeScanner enabled")
        return Result.success(Unit)
    }

    override suspend fun disable() {
        isEnabledState = false
        Timber.d("NullBarcodeScanner disabled")
    }

    override fun isInitialized(): Boolean = isInitializedState

    override fun isEnabled(): Boolean = isEnabledState

    override fun getManufacturer(): ScannerManufacturer = ScannerManufacturer.UNKNOWN
}