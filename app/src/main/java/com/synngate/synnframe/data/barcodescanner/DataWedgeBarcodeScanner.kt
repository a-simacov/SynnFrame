package com.synngate.synnframe.data.barcodescanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.synngate.synnframe.domain.common.BarcodeScanner
import com.synngate.synnframe.domain.common.BarcodeType
import com.synngate.synnframe.domain.common.ScanResult
import com.synngate.synnframe.domain.common.ScanResultListener
import com.synngate.synnframe.domain.common.ScannerManufacturer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class DataWedgeBarcodeScanner(
    private val context: Context
) : BarcodeScanner, DataWedgeReceiver.ScanListener {
    private var isInitializedState = false
    private var isEnabledState = false
    private var scanListener: ScanResultListener? = null

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

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isInitializedState) {
            return@withContext Result.success(Unit)
        }

        return@withContext try {
            // Создаем профиль DataWedge для нашего приложения
            createDataWedgeProfile()

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
            // Регистрируем слушателя в DataWedgeReceiver
            DataWedgeReceiver.addListener(this@DataWedgeBarcodeScanner)

            // Активируем сканирование через DataWedge API
            enableScanner()

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
            configBundle.putString("CONFIG_MODE", "UPDATE")

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
            val intentParams = Bundle()
            intentParams.putString("intent_output_enabled", "true")
            intentParams.putString("intent_action", "com.symbol.datawedge.api.RESULT_ACTION")
            intentParams.putString("intent_delivery", "2") // 0-запуск activity, 1-сервис, 2-бродкаст

            // ВАЖНО: добавляем информацию о компоненте
            intentParams.putString("intent_component_name", context.packageName + "/.data.barcodescanner.DataWedgeReceiver")
            intentParams.putString("intent_component_package", context.packageName)

            intentConfig.putBundle("PARAM_LIST", intentParams)

            // Настройка списка приложений
            val appConfig = Bundle()
            appConfig.putString("PACKAGE_NAME", context.packageName)
            appConfig.putStringArray("ACTIVITY_LIST", arrayOf("*"))

            // Добавляем конфигурации в профиль
            configBundle.putParcelableArray("APP_LIST", arrayOf(appConfig))
            configBundle.putParcelableArray("PLUGIN_CONFIG", arrayOf(barcodeConfig, intentConfig))

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
}