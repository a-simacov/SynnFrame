package com.synngate.synnframe.domain.service

/**
 * Сервис для получения информации об устройстве
 */
interface DeviceInfoService {
    /**
     * Получение информации об устройстве
     *
     * @return Map<String, String> с данными об устройстве:
     * - deviceIp - IP-адрес устройства в локальной сети
     * - deviceId - уникальный идентификатор устройства
     * - deviceName - имя устройства
     */
    fun getDeviceInfo(): Map<String, String>

    /**
     * Получение идентификатора устройства
     *
     * @return String - уникальный идентификатор устройства
     */
    fun getDeviceId(): String

    /**
     * Получение имени устройства
     *
     * @return String - имя устройства
     */
    fun getDeviceName(): String

    /**
     * Получение IP-адреса устройства в локальной сети
     *
     * @return String - IP-адрес устройства или пустая строка, если IP не найден
     */
    fun getDeviceIp(): String
}