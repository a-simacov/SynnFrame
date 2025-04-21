package com.synngate.synnframe.data.barcodescanner

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.synngate.synnframe.domain.common.BarcodeScanner
import com.synngate.synnframe.domain.common.ScanError
import com.synngate.synnframe.domain.common.ScanResult
import com.synngate.synnframe.domain.common.ScanResultListener
import com.synngate.synnframe.domain.common.ScannerManufacturer
import timber.log.Timber

/**
 * Реализация сканера штрихкодов на основе камеры устройства
 */
class CameraScanner(
    private val context: Context
) : BarcodeScanner {
    private val defaultScanner = DefaultBarcodeScanner(context)
    private var scanListener: ScanResultListener? = null
    private var onBarcodeListener: ((String) -> Unit)? = null
    private var isInitializedState = false
    private var isEnabledState = false

    /**
     * Устанавливает LifecycleOwner для работы с камерой
     */
    fun setLifecycleOwner(owner: LifecycleOwner) {
        defaultScanner.setLifecycleOwner(owner)
    }

    override suspend fun initialize(): Result<Unit> {
        if (isInitializedState) {
            return Result.success(Unit)
        }

        return try {
            val result = defaultScanner.initialize()
            isInitializedState = result.isSuccess
            result
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize camera scanner")
            Result.failure(e)
        }
    }

    override suspend fun dispose() {
        try {
            defaultScanner.disable()
            defaultScanner.dispose()
            isInitializedState = false
            isEnabledState = false
            scanListener = null
            onBarcodeListener = null
        } catch (e: Exception) {
            Timber.e(e, "Error disposing camera scanner")
        }
    }

    override suspend fun enable(listener: ScanResultListener): Result<Unit> {
        if (!isInitializedState) {
            return Result.failure(Exception("Scanner not initialized"))
        }

        scanListener = listener

        return try {
            // Оборачиваем существующий слушатель, чтобы обрабатывать callback для onBarcodeListener
            val wrappedListener = object : ScanResultListener {
                override fun onScanSuccess(result: ScanResult) {
                    listener.onScanSuccess(result)
                    onBarcodeListener?.invoke(result.barcode)
                }

                override fun onScanError(error: ScanError) {
                    listener.onScanError(error)
                }
            }

            val result = defaultScanner.enable(wrappedListener)
            isEnabledState = result.isSuccess
            result
        } catch (e: Exception) {
            Timber.e(e, "Error enabling camera scanner")
            Result.failure(e)
        }
    }

    override suspend fun disable() {
        try {
            defaultScanner.disable()
            isEnabledState = false
        } catch (e: Exception) {
            Timber.e(e, "Error disabling camera scanner")
        }
    }

    override fun isInitialized(): Boolean = isInitializedState

    override fun isEnabled(): Boolean = isEnabledState

    override fun getManufacturer(): ScannerManufacturer = ScannerManufacturer.DEFAULT

    /**
     * Устанавливает слушатель для сканирования штрихкодов
     * @param listener Функция-обработчик результата сканирования или null для отмены
     */
    fun setOnBarcodeScannedListener(listener: ((String) -> Unit)?) {
        onBarcodeListener = listener
    }
}