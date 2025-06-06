package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.Server

/**
 * Интерфейс сервиса для работы с QR-кодами серверов
 */
interface ServerQrService {

    /**
     * Преобразует объект сервера в строку для QR-кода
     * @param server Сервер для кодирования
     * @return Строка, представляющая данные сервера, которую можно закодировать в QR-код
     */
    fun serverToQrString(server: Server): String

    /**
     * Преобразует строку из QR-кода в объект сервера
     * @param qrString Строка, полученная из QR-кода
     * @return Объект сервера или null, если строка не соответствует ожидаемому формату
     */
    fun qrStringToServer(qrString: String): Server?
}