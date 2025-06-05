package com.synngate.synnframe.data.barcodescanner

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.synngate.synnframe.domain.common.BarcodeScanner
import com.synngate.synnframe.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class BarcodeScannerFactory(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    suspend fun createScanner(lifecycleOwner: LifecycleOwner? = null): BarcodeScanner {
        val deviceType = settingsRepository.getDeviceType().first()

        return when (deviceType) {
            DeviceType.ZEBRA_DATAWEDGE -> {
                DataWedgeBarcodeScanner(context)
            }
            DeviceType.CAMERA_SCANNER -> {
                val scanner = DefaultBarcodeScanner(context)
                lifecycleOwner?.let { scanner.setLifecycleOwner(it) }
                scanner
            }
            else -> {
                NullBarcodeScanner()
            }
        }
    }
}

enum class DeviceType {
    STANDARD,
    ZEBRA_DATAWEDGE,
    CAMERA_SCANNER
}