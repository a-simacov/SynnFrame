package com.synngate.synnframe.util.network

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Класс для категоризации сетевых ошибок
 */
object NetworkErrorClassifier {

    /**
     * Типы сетевых ошибок
     */
    enum class ErrorType {
        /**
         * Временная ошибка, можно повторить запрос
         */
        TRANSIENT,

        /**
         * Ошибка на стороне сервера, можно повторить запрос
         */
        SERVER_ERROR,

        /**
         * Ошибка аутентификации, требуется повторная аутентификация
         */
        AUTHENTICATION_ERROR,

        /**
         * Ошибка запроса, нужно исправить запрос
         */
        CLIENT_ERROR,

        /**
         * Неизвестная ошибка
         */
        UNKNOWN
    }

    /**
     * Классификация исключения по типу ошибки
     */
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

    /**
     * Определяет, можно ли повторить запрос при данной ошибке
     */
    fun isRetryable(e: Throwable): Boolean {
        return when (classify(e)) {
            ErrorType.TRANSIENT, ErrorType.SERVER_ERROR -> true
            ErrorType.AUTHENTICATION_ERROR, ErrorType.CLIENT_ERROR, ErrorType.UNKNOWN -> false
        }
    }

    /**
     * Получение сообщения об ошибке для пользователя
     */
    fun getErrorMessage(e: Throwable): String {
        return when (classify(e)) {
            ErrorType.TRANSIENT -> "Проблема с сетью. Пожалуйста, проверьте подключение к Интернету."
            ErrorType.SERVER_ERROR -> "Ошибка на сервере. Пожалуйста, повторите попытку позже."
            ErrorType.AUTHENTICATION_ERROR -> "Ошибка аутентификации. Пожалуйста, войдите в систему снова."
            ErrorType.CLIENT_ERROR -> "Некорректный запрос. Пожалуйста, обратитесь в службу поддержки."
            ErrorType.UNKNOWN -> "Неизвестная ошибка: ${e.message}"
        }
    }
}