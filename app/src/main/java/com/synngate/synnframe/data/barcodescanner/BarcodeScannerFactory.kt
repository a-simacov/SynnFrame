package com.synngate.synnframe.data.barcodescanner

import android.content.Context
import com.synngate.synnframe.domain.common.BarcodeScanner
import com.synngate.synnframe.domain.repository.SettingsRepository
import timber.log.Timber

/**
 * Фабрика для создания сканеров штрихкодов
 */
class BarcodeScannerFactory(
    val context: Context,
    private val settingsRepository: SettingsRepository
) {
    // Детектор встроенного сканера
    private val hardwareScannerDetector = HardwareScannerDetector(context)

    /**
     * Создает сканер в зависимости от настроек и доступности оборудования
     * @param preferHardware Предпочитать аппаратный сканер
     * @return BarcodeScanner или null, если не удалось создать сканер
     */
    fun createScanner(preferHardware: Boolean = true): BarcodeScanner? {
        Timber.d("Создание сканера с preferHardware = $preferHardware")

        // Проверка наличия встроенного сканера
        val hasHardwareScanner = hardwareScannerDetector.isHardwareScannerAvailable()

        if (hasHardwareScanner && preferHardware) {
            Timber.d("Доступен встроенный сканер, пытаемся создать")

            // Пробуем создать различные типы аппаратных сканеров
            createDataWedgeScanner()?.let {
                Timber.d("Создан сканер DataWedge")
                return it
            }

            createHoneywellScanner()?.let {
                Timber.d("Создан сканер Honeywell")
                return it
            }

            // Если не удалось создать ни один из известных типов сканеров,
            // пробуем создать дженерик-сканер
            createGenericScanner()?.let {
                Timber.d("Создан универсальный аппаратный сканер")
                return it
            }
        }

        // Если аппаратный сканер не доступен или не предпочтителен,
        // НЕ пытаемся автоматически создать сканер на основе камеры
        Timber.d("Аппаратный сканер не доступен или не предпочтителен. Камера не будет использована автоматически.")
        return null
    }

    /**
     * Создает сканер DataWedge (для устройств Zebra)
     * @return DataWedgeScanner или null, если не удалось создать
     */
    private fun createDataWedgeScanner(): BarcodeScanner? {
        return try {
            // Проверяем наличие класса DataWedgeScanner
            val scannerClass = Class.forName("com.synngate.synnframe.data.barcodescanner.datawedge.DataWedgeScanner")
            val constructor = scannerClass.getConstructor(Context::class.java)
            constructor.newInstance(context) as? BarcodeScanner
        } catch (e: Exception) {
            Timber.d("Не удалось создать DataWedgeScanner: ${e.message}")
            null
        }
    }

    /**
     * Создает сканер Honeywell
     * @return HoneywellScanner или null, если не удалось создать
     */
    private fun createHoneywellScanner(): BarcodeScanner? {
        return try {
            // Проверяем наличие класса HoneywellScanner
            val scannerClass = Class.forName("com.synngate.synnframe.data.barcodescanner.honeywell.HoneywellScanner")
            val constructor = scannerClass.getConstructor(Context::class.java)
            constructor.newInstance(context) as? BarcodeScanner
        } catch (e: Exception) {
            Timber.d("Не удалось создать HoneywellScanner: ${e.message}")
            null
        }
    }

    /**
     * Создает универсальный аппаратный сканер
     * @return GenericScanner или null, если не удалось создать
     */
    private fun createGenericScanner(): BarcodeScanner? {
        return try {
            // Проверяем наличие класса GenericScanner
            val scannerClass = Class.forName("com.synngate.synnframe.data.barcodescanner.generic.GenericScanner")
            val constructor = scannerClass.getConstructor(Context::class.java)
            constructor.newInstance(context) as? BarcodeScanner
        } catch (e: Exception) {
            Timber.d("Не удалось создать GenericScanner: ${e.message}")
            null
        }
    }

    /**
     * Явно создает сканер на основе камеры - NEW!
     * @return CameraScanner или null, если не удалось создать
     */
    fun createCameraScanner(): BarcodeScanner? {
        Timber.d("Явное создание сканера на основе камеры")

        return try {
            // Создаем CameraScanner
            CameraScanner(context)
        } catch (e: Exception) {
            Timber.e(e, "Не удалось создать CameraScanner: ${e.message}")
            null
        }
    }

    /**
     * Проверяет наличие встроенного сканера
     * @return true, если встроенный сканер доступен
     */
    fun hasHardwareScanner(): Boolean {
        return hardwareScannerDetector.isHardwareScannerAvailable()
    }
}