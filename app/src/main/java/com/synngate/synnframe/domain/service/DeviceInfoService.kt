package com.synngate.synnframe.domain.service

interface DeviceInfoService {

    fun getDeviceInfo(): Map<String, String>

    fun getDeviceId(): String

    fun getDeviceName(): String

    fun getDeviceIp(): String
}