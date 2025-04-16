package com.synngate.synnframe.presentation.service.webserver

import com.synngate.synnframe.domain.entity.Server
import com.synngate.synnframe.domain.repository.ServerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class WebServerAuthProvider(private val serverRepository: ServerRepository) {

    fun validateCredentials(username: String, password: String): Boolean {
        // Получаем активный сервер из репозитория
        val activeServer = runBlocking {
            try {
                serverRepository.getActiveServer().first()
            } catch (e: Exception) {
                Timber.e(e, "Error getting active server for auth validation")
                null
            }
        }

        if (activeServer == null) {
            Timber.w("No active server found for auth validation")
            // Если нет активного сервера, можно либо отклонить все запросы,
            // либо использовать резервные учетные данные для отладки
            return (username == "admin" && password == "admin") // Для отладки
        }

        return validateWithActiveServer(activeServer, username, password)
    }

    private fun validateWithActiveServer(activeServer: Server, username: String, password: String): Boolean {
        // Проверяем, совпадают ли учетные данные с учетными данными активного сервера
        val loginMatches = username == activeServer.login
        val passwordMatches = password == activeServer.password

        if (!loginMatches || !passwordMatches) {
            Timber.w("Authentication failed: credentials don't match active server")
            return false
        }

        Timber.d("Authentication successful using active server credentials")
        return true
    }
}