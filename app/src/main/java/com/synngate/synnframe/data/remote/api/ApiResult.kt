package com.synngate.synnframe.data.remote.api

sealed class ApiResult<out T> {

    data class Success<T>(val data: T) : ApiResult<T>()

    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()

    fun isSuccess(): Boolean = this is Success

    fun getOrNull(): T? = (this as? Success)?.data

    fun <R> map(transform: (T) -> R): ApiResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }
}