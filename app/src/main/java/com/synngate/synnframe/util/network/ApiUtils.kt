package com.synngate.synnframe.util.network

/**
 * Утилитный класс для работы с API
 */
object ApiUtils {
    /**
     * Генерирует строку для Basic Authentication по логину и паролю
     *
     * @param login Логин
     * @param password Пароль
     * @return Строка для заголовка Authorization в формате Base64
     */
    fun getBasicAuth(login: String, password: String): String {
        val credentials = "$login:$password"
        return java.util.Base64.getEncoder().encodeToString(credentials.toByteArray())
    }
}