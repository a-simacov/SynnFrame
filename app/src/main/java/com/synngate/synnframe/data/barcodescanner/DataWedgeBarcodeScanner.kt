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

    companion object {
        const val ACTION_DATAWEDGE = "com.symbol.datawedge.api.ACTION"
        const val EXTRA_CREATE_PROFILE = "com.symbol.datawedge.api.CREATE_PROFILE"
        const val EXTRA_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG"
        const val EXTRA_SOFT_SCAN_TRIGGER = "com.symbol.datawedge.api.SOFT_SCAN_TRIGGER"

        const val PROFILE_NAME = "SynnFrameProfile"
    }

    private fun disableAutoScan() {
        try {
            val disableAutoIntent = Intent("com.symbol.datawedge.api.ACTION")
            disableAutoIntent.putExtra("com.symbol.datawedge.api.SCANNER_INPUT_PLUGIN", "DISABLE_PLUGIN")
            context.sendBroadcast(disableAutoIntent)

            val disableTriggerIntent = Intent("com.symbol.datawedge.api.ACTION")
            disableTriggerIntent.putExtra("com.symbol.datawedge.api.SOFT_SCAN_TRIGGER", "DISABLE_HARDWARE")
            context.sendBroadcast(disableTriggerIntent)
        } catch (e: Exception) {
            Timber.e(e, "Error disabling auto scan: ${e.message}")
        }
    }

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isInitializedState) {
            return@withContext Result.success(Unit)
        }

        return@withContext try {
            createDataWedgeProfile()
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
            disableScanner()

            if (dataWedgeReceiver != null && isReceiverRegistered) {
                try {
                    context.unregisterReceiver(dataWedgeReceiver)
                    isReceiverRegistered = false
                } catch (e: IllegalArgumentException) {
                    Timber.w("DataWedge receiver was not registered or already unregistered")
                    isReceiverRegistered = false
                } catch (e: Exception) {
                    Timber.e(e, "Error unregistering DataWedge receiver")
                    isReceiverRegistered = false
                }
            }

            dataWedgeReceiver = null

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

    override fun onScan(barcode: String, labelType: String) {
        scanListener?.onScanSuccess(
            ScanResult(
                barcode = barcode,
                type = convertLabelType(labelType)
            )
        )
    }

    private fun createDataWedgeProfile() {
        try {
            val profileIntent = Intent(ACTION_DATAWEDGE)
            profileIntent.putExtra(EXTRA_CREATE_PROFILE, PROFILE_NAME)
            context.sendBroadcast(profileIntent)

            configureProfile()
        } catch (e: Exception) {
            Timber.e(e, "Error creating DataWedge profile: ${e.message}")
            throw e
        }
    }

    private fun configureProfile() {
        try {
            val configBundle = Bundle()


            configBundle.putString("PROFILE_NAME", PROFILE_NAME)
            configBundle.putString("PROFILE_ENABLED", "true")
            configBundle.putString("CONFIG_MODE", "UPDATE") // "CREATE_IF_NOT_EXIST"

            val barcodeConfig = Bundle()
            barcodeConfig.putString("PLUGIN_NAME", "BARCODE")
            barcodeConfig.putString("RESET_CONFIG", "true")

            val scannerParams = Bundle()
            scannerParams.putString("scanner_selection", "auto")
            scannerParams.putString("scanner_input_enabled", "true")

            scannerParams.putString("decoder_ean13", "true")
            scannerParams.putString("decoder_ean8", "true")
            scannerParams.putString("decoder_code39", "true")
            scannerParams.putString("decoder_code128", "true")
            scannerParams.putString("decoder_qrcode", "true")
            scannerParams.putString("decoder_datamatrix", "true")
            scannerParams.putString("decoder_pdf417", "true")

            barcodeConfig.putBundle("PARAM_LIST", scannerParams)

            val intentConfig = Bundle()
            intentConfig.putString("PLUGIN_NAME", "INTENT")
            intentConfig.putString("RESET_CONFIG", "true")

            val intentProps = Bundle()
            intentProps.putString("intent_output_enabled", "true")
            intentProps.putString("intent_action", "com.symbol.datawedge.api.RESULT_ACTION")
            intentProps.putString("intent_category", "android.intent.category.DEFAULT")
            intentProps.putString("intent_delivery", "2") // 2 = Broadcast intent

            intentProps.putString("intent_component_name", "com.synngate.synnframe")

            val bundleComponentInfo = ArrayList<Bundle>()

            val signature = getSHA1Signature(context) ?: "E22084421EAE1A65EEBCB68D4341FE3C2BB6BEC9D"

            val component0 = Bundle()
            component0.putString("PACKAGE_NAME", context.packageName)
            component0.putString("SIGNATURE", signature)
            bundleComponentInfo.add(component0)

            intentProps.putParcelableArrayList("intent_component_info", bundleComponentInfo)

            intentConfig.putBundle("PARAM_LIST", intentProps)

            val pluginList = ArrayList<Bundle>()
            pluginList.add(intentConfig)
            pluginList.add(barcodeConfig)

            val appConfig = Bundle()
            appConfig.putString("PACKAGE_NAME", context.packageName)
            appConfig.putStringArray("ACTIVITY_LIST", arrayOf("*"))

            configBundle.putParcelableArray("APP_LIST", arrayOf(appConfig))
            configBundle.putParcelableArrayList("PLUGIN_CONFIG", pluginList)

            val configIntent = Intent(ACTION_DATAWEDGE)
            configIntent.putExtra(EXTRA_SET_CONFIG, configBundle)
            context.sendBroadcast(configIntent)
        } catch (e: Exception) {
            Timber.e(e, "Error configuring DataWedge profile: ${e.message}")
            throw e
        }
    }

    private fun disableScanner() {
        val dwIntent = Intent()
        dwIntent.action = ACTION_DATAWEDGE
        dwIntent.putExtra(EXTRA_SOFT_SCAN_TRIGGER, "STOP_SCANNING")
        context.sendBroadcast(dwIntent)
        Timber.d("DataWedge scanner disabled")
    }

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

    fun triggerScan() {
        try {
            val dwIntent = Intent()
            dwIntent.action = ACTION_DATAWEDGE
            dwIntent.putExtra(EXTRA_SOFT_SCAN_TRIGGER, "START_SCANNING")
            context.sendBroadcast(dwIntent)

            // Запускаем отложенную задачу для остановки сканирования через 5 секунд
            Handler(Looper.getMainLooper()).postDelayed({
                val stopIntent = Intent()
                stopIntent.action = ACTION_DATAWEDGE
                stopIntent.putExtra(EXTRA_SOFT_SCAN_TRIGGER, "STOP_SCANNING")
                context.sendBroadcast(stopIntent)
            }, 5000)
        } catch (e: Exception) {
            Timber.e(e, "Error triggering DataWedge scan")
        }
    }
}