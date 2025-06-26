package com.synngate.synnframe.data.service

import android.util.Base64
import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.domain.service.ServerQrService
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Реализация сервиса для работы с QR-кодами серверов
 */
class ServerQrServiceImpl : ServerQrService {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val aesKey =
        "SynnGateSecretKey" // В реальном приложении должен быть более безопасный способ хранения ключа
    private val useEncyption = false

    /**
     * Преобразует объект сервера в строку для QR-кода
     */
    override fun serverToQrString(server: Server): String {
        // Создаем модель данных для QR-кода
        val qrData = ServerQrData(
            name = server.name,
            host = server.host,
            port = server.port,
            apiEndpoint = server.apiEndpoint,
            encrypted_data = if (useEncyption)
                encryptCredentials(server.login, server.password, server.name, server.host)
            else
                "${server.login}:${server.password}",
            version = 1
        )

        // Сериализуем в JSON
        return json.encodeToString(qrData)
    }

    /**
     * Преобразует строку из QR-кода в объект сервера
     */
    override fun qrStringToServer(qrString: String): Server? {
        return try {
            // Десериализуем из JSON
            val qrData = json.decodeFromString<ServerQrData>(qrString)

            // Расшифровываем учетные данные
            val credentials = if (useEncyption)
                decryptCredentials(qrData.encrypted_data, qrData.name, qrData.host)
            else
                qrData.encrypted_data
            val loginAndPassword = credentials.split(":")

            if (loginAndPassword.size != 2) {
                return null
            }

            Server(
                id = 0, // ID будет назначен при сохранении в БД
                name = qrData.name,
                host = qrData.host,
                port = qrData.port,
                apiEndpoint = qrData.apiEndpoint,
                login = loginAndPassword[0],
                password = loginAndPassword[1],
                isActive = false // По умолчанию не активный
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Шифрует логин и пароль
     */
    private fun encryptCredentials(
        login: String,
        password: String,
        name: String,
        host: String
    ): String {
        if (useEncyption) {
            val credentials = "$login:$password"
            val derivedKey = deriveKeyFromServerInfo(name, host)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(derivedKey, "AES")

            // Используем первые 16 байт ключа в качестве IV
            val iv = ByteArray(16)
            System.arraycopy(derivedKey, 0, iv, 0, 16)
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encrypted = cipher.doFinal(credentials.toByteArray(StandardCharsets.UTF_8))

            return Base64.encodeToString(encrypted, Base64.NO_WRAP)
        } else
            return "$login:$password"
    }

    /**
     * Расшифровывает логин и пароль
     */
    private fun decryptCredentials(encryptedData: String, name: String, host: String): String {
        if (useEncyption) {
            val derivedKey = deriveKeyFromServerInfo(name, host)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(derivedKey, "AES")

            // Используем первые 16 байт ключа в качестве IV
            val iv = ByteArray(16)
            System.arraycopy(derivedKey, 0, iv, 0, 16)
            val ivSpec = IvParameterSpec(iv)

            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decrypted = cipher.doFinal(Base64.decode(encryptedData, Base64.NO_WRAP))

            return String(decrypted, StandardCharsets.UTF_8)
        } else
            return encryptedData
    }

    /**
     * Генерирует ключ на основе имени и хоста сервера
     */
    private fun deriveKeyFromServerInfo(name: String, host: String): ByteArray {
        val salt = "$name:$host"
        val toHash = aesKey + salt

        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(toHash.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Класс данных для JSON-сериализации
     */
    @Serializable
    private data class ServerQrData(
        val name: String,
        val host: String,
        val port: Int,
        val apiEndpoint: String,
        val encrypted_data: String,
        val version: Int
    )
}