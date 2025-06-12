package com.synngate.synnframe.util.network

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkErrorClassifier {

    enum class ErrorType {
        TRANSIENT,

        SERVER_ERROR,

        AUTHENTICATION_ERROR,

        CLIENT_ERROR,

        UNKNOWN
    }

    fun classify(e: Throwable): ErrorType {
        return when (e) {
            // Ошибки, связанные с сетью (можно повторить)
            is SocketTimeoutException,
            is ConnectException,
            is UnknownHostException,
            is IOException -> ErrorType.TRANSIENT

            // Ошибки HTTP сервера (5xx)
            is ServerResponseException -> ErrorType.SERVER_ERROR

            // Ошибки HTTP клиента (4xx)
            is ClientRequestException -> {
                when (e.response.status.value) {
                    HttpStatusCode.Unauthorized.value,
                    HttpStatusCode.Forbidden.value -> ErrorType.AUTHENTICATION_ERROR
                    else -> ErrorType.CLIENT_ERROR
                }
            }

            // Ошибки перенаправления (3xx)
            is RedirectResponseException -> ErrorType.CLIENT_ERROR

            // Все остальные ошибки
            else -> ErrorType.UNKNOWN
        }
    }

    fun isRetryable(e: Throwable): Boolean {
        // Добавим специальную проверку на таймаут
        if (e.message?.contains("timeout", ignoreCase = true) == true ||
            e.message?.contains("timed out", ignoreCase = true) == true) {
            return true
        }

        return when (classify(e)) {
            ErrorType.TRANSIENT, ErrorType.SERVER_ERROR -> true
            ErrorType.AUTHENTICATION_ERROR, ErrorType.CLIENT_ERROR, ErrorType.UNKNOWN -> false
        }
    }

    fun getErrorMessage(e: Throwable): String {
        return when (classify(e)) {
            ErrorType.TRANSIENT -> "Network issue. Please check your Internet connection."
            ErrorType.SERVER_ERROR -> "Server error. Please try again later."
            ErrorType.AUTHENTICATION_ERROR -> "Authentication error. Please log in again."
            ErrorType.CLIENT_ERROR -> "Invalid request. Please contact support."
            ErrorType.UNKNOWN -> "Unknown error: ${e.message}"
        }
    }
}