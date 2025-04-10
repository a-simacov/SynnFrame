package com.synngate.synnframe.domain.common

/**
 * Sealed класс для результатов операций
 */
sealed class OperationResult<out T> {
    data class Success<T>(val data: T) : OperationResult<T>()
    data class Error(val exception: Throwable) : OperationResult<Nothing>()

    fun isSuccess(): Boolean = this is Success

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }

    companion object {
        fun <T> success(data: T): OperationResult<T> = Success(data)
        fun error(exception: Throwable): OperationResult<Nothing> = Error(exception)
        fun error(message: String): OperationResult<Nothing> = Error(Exception(message))
    }
}

/**
 * Преобразование Result в OperationResult
 */
fun <T> Result<T>.toOperationResult(): OperationResult<T> = fold(
    onSuccess = { OperationResult.success(it) },
    onFailure = { OperationResult.error(it) }
)

/**
 * Преобразование OperationResult в Result
 */
fun <T> OperationResult<T>.toResult(): Result<T> = when (this) {
    is OperationResult.Success -> Result.success(data)
    is OperationResult.Error -> Result.failure(exception)
}