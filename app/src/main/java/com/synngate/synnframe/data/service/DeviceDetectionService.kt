package com.synngate.synnframe.data.service

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import com.synngate.synnframe.data.barcodescanner.DeviceType
import com.synngate.synnframe.data.datastore.AppSettingsDataStore
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Сервис для автоматического определения типа устройства и сканера
 */
class DeviceDetectionService(
    private val context: Context,
    private val appSettingsDataStore: AppSettingsDataStore? = null
) {

    /**
     * Определяет тип устройства для сканирования
     * @return Тип устройства ([DeviceType])
     */
    suspend fun detectDeviceType(respectManualSettings: Boolean = true): DeviceType {
        Timber.d("Начало автоматического определения типа устройства")

        // Проверяем, не установлен ли тип устройства вручную
        if (respectManualSettings && appSettingsDataStore != null) {
            val manuallySet = appSettingsDataStore.deviceTypeManuallySet.first()
            if (manuallySet) {
                val currentType = appSettingsDataStore.deviceType.first()
                Timber.i("Тип устройства установлен вручную: $currentType, пропускаем автоопределение")
                return currentType
            }
        }

        // Сначала проверяем устройства Zebra с DataWedge
        if (isZebraDevice() && hasDataWedgeSupport()) {
            Timber.i("Обнаружено устройство Zebra с поддержкой DataWedge")
            return DeviceType.ZEBRA_DATAWEDGE
        }

        // Затем проверяем наличие камеры для использования как сканер
        if (hasCamera()) {
            Timber.i("Устройство имеет камеру, будет использоваться для сканирования")
            return DeviceType.CAMERA_SCANNER
        }

        // По умолчанию используем стандартный тип
        Timber.i("Не удалось определить специальный тип сканера, используется стандартный")
        return DeviceType.STANDARD
    }

    /**
     * Проверяет, является ли устройство продуктом Zebra
     */
    private fun isZebraDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        val model = Build.MODEL?.lowercase() ?: ""

        val isZebra = manufacturer.contains("zebra") ||
                model.contains("tc") ||
                model.contains("mc") ||
                model.contains("ec") ||
                model.contains("tc52") ||
                model.contains("tc57") ||
                model.contains("tc8000") ||
                model.startsWith("mc3") ||
                model.startsWith("mc9") ||
                model.startsWith("et")

        Timber.d("Проверка на устройство Zebra: $isZebra (manufacturer: $manufacturer, model: $model)")
        return isZebra
    }

    /**
     * Проверяет наличие поддержки DataWedge на устройстве
     */
    private fun hasDataWedgeSupport(): Boolean {
        return try {
            // Проверяем наличие пакета DataWedge
            val packageManager = context.packageManager

            // Проверка по наличию пакета DataWedge
            val datawedgePackage = "com.symbol.datawedge"
            val datawedgeApiPackage = "com.symbol.datawedge.api"

            val hasDataWedge = try {
                packageManager.getPackageInfo(datawedgePackage, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }

            val hasDataWedgeApi = try {
                packageManager.getPackageInfo(datawedgeApiPackage, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }

            // Проверяем наличие intent-фильтра для DataWedge
            val intent = context.packageManager.getLaunchIntentForPackage(datawedgePackage)
            val hasLaunchIntent = intent != null

            val result = hasDataWedge || hasDataWedgeApi || hasLaunchIntent
            Timber.d("Проверка поддержки DataWedge: $result (hasDataWedge: $hasDataWedge, hasDataWedgeApi: $hasDataWedgeApi, hasLaunchIntent: $hasLaunchIntent)")

            result
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при проверке поддержки DataWedge")
            false
        }
    }

    /**
     * Проверяет наличие камеры для использования в качестве сканера
     */
    private fun hasCamera(): Boolean {
        return try {
            // Проверяем через feature
            val hasFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

            // Дополнительно проверяем через CameraManager
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraIds = cameraManager?.cameraIdList
            val hasCameras = cameraIds?.isNotEmpty() == true

            val result = hasFeature && hasCameras
            Timber.d("Проверка наличия камеры: $result (hasFeature: $hasFeature, hasCameras: $hasCameras)")

            result
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при проверке наличия камеры")
            false
        }
    }
}