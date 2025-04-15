package com.synngate.synnframe.data.barcodescanner

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.synngate.synnframe.domain.common.BarcodeScanner
import com.synngate.synnframe.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Фабрика создания сканеров в зависимости от типа устройства
 */
class BarcodeScannerFactory(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    suspend fun createScanner(lifecycleOwner: LifecycleOwner? = null): BarcodeScanner {
        val deviceType = settingsRepository.getDeviceType().first()

        return when (deviceType) {
            DeviceType.ZEBRA -> ZebraBarcodeScanner(context)
            // В будущем можно добавить другие типы устройств
            else -> {
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
    // Другие производители можно добавить в будущем
}