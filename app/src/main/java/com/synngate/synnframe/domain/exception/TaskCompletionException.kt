package com.synngate.synnframe.domain.exception

/**
 * Исключение для завершения задачи с пользовательским сообщением
 */
class TaskCompletionException(
    message: String,
    val userMessage: String? = null,
    val isSuccess: Boolean = false
) : Exception(message)