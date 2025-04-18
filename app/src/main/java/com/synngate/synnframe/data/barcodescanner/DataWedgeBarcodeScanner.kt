package com.synngate.synnframe.data.barcodescanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.synngate.synnframe.domain.common.BarcodeScanner
import com.synngate.synnframe.domain.common.BarcodeType
import com.synngate.synnframe.domain.common.ScanResult
import com.synngate.synnframe.domain.common.ScanResultListener
import com.synngate.synnframe.domain.common.ScannerManufacturer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.security.MessageDigest


class DataWedgeBarcodeScanner(
    private val context: Context
) : BarcodeScanner, DataWedgeReceiver.ScanListener {
    private var isInitializedState = false
    private var isEnabledState = false
    private var scanListener: ScanResultListener? = null

    private var dataWedgeReceiver: BroadcastReceiver? = null
    private var isReceiverRegistered = false

    // Константы для работы с DataWedge
    companion object {
        // Интенты для конфигурации DataWedge
        const val ACTION_DATAWEDGE = "com.symbol.datawedge.api.ACTION"
        const val EXTRA_CREATE_PROFILE = "com.symbol.datawedge.api.CREATE_PROFILE"
        const val EXTRA_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG"
        const val EXTRA_SOFT_SCAN_TRIGGER = "com.symbol.datawedge.api.SOFT_SCAN_TRIGGER"

        // Имя профиля DataWedge для нашего приложения
        const val PROFILE_NAME = "SynnFrameProfile"
    }

    private fun disableAutoScan() {
        try {
            // Отправляем команду на отключение автоматического сканирования
            val disableAutoIntent = Intent("com.symbol.datawedge.api.ACTION")
            disableAutoIntent.putExtra("com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN", "DISABLE_PLUGIN")
            context.sendBroadcast(disableAutoIntent)

            // Для точного контроля можно также отключить триггер
            val disableTriggerIntent = Intent("com.symbol.datawedge.api.ACTION")
            disableTriggerIntent.putExtra("com.symbol.datawedge.api.SOFT_SCAN_TRIGGER", "DISABLE_HARDWARE")
            context.sendBroadcast(disableTriggerIntent)

            Timber.d("DataWedge auto scan disabled")
        } catch (e: Exception) {
            Timber.e(e, "Error disabling auto scan: ${e.message}")
        }
    }

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isInitializedState) {
            return@withContext Result.success(Unit)
        }

        return@withContext try {
            // Создаем профиль DataWedge
            createDataWedgeProfile()

            // Важно! Отключаем автоматическое сканирование
            disableAutoScan()

            isInitializedState = true
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error initializing DataWedge scanner: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun dispose() {
        disable()
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
            // Регистрируем слушателя
            DataWedgeReceiver.addListener(this@DataWedgeBarcodeScanner)

            // Создаем и регистрируем receiver динамически
            if (dataWedgeReceiver == null) {
                dataWedgeReceiver = DataWedgeReceiver()
                val intentFilter = IntentFilter("com.symbol.datawedge.api.RESULT_ACTION")
                intentFilter.addCategory(Intent.CATEGORY_DEFAULT)

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
                        context.registerReceiver(
                            dataWedgeReceiver,
                            intentFilter,
                            Context.RECEIVER_NOT_EXPORTED
                        )
                        isReceiverRegistered = true
                    } else {
                        context.registerReceiver(dataWedgeReceiver, intentFilter)
                        isReceiverRegistered = true
                    }
                    Timber.d("DataWedge receiver registered dynamically")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to register DataWedge receiver")
                    isReceiverRegistered = false
                }
            }

            // Явно включаем сканер только когда это запрошено
            val enableScanIntent = Intent("com.symbol.datawedge.api.ACTION")
            enableScanIntent.putExtra("com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN", "ENABLE_PLUGIN")
            context.sendBroadcast(enableScanIntent)

            isEnabledState = true
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error enabling DataWedge scanner: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun disable() {
        if (!isEnabledState) return

        try {
            // Деактивируем сканирование
            disableScanner()

            if (dataWedgeReceiver != null && isReceiverRegistered) {
                try {
                    context.unregisterReceiver(dataWedgeReceiver)
                    isReceiverRegistered = false
                    Timber.d("DataWedge receiver unregistered")
                } catch (e: IllegalArgumentException) {
                    // Приемник не был зарегистрирован - просто логируем это
                    Timber.w("DataWedge receiver was not registered or already unregistered")
                    isReceiverRegistered = false
                } catch (e: Exception) {
                    // Другие ошибки
                    Timber.e(e, "Error unregistering DataWedge receiver")
                    isReceiverRegistered = false
                }
            }

            dataWedgeReceiver = null

            // Удаляем слушателя
            DataWedgeReceiver.removeListener(this)
        } catch (e: Exception) {
            Timber.e(e, "Error disabling DataWedge scanner: ${e.message}")
        } finally {
            isEnabledState = false
            scanListener = null
        }
    }

    override fun isInitialized(): Boolean = isInitializedState

    override fun isEnabled(): Boolean = isEnabledState

    override fun getManufacturer(): ScannerManufacturer = ScannerManufacturer.ZEBRA

    // Реализация интерфейса ScanListener
    override fun onScan(barcode: String, labelType: String) {
        Timber.d("DataWedgeBarcodeScanner received scan: $barcode, type: $labelType")

        scanListener?.onScanSuccess(
            ScanResult(
                barcode = barcode,
                type = convertLabelType(labelType)
            )
        )
    }

    // Создание профиля DataWedge для приложения
    private fun createDataWedgeProfile() {
        try {
            // 1. Создаем профиль
            val profileIntent = Intent(ACTION_DATAWEDGE)
            profileIntent.putExtra(EXTRA_CREATE_PROFILE, PROFILE_NAME)
            context.sendBroadcast(profileIntent)
            Timber.d("DataWedge create profile intent sent: $PROFILE_NAME")

            // 2. Настраиваем профиль
            configureProfile()

            Timber.d("DataWedge profile created and configured: $PROFILE_NAME")
        } catch (e: Exception) {
            Timber.e(e, "Error creating DataWedge profile: ${e.message}")
            throw e
        }
    }

    // Настройка профиля DataWedge
    private fun configureProfile() {
        try {
            val configBundle = Bundle()

            // Профильные настройки
            configBundle.putString("PROFILE_NAME", PROFILE_NAME)
            configBundle.putString("PROFILE_ENABLED", "true")
            configBundle.putString("CONFIG_MODE", "UPDATE") // "CREATE_IF_NOT_EXIST"

            // Настройка плагина сканера
            val barcodeConfig = Bundle()
            barcodeConfig.putString("PLUGIN_NAME", "BARCODE")
            barcodeConfig.putString("RESET_CONFIG", "true")

            // Параметры сканера
            val scannerParams = Bundle()
            scannerParams.putString("scanner_selection", "auto")
            scannerParams.putString("scanner_input_enabled", "true")
            // Типы штрихкодов
            scannerParams.putString("decoder_ean13", "true")
            scannerParams.putString("decoder_ean8", "true")
            scannerParams.putString("decoder_code39", "true")
            scannerParams.putString("decoder_code128", "true")
            scannerParams.putString("decoder_qrcode", "true")
            scannerParams.putString("decoder_datamatrix", "true")
            scannerParams.putString("decoder_pdf417", "true")

            barcodeConfig.putBundle("PARAM_LIST", scannerParams)

            // Настройка плагина Intent
            val intentConfig = Bundle()
            intentConfig.putString("PLUGIN_NAME", "INTENT")
            intentConfig.putString("RESET_CONFIG", "true")

            // Параметры Intent
            val intentProps = Bundle()
            intentProps.putString("intent_output_enabled", "true")
            intentProps.putString("intent_action", "com.symbol.datawedge.api.RESULT_ACTION")
            intentProps.putString("intent_category", "android.intent.category.DEFAULT")
            intentProps.putString("intent_delivery", "2") // 2 = Broadcast intent
            // Указываем компонент явно
            intentProps.putString("intent_component_name", "com.synngate.synnframe")//context.packageName + "/com.synngate.synnframe.data.barcodescanner.DataWedgeReceiver")

            val bundleComponentInfo = ArrayList<Bundle>()

            val signature = getSHA1Signature(context) ?: "E22084421EAE1A65EEBCB68D4341FE3C2BB6BEC9D" // Default as fallback

            val component0 = Bundle()
            component0.putString("PACKAGE_NAME", context.packageName)
            component0.putString("SIGNATURE", signature)
            bundleComponentInfo.add(component0)

            intentProps.putParcelableArrayList("intent_component_info", bundleComponentInfo)

            // Ставим отдельно параметры intent output

            intentConfig.putBundle("PARAM_LIST", intentProps)

            val pluginList = ArrayList<Bundle>()
            pluginList.add(intentConfig)
            pluginList.add(barcodeConfig)

            // Настройка списка приложений
            val appConfig = Bundle()
            appConfig.putString("PACKAGE_NAME", context.packageName)
            appConfig.putStringArray("ACTIVITY_LIST", arrayOf("*"))

            // Добавляем конфигурации в профиль
            configBundle.putParcelableArray("APP_LIST", arrayOf(appConfig))
            configBundle.putParcelableArrayList("PLUGIN_CONFIG", pluginList)

            // Отправляем конфигурацию
            val configIntent = Intent(ACTION_DATAWEDGE)
            configIntent.putExtra(EXTRA_SET_CONFIG, configBundle)
            context.sendBroadcast(configIntent)

            Timber.d("DataWedge profile configuration sent")
        } catch (e: Exception) {
            Timber.e(e, "Error configuring DataWedge profile: ${e.message}")
            throw e
        }
    }

    // Активация сканера
    private fun enableScanner() {
        val dwIntent = Intent()
        dwIntent.action = ACTION_DATAWEDGE
        dwIntent.putExtra(EXTRA_SOFT_SCAN_TRIGGER, "START_SCANNING")
        context.sendBroadcast(dwIntent)
        Timber.d("DataWedge scanner enabled")
    }

    // Деактивация сканера
    private fun disableScanner() {
        val dwIntent = Intent()
        dwIntent.action = ACTION_DATAWEDGE
        dwIntent.putExtra(EXTRA_SOFT_SCAN_TRIGGER, "STOP_SCANNING")
        context.sendBroadcast(dwIntent)
        Timber.d("DataWedge scanner disabled")
    }

    // Конвертация типов штрихкодов
    private fun convertLabelType(labelType: String): BarcodeType {
        return when (labelType.uppercase()) {
            "EAN13", "EAN-13" -> BarcodeType.EAN13
            "EAN8", "EAN-8" -> BarcodeType.EAN8
            "CODE39", "CODE-39" -> BarcodeType.CODE39
            "CODE128", "CODE-128" -> BarcodeType.CODE128
            "QRCODE", "QR-CODE" -> BarcodeType.QR
            "DATAMATRIX", "DATA-MATRIX" -> BarcodeType.DATAMATRIX
            "PDF417", "PDF-417" -> BarcodeType.PDF417
            else -> BarcodeType.UNKNOWN
        }
    }

    private fun getSHA1Signature(context: Context): String? {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            if (packageInfo.signingInfo != null) {
                val signatures = if (packageInfo.signingInfo!!.hasMultipleSigners()) {
                    packageInfo.signingInfo!!.apkContentsSigners
                } else {
                    packageInfo.signingInfo!!.signingCertificateHistory
                }

                if (signatures.isNotEmpty()) {
                    val cert = signatures[0].toByteArray()
                    val md = MessageDigest.getInstance("SHA1")
                    val digest = md.digest(cert)
                    val sb = StringBuilder()
                    for (b in digest) {
                        sb.append(String.format("%02X", b))
                    }
                    return sb.toString()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting app SHA1 signature")
        }
        return null
    }

    // Метод для программного запуска сканирования
    fun triggerScan() {
        try {
            val dwIntent = Intent()
            dwIntent.action = ACTION_DATAWEDGE
            dwIntent.putExtra(EXTRA_SOFT_SCAN_TRIGGER, "START_SCANNING")
            context.sendBroadcast(dwIntent)
            Timber.d("DataWedge scan triggered programmatically")

            // Запускаем отложенную задачу для остановки сканирования через 5 секунд
            Handler(Looper.getMainLooper()).postDelayed({
                val stopIntent = Intent()
                stopIntent.action = ACTION_DATAWEDGE
                stopIntent.putExtra(EXTRA_SOFT_SCAN_TRIGGER, "STOP_SCANNING")
                context.sendBroadcast(stopIntent)
                Timber.d("DataWedge auto-stop scan after timeout")
            }, 5000)
        } catch (e: Exception) {
            Timber.e(e, "Error triggering DataWedge scan")
        }
    }
}