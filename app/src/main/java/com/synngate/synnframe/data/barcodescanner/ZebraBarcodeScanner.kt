package com.synngate.synnframe.data.barcodescanner

import android.content.Context
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.barcode.BarcodeManager
import com.symbol.emdk.barcode.ScanDataCollection
import com.symbol.emdk.barcode.Scanner
import com.symbol.emdk.barcode.StatusData
import com.synngate.synnframe.domain.common.BarcodeScanner
import com.synngate.synnframe.domain.common.BarcodeType
import com.synngate.synnframe.domain.common.ScanError
import com.synngate.synnframe.domain.common.ScanResult
import com.synngate.synnframe.domain.common.ScanResultListener
import com.synngate.synnframe.domain.common.ScannerManufacturer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume

// Data слой - конкретные реализации

/**
 * Реализация сканера для устройств Zebra
 */
class ZebraBarcodeScanner(
    private val context: Context
) : BarcodeScanner {
    private var emdkManager: EMDKManager? = null
    private var barcodeManager: BarcodeManager? = null
    private var scanner: Scanner? = null
    private var isInitializedState = false
    private var isEnabledState = false
    private var scanListener: ScanResultListener? = null

    private var dataListener: Scanner.DataListener? = null
    private var statusListener: Scanner.StatusListener? = null

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Проверяем наличие EMDK
            if (!isEmdkAvailable()) {
                return@withContext Result.failure(Exception("EMDK not available on this device"))
            }

            if (isInitializedState) {
                return@withContext Result.success(Unit)
            }

            // Получаем EMDK Manager через корутину и suspend функцию
            val initResult = suspendCancellableCoroutine<Result<Unit>> { continuation ->
                try {
                    EMDKManager.getEMDKManager(context, object : EMDKManager.EMDKListener {
                        override fun onOpened(manager: EMDKManager?) {
                            if (manager != null) {
                                emdkManager = manager
                                // Получаем BarcodeManager
                                barcodeManager = manager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE) as? BarcodeManager
                                if (barcodeManager != null) {
                                    // Перечисляем доступные устройства сканера
                                    try {
                                        val deviceList = barcodeManager?.getSupportedDevicesInfo()
                                        if (deviceList != null && deviceList.isNotEmpty()) {
                                            Timber.d("Available scanner devices:")
                                            for (device in deviceList) {
                                                Timber.d("Device: ${device.friendlyName}, ID: ${device.deviceIdentifier}")
                                            }
                                        } else {
                                            Timber.w("No scanner devices available")
                                        }
                                    } catch (e: Exception) {
                                        Timber.e(e, "Error getting device list")
                                    }

                                    isInitializedState = true
                                    continuation.resume(Result.success(Unit))
                                } else {
                                    continuation.resume(Result.failure(Exception("Failed to get BarcodeManager")))
                                }
                            } else {
                                continuation.resume(Result.failure(Exception("EMDK Manager is null")))
                            }
                        }

                        override fun onClosed() {
                            emdkManager = null
                            barcodeManager = null
                            scanner = null
                            isInitializedState = false
                            isEnabledState = false
                        }
                    })
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }

            initResult
        } catch (e: Exception) {
            Timber.e(e, "Error initializing Zebra scanner")
            Result.failure(e)
        }
    }

    // Проверка наличия EMDK
    private fun isEmdkAvailable(): Boolean {
        return try {
            val emdkClass = Class.forName("com.symbol.emdk.EMDKManager")
            Timber.d("EMDK classes are available on this device")

            // Дополнительная проверка - можем ли мы создать экземпляр EMDK Manager
            try {
                EMDKManager.getEMDKManager(context, object : EMDKManager.EMDKListener {
                    override fun onOpened(manager: EMDKManager?) {
                        if (manager != null) {
                            Timber.d("EMDK Manager successfully opened")
                            manager.release() // Освобождаем сразу после теста
                        } else {
                            Timber.w("EMDK Manager is null after opening")
                        }
                    }

                    override fun onClosed() {
                        Timber.d("EMDK Manager closed")
                    }
                })
                true
            } catch (e: Exception) {
                Timber.e(e, "Error getting EMDK Manager")
                false
            }
        } catch (e: ClassNotFoundException) {
            Timber.w("EMDK classes not available on this device")
            false
        }
    }

    override suspend fun dispose() {
        disable()

        barcodeManager = null
        scanner?.let {
            it.release()
            scanner = null
        }

        emdkManager?.let {
            it.release()
            emdkManager = null
        }

        isInitializedState = false
    }

    override suspend fun enable(listener: ScanResultListener): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!isInitializedState) {
                return@withContext Result.failure(Exception("Scanner not initialized"))
            }

            if (isEnabledState) {
                scanListener = listener
                return@withContext Result.success(Unit)
            }

            scanListener = listener

            // Используем INTERNAL_IMAGER1 вместо DEFAULT
            Timber.d("Trying to get scanner with identifier: INTERNAL_IMAGER1")
            scanner = barcodeManager?.getDevice(BarcodeManager.DeviceIdentifier.INTERNAL_IMAGER1)

            if (scanner == null) {
                Timber.e("Could not get scanner device with INTERNAL_IMAGER1")
                return@withContext Result.failure(Exception("Could not get scanner device"))
            }

            try {
                // Создаем и сохраняем слушатели
                dataListener = object : Scanner.DataListener {
                    override fun onData(scanDataCollection: ScanDataCollection?) {
                        Timber.d("Zebra scanner onData called")
                        if (scanDataCollection != null) {
                            val scanData = scanDataCollection.scanData
                            if (scanData != null && scanData.size > 0) {
                                val data = scanData[0]
                                val barcodeData = data.data
                                val labelType = convertLabelType(data.labelType)

                                Timber.d("Zebra scanner detected barcode: $barcodeData")
                                scanListener?.onScanSuccess(
                                    ScanResult(
                                        barcode = barcodeData,
                                        type = labelType
                                    )
                                )
                            } else {
                                Timber.d("Zebra scanner received empty scan data")
                            }
                        } else {
                            Timber.d("Zebra scanner received null scanDataCollection")
                        }
                    }
                }

                statusListener = object : Scanner.StatusListener {
                    override fun onStatus(statusData: StatusData?) {
                        Timber.d("Zebra scanner onStatus called: ${statusData?.state}")
                        statusData?.let { status ->
                            when (status.state) {
                                StatusData.ScannerStates.ERROR -> {
                                    Timber.e("Scanner error: ${status.friendlyName}")
                                    scanListener?.onScanError(
                                        ScanError(
                                            code = status.friendlyName.hashCode(),
                                            message = "Scanner error: ${status.friendlyName}"
                                        )
                                    )
                                }
                                else -> {
                                    Timber.d("Scanner status changed: ${status.state}, ${status.friendlyName}")
                                }
                            }
                        }
                    }
                }

                // Добавляем слушатели
                scanner!!.addDataListener(dataListener)
                scanner!!.addStatusListener(statusListener)

                // Включение сканера
                Timber.d("Enabling scanner...")
                scanner!!.enable()
                Timber.d("Scanner enabled successfully")

                isEnabledState = true
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Error configuring scanner: ${e.message}")
                Result.failure(e)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error enabling Zebra scanner")
            Result.failure(e)
        }
    }

    override suspend fun disable() {
        if (!isEnabledState) return

        try {
            scanner?.let {
                // Деактивируем сканер
                it.disable()

                // Удаляем слушателей
                dataListener?.let { listener -> it.removeDataListener(listener) }
                statusListener?.let { listener -> it.removeStatusListener(listener) }

                // Очищаем ссылки на слушателей
                dataListener = null
                statusListener = null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error disabling Zebra scanner")
        } finally {
            isEnabledState = false
            scanListener = null
        }
    }

    override fun isInitialized(): Boolean = isInitializedState

    override fun isEnabled(): Boolean = isEnabledState

    override fun getManufacturer(): ScannerManufacturer = ScannerManufacturer.ZEBRA

    // Конвертация типов штрихкодов из формата Zebra в наш формат
    private fun convertLabelType(zebraType: ScanDataCollection.LabelType): BarcodeType {
        return when (zebraType) {
            ScanDataCollection.LabelType.CODE39 -> BarcodeType.CODE39
            ScanDataCollection.LabelType.CODE128 -> BarcodeType.CODE128
            ScanDataCollection.LabelType.EAN13 -> BarcodeType.EAN13
            ScanDataCollection.LabelType.EAN8 -> BarcodeType.EAN8
            ScanDataCollection.LabelType.QRCODE -> BarcodeType.QR
            ScanDataCollection.LabelType.DATAMATRIX -> BarcodeType.DATAMATRIX
            ScanDataCollection.LabelType.PDF417 -> BarcodeType.PDF417
            else -> BarcodeType.UNKNOWN
        }
    }
}