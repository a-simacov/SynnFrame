package com.synngate.synnframe.data.barcodescanner

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.synngate.synnframe.domain.common.BarcodeScanner
import com.synngate.synnframe.domain.common.BarcodeType
import com.synngate.synnframe.domain.common.ScanResult
import com.synngate.synnframe.domain.common.ScanResultListener
import com.synngate.synnframe.domain.common.ScannerManufacturer
import com.synngate.synnframe.util.scanner.BarcodeAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.Executors
import kotlin.coroutines.resume

class DefaultBarcodeScanner(
    private val context: Context
) : BarcodeScanner {
    private var isInitializedState = false
    private var isEnabledState = false
    private var scanListener: ScanResultListener? = null

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysisUseCase: ImageAnalysis? = null

    private var lifecycleOwner: LifecycleOwner? = null

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isInitializedState) {
            return@withContext Result.success(Unit)
        }

        return@withContext try {
            val provider = suspendCancellableCoroutine<ProcessCameraProvider> { continuation ->
                ProcessCameraProvider.getInstance(context).also { future ->
                    future.addListener({
                        try {
                            val cameraProvider = future.get()
                            continuation.resume(cameraProvider)
                        } catch (e: Exception) {
                            Timber.e(e, "Error getting camera provider")
                            continuation.resume(throw e)
                        }
                    }, context.mainExecutor)
                }
            }

            cameraProvider = provider
            isInitializedState = true
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize camera scanner")
            Result.failure(e)
        }
    }

    override suspend fun dispose() {
        disable()

        cameraProvider?.unbindAll()
        cameraProvider = null
        cameraExecutor.shutdown()

        isInitializedState = false
    }

    override suspend fun enable(listener: ScanResultListener): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isInitializedState) {
            return@withContext Result.failure(Exception("Scanner not initialized"))
        }

        if (isEnabledState) {
            scanListener = listener
            return@withContext Result.success(Unit)
        }

        scanListener = listener

        return@withContext try {
            withContext(Dispatchers.Main) {
                if (lifecycleOwner == null) {
                    return@withContext Result.failure(Exception("LifecycleOwner not set for camera"))
                }

                cameraProvider?.unbindAll()

                analysisUseCase = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(
                            cameraExecutor,
                            BarcodeAnalyzer { barcode ->
                                Timber.d("Barcode detected: $barcode")
                                scanListener?.onScanSuccess(
                                    ScanResult(
                                        barcode = barcode,
                                        type = BarcodeType.UNKNOWN // По умолчанию неизвестный тип
                                    )
                                )
                            }
                        )
                    }

                try {
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner!!,
                        cameraSelector,
                        analysisUseCase
                    )

                    isEnabledState = true
                    Result.success(Unit)
                } catch (e: Exception) {
                    Timber.e(e, "Use case binding failed")
                    Result.failure(e)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error enabling camera scanner")
            Result.failure(e)
        }
    }

    override suspend fun disable() {
        if (!isEnabledState) return

        try {
            withContext(Dispatchers.Main) {
                cameraProvider?.unbindAll()
                analysisUseCase = null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error disabling camera scanner")
        } finally {
            isEnabledState = false
            scanListener = null
        }
    }

    override fun isInitialized(): Boolean = isInitializedState

    override fun isEnabled(): Boolean = isEnabledState

    override fun getManufacturer(): ScannerManufacturer = ScannerManufacturer.DEFAULT

    fun setLifecycleOwner(owner: LifecycleOwner) {
        lifecycleOwner = owner
    }
}