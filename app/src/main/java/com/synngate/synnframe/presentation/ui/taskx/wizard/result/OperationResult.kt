package com.synngate.synnframe.presentation.ui.taskx.wizard.result

interface OperationResult {

    fun isSuccess(): Boolean

    fun getErrorMessage(): String?
}

abstract class BaseOperationResult(
    private val success: Boolean,
    private val errorMessage: String?
) : OperationResult {
    override fun isSuccess(): Boolean = success
    override fun getErrorMessage(): String? = errorMessage
}

data class ValidationResult<T>(
    private val success: Boolean,
    private val errorMessage: String?,
    private val validatedData: T? = null
) : BaseOperationResult(success, errorMessage) {

    fun getValidatedData(): T? = validatedData

    fun <R> map(transform: (T?) -> R?): ValidationResult<R> {
        return ValidationResult(success, errorMessage, transform(validatedData))
    }

    fun <R> flatMap(transform: (T?) -> ValidationResult<R>): ValidationResult<R> {
        return if (isSuccess() && validatedData != null) {
            transform(validatedData)
        } else {
            ValidationResult(false, errorMessage, null)
        }
    }

    companion object {
        fun <T> success(data: T): ValidationResult<T> = ValidationResult(true, null, data)
        fun <T> error(message: String): ValidationResult<T> = ValidationResult(false, message, null)
    }
}

data class SearchResult<T>(
    private val success: Boolean,
    private val errorMessage: String?,
    private val resultType: ResultType = ResultType.SUCCESS,
    private val foundData: T? = null
) : BaseOperationResult(success, errorMessage) {

    fun getFoundData(): T? = foundData

    fun getResultType(): ResultType = resultType

    fun <R> map(transform: (T?) -> R?): SearchResult<R> {
        return if (isSuccess() && foundData != null) {
            success(transform(foundData)!!)
        } else if (resultType == ResultType.NOT_FOUND) {
            notFound()
        } else {
            SearchResult(false, errorMessage, ResultType.ERROR, null)
        }
    }

    fun <R> flatMap(transform: (T?) -> SearchResult<R>): SearchResult<R> {
        return if (isSuccess() && foundData != null) {
            transform(foundData)
        } else if (resultType == ResultType.NOT_FOUND) {
            notFound()
        } else {
            SearchResult(false, errorMessage, ResultType.ERROR, null)
        }
    }

    enum class ResultType {
        SUCCESS,    // Объект найден успешно
        NOT_FOUND,  // Объект не найден (это не ошибка)
        ERROR       // Произошла ошибка при поиске
    }

    companion object {
        fun <T> success(data: T): SearchResult<T> = SearchResult(true, null, ResultType.SUCCESS, data)
        fun <T> notFound(): SearchResult<T> = SearchResult(true, null, ResultType.NOT_FOUND, null)
        fun <T> error(message: String): SearchResult<T> = SearchResult(false, message, ResultType.ERROR, null)
    }
}

data class CreationResult<T>(
    private val success: Boolean,
    private val errorMessage: String?,
    private val createdData: T? = null
) : BaseOperationResult(success, errorMessage) {

    fun getCreatedData(): T? = createdData

    fun <R> map(transform: (T?) -> R?): CreationResult<R> {
        return CreationResult(success, errorMessage, transform(createdData))
    }

    fun <R> flatMap(transform: (T?) -> CreationResult<R>): CreationResult<R> {
        return if (isSuccess() && createdData != null) {
            transform(createdData)
        } else {
            CreationResult(false, errorMessage, null)
        }
    }

    companion object {
        fun <T> success(data: T): CreationResult<T> = CreationResult(true, null, data)
        fun <T> error(message: String): CreationResult<T> = CreationResult(false, message, null)
    }
}

data class NetworkResult<T>(
    private val success: Boolean,
    private val errorMessage: String?,
    private val responseData: T? = null
) : BaseOperationResult(success, errorMessage) {

    fun getResponseData(): T? = responseData

    fun <R> map(transform: (T?) -> R?): NetworkResult<R> {
        return NetworkResult(success, errorMessage, transform(responseData))
    }

    fun <R> flatMap(transform: (T?) -> NetworkResult<R>): NetworkResult<R> {
        return if (isSuccess() && responseData != null) {
            transform(responseData)
        } else {
            NetworkResult(false, errorMessage, null)
        }
    }

    companion object {
        fun <T> success(data: T): NetworkResult<T> = NetworkResult(true, null, data)
        fun success(): NetworkResult<Unit> = NetworkResult(true, null, Unit)
        fun <T> error(message: String): NetworkResult<T> = NetworkResult(false, message, null)
    }
}

data class StateTransitionResult<T>(
    private val success: Boolean,
    private val newState: T,
    private val errorMessage: String? = null
) : BaseOperationResult(success, errorMessage) {

    fun getNewState(): T = newState

    fun <R> map(transform: (T) -> R): StateTransitionResult<R> {
        return StateTransitionResult(success, transform(newState), errorMessage)
    }

    companion object {
        fun <T> success(state: T): StateTransitionResult<T> = StateTransitionResult(true, state, null)
        fun <T> error(state: T, message: String): StateTransitionResult<T> = StateTransitionResult(false, state, message)
    }
}

data class BarcodeProcessResult<T>(
    private val success: Boolean,
    private val errorMessage: String?,
    private val processingRequired: Boolean = true,
    private val resultData: T? = null
) : BaseOperationResult(success, errorMessage) {

    fun isProcessingRequired(): Boolean = processingRequired

    fun getResultData(): T? = resultData

    fun <R> map(transform: (T?) -> R?): BarcodeProcessResult<R> {
        return BarcodeProcessResult(success, errorMessage, processingRequired, transform(resultData))
    }

    companion object {
        fun <T> success(data: T): BarcodeProcessResult<T> = BarcodeProcessResult(true, null, true, data)
        fun <T> ignored(): BarcodeProcessResult<T> = BarcodeProcessResult(true, null, false, null)
        fun <T> error(message: String): BarcodeProcessResult<T> = BarcodeProcessResult(false, message, true, null)
    }
}