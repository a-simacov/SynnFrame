package com.synngate.synnframe.data.barcodescanner

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import timber.log.Timber

/**
 * Класс для определения наличия встроенного сканера на устройстве
 */
class HardwareScannerDetector(private val context: Context) {

    // Кэшированный результат обнаружения сканера
    private var cachedResult: Boolean? = null

    /**
     * Проверяет наличие встроенного сканера на устройстве
     * @return true если встроенный сканер обнаружен, false в противном случае
     */
    fun isHardwareScannerAvailable(): Boolean {
        // Используем кэшированный результат, если он есть
        cachedResult?.let { return it }

        Timber.d("Проверка наличия встроенного сканера")

        val result = isDataWedgeAvailable() ||
                isHoneywell() ||
                isZebraDevice() ||
                isKnownScannerDevice()

        // Кэшируем результат
        cachedResult = result

        Timber.d("Результат проверки наличия встроенного сканера: $result")
        return result
    }

    /**
     * Проверяет наличие DataWedge на устройстве
     */
    private fun isDataWedgeAvailable(): Boolean {
        return try {
            // Проверяем наличие пакета DataWedge
            val packageInfo = context.packageManager.getPackageInfo("com.symbol.datawedge", PackageManager.GET_ACTIVITIES)

            // Дополнительно проверяем, можем ли мы отправить интент DataWedge
            val intent = Intent()
            intent.action = "com.symbol.datawedge.api.ACTION"

            val activities = context.packageManager.queryIntentActivities(intent, 0)

            val available = packageInfo != null && activities.isNotEmpty()
            Timber.d("DataWedge доступен: $available")
            available
        } catch (e: Exception) {
            Timber.d("DataWedge не обнаружен: ${e.message}")
            false
        }
    }

    /**
     * Проверяет, является ли устройство продуктом Honeywell с встроенным сканером
     */
    private fun isHoneywell(): Boolean {
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        val model = Build.MODEL?.lowercase() ?: ""

        val isHoneywellDevice = manufacturer.contains("honeywell") ||
                model.contains("honeywell") ||
                model.startsWith("eda") ||     // EDA серия Honeywell
                model.startsWith("ct") ||      // CT серия Honeywell
                model.startsWith("cn") ||      // CN серия Honeywell
                model.startsWith("cw")         // CW серия Honeywell

        if (isHoneywellDevice) {
            Timber.d("Обнаружено устройство Honeywell: $manufacturer $model")
        }

        return isHoneywellDevice
    }

    /**
     * Проверяет, является ли устройство продуктом Zebra с встроенным сканером
     */
    private fun isZebraDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        val model = Build.MODEL?.lowercase() ?: ""

        val isZebraDevice = manufacturer.contains("zebra") ||
                manufacturer.contains("motorola solutions") ||
                manufacturer.contains("symbol") ||
                model.startsWith("mc") ||    // MC серия Zebra
                model.startsWith("tc") ||    // TC серия Zebra
                model.startsWith("ec") ||    // EC серия Zebra
                model.startsWith("wt")       // WT серия Zebra

        if (isZebraDevice) {
            Timber.d("Обнаружено устройство Zebra: $manufacturer $model")
        }

        return isZebraDevice
    }

    /**
     * Проверяет, является ли устройство известным устройством со встроенным сканером
     */
    private fun isKnownScannerDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        val model = Build.MODEL?.lowercase() ?: ""

        // Список известных производителей устройств со сканерами
        val knownManufacturers = listOf(
            "datalogic",      // Datalogic
            "newland",        // Newland
            "bluebird",       // Bluebird
            "unitech",        // Unitech
            "casio",          // Casio
            "urovo",          // Urovo
            "chainway",       // Chainway
            "cipherlab",      // CipherLab
            "panasonic"       // Panasonic
        )

        // Список известных префиксов моделей устройств со сканерами
        val knownModelPrefixes = listOf(
            "rt", "pm", "dt", "ht", "pt", "fx", "sg", "rk", "pd", "gx"
        )

        val isKnownManufacturer = knownManufacturers.any { manufacturer.contains(it) }
        val hasKnownModelPrefix = knownModelPrefixes.any { model.startsWith(it) }

        val isKnownDevice = isKnownManufacturer || hasKnownModelPrefix

        if (isKnownDevice) {
            Timber.d("Обнаружено известное устройство со сканером: $manufacturer $model")
        }

        return isKnownDevice
    }

    /**
     * Сбрасывает кэшированный результат проверки наличия сканера
     */
    fun resetCache() {
        cachedResult = null
    }
}