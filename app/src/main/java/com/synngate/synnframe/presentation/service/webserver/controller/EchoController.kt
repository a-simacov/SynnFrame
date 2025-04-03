package com.synngate.synnframe.presentation.service.webserver.controller

import com.synngate.synnframe.domain.repository.UserRepository
import com.synngate.synnframe.domain.service.LoggingService
import com.synngate.synnframe.presentation.service.webserver.util.respondError
import com.synngate.synnframe.presentation.service.webserver.util.respondSuccess
import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import timber.log.Timber

class EchoController(
    override val logger: LoggingService,
    private val userRepository: UserRepository
) : WebServerController {

    @Serializable
    data class EchoData(
        val serverVersion: String,
        val serverTime: Long = System.currentTimeMillis(),
        val currentUser: String? = null
    )

    suspend fun handleEcho(call: ApplicationCall) {
        try {
            val currentUserId = try {
                userRepository.getCurrentUser().first()?.id
            } catch (e: Exception) {
                Timber.w(e, "Failed to get current user")
                null
            }

            val response = EchoData(
                serverVersion = "1.0.0",
                serverTime = System.currentTimeMillis(),
                currentUser = currentUserId
            )

            call.respondSuccess(response)
        } catch (e: Exception) {
            handleError(call, e, "echo endpoint")
            call.respondError("Failed to process echo request: ${e.message}")
        }
    }
}