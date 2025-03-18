package com.synngate.synnframe.data.remote.api

/**
 * Результат API запроса
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()

    /**
     * Проверка на успешный результат
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * Получение данных из успешного результата или null, если результат ошибочный
     */
    fun getOrNull(): T? = (this as? Success)?.data

    /**
     * Преобразование результата с помощью функции transform
     */
    fun <R> map(transform: (T) -> R): ApiResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }
}