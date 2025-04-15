package com.synngate.synnframe.data.barcodescanner

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.synngate.synnframe.domain.common.BarcodeScanner
import com.synngate.synnframe.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Фабрика создания сканеров в зависимости от типа устройства
 */
class BarcodeScannerFactory(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    suspend fun createScanner(lifecycleOwner: LifecycleOwner? = null): BarcodeScanner {
        val deviceType = settingsRepository.getDeviceType().first()
        Timber.d("Device type from settings: $deviceType")

        return when (deviceType) {
            DeviceType.ZEBRA -> {
                Timber.d("Creating ZebraBarcodeScanner")
                ZebraBarcodeScanner(context)
            }
            DeviceType.ZEBRA_DATAWEDGE -> {
                Timber.d("Creating DataWedgeBarcodeScanner")
                DataWedgeBarcodeScanner(context)
            }
            else -> {
                Timber.d("Creating DefaultBarcodeScanner")
                val scanner = DefaultBarcodeScanner(context)
                lifecycleOwner?.let { scanner.setLifecycleOwner(it) }
                scanner
            }
        }
    }
}

/**
 * Типы поддерживаемых устройств
 */
enum class DeviceType {
    STANDARD, // Обычное Android-устройство
    ZEBRA,    // Устройство Zebra
    ZEBRA_DATAWEDGE
    // Другие производители можно добавить в будущем
}