package com.synngate.synnframe.data.service

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.synngate.synnframe.domain.service.DeviceInfoService
import timber.log.Timber
import java.net.NetworkInterface
import java.util.Collections

class DeviceInfoServiceImpl(
    private val context: Context
) : DeviceInfoService {

    override fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "deviceIp" to getDeviceIp(),
            "deviceId" to getDeviceId(),
            "deviceName" to getDeviceName()
        )
    }

    @SuppressLint("HardwareIds")
    override fun getDeviceId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            Timber.e(e, "Error getting device ID")
            Build.getSerial().takeIf { it != Build.UNKNOWN } ?: "unknown"
        }
    }

    override fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL

        return if (model.startsWith(manufacturer)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    override fun getDeviceIp(): String {
        val wifiIp = getWifiIpAddress()
        if (wifiIp.isNotEmpty()) return wifiIp

        return getIpFromNetworkInterfaces()
    }

    @SuppressLint("WifiManagerPotentialLeak")
    private fun getWifiIpAddress(): String {
        try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipAddress = wifiInfo.ipAddress

            return String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
        } catch (e: Exception) {
            Timber.e(e, "Error getting WiFi IP address")
            return ""
        }
    }

    private fun getIpFromNetworkInterfaces(): String {
        try {
            val networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in networkInterfaces) {
                // Ищем не локальные интерфейсы (не петля обратной связи)
                if (!networkInterface.isLoopback) {
                    val addresses = Collections.list(networkInterface.inetAddresses)
                    for (address in addresses) {
                        if (!address.isLoopbackAddress && address.hostAddress.indexOf(':') < 0) {
                            return address.hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting IP from network interfaces")
        }
        return "127.0.0.1"
    }
}