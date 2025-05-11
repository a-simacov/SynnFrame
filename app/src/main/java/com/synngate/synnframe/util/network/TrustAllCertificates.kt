package com.synngate.synnframe.util.network

import timber.log.Timber
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Утилитный класс для настройки доверия всем SSL сертификатам
 */
object TrustAllCertificates {

    /**
     * Настраивает глобальное доверие всем SSL сертификатам
     * ВНИМАНИЕ: Использовать только для разработки или в контролируемых средах
     */
    fun initialize() {
        try {
            // Создаем TrustManager, который доверяет всем сертификатам
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            })

            // Настраиваем SSL контекст с нашим TrustManager
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())

            // Устанавливаем глобальные настройки SSL
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }

            Timber.d("Установлено глобальное доверие всем SSL сертификатам")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при настройке доверия SSL сертификатам")
        }
    }
}