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
            DeviceType.ZEBRA_DATAWEDGE -> {
                Timber.d("Creating DataWedgeBarcodeScanner")
                DataWedgeBarcodeScanner(context)
            }
            DeviceType.CAMERA_SCANNER -> {
                // Явно указано использование камеры как сканера
                Timber.d("Creating DefaultBarcodeScanner (camera) by explicit setting")
                val scanner = DefaultBarcodeScanner(context)
                lifecycleOwner?.let { scanner.setLifecycleOwner(it) }
                scanner
            }
            else -> {
                // Для стандартного типа и других неуказанных типов используем NullBarcodeScanner
                Timber.d("Creating NullBarcodeScanner - no built-in scanner available")
                NullBarcodeScanner(context)
            }
        }
    }
}

/**
 * Типы поддерживаемых устройств
 */
enum class DeviceType {
    STANDARD,        // Обычное Android-устройство без автоматического сканирования
    ZEBRA_DATAWEDGE, // Устройство Zebra с DataWedge
    CAMERA_SCANNER   // Устройство, где камера используется как сканер
}