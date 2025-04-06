package com.synngate.synnframe.presentation.service.webserver.controller

import io.ktor.server.application.ApplicationCall
import timber.log.Timber

interface WebServerController {

    suspend fun handleError(call: ApplicationCall, e: Throwable, operation: String) {
        Timber.e(e, "Error in $operation")
//        try {
//            logger.logError("Ошибка в $operation: ${e.message}")
//        } catch (innerE: Exception) {
//            Timber.e(innerE, "Failed to log error")
//        }
    }
}