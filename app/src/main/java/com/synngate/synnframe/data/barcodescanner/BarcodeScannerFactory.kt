package com.synngate.synnframe.data.barcodescanner

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.synngate.synnframe.data.service.DeviceDetectionService
import com.synngate.synnframe.domain.common.BarcodeScanner
import com.synngate.synnframe.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber

class BarcodeScannerFactory(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val deviceDetectionService: DeviceDetectionService? = null
) {
    /**
     * Создает сканер на основе типа устройства
     * Если тип устройства не задан, пытается определить его автоматически
     */
    suspend fun createScanner(lifecycleOwner: LifecycleOwner? = null): BarcodeScanner {
        // Получаем тип устройства из настроек
        var deviceType = settingsRepository.getDeviceType().first()

        // Если тип устройства STANDARD и доступен сервис определения,
        // пытаемся автоматически определить тип устройства
        if (deviceType == DeviceType.STANDARD && deviceDetectionService != null) {
            Timber.d("Тип устройства не задан, пытаемся определить автоматически")

            // Определяем тип устройства
            val detectedType = deviceDetectionService.detectDeviceType()

            // Если определенный тип отличается от стандартного, сохраняем его
            if (detectedType != DeviceType.STANDARD) {
                Timber.i("Автоматически определен тип устройства: $detectedType")
                settingsRepository.setDeviceType(detectedType)
                deviceType = detectedType
            }
        }

        Timber.d("Создание сканера для типа устройства: $deviceType")
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