package com.synngate.synnframe.presentation.service.webserver.util

import com.synngate.synnframe.presentation.service.webserver.dto.ErrorResponse
import com.synngate.synnframe.presentation.service.webserver.dto.SuccessResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

// Успешный ответ со статусом 200 OK
suspend inline fun <reified T> ApplicationCall.respondSuccess(
    data: T,
    statusCode: HttpStatusCode = HttpStatusCode.OK
) {
    this.respond(statusCode, SuccessResponse(data))
}

// Ответ с ошибкой
suspend fun ApplicationCall.respondError(
    message: String,
    code: Int = 500,
    statusCode: HttpStatusCode = HttpStatusCode.InternalServerError
) {
    this.respond(statusCode, ErrorResponse(message, code))
}