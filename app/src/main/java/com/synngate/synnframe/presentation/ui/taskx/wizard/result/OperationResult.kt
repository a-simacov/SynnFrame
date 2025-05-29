// Базовый интерфейс для всех результатов
package com.synngate.synnframe.presentation.ui.taskx.wizard.result

/**
 * Базовый интерфейс для результатов операций
 */
interface OperationResult {
    /** Возвращает признак успешного выполнения операции */
    fun isSuccess(): Boolean

    /** Возвращает сообщение об ошибке (null в случае успеха) */
    fun getErrorMessage(): String?
}

/**
 * Базовая абстрактная реализация интерфейса OperationResult
 */
abstract class BaseOperationResult(
    private val success: Boolean,
    private val errorMessage: String?
) : OperationResult {
    override fun isSuccess(): Boolean = success
    override fun getErrorMessage(): String? = errorMessage
}

/**
 * Результат валидации данных
 */
data class ValidationResult<T>(
    private val success: Boolean,
    private val errorMessage: String?,
    private val validatedData: T? = null
) : BaseOperationResult(success, errorMessage) {

    /**
     * Возвращает проверенный объект
     */
    fun getValidatedData(): T? = validatedData

    /**
     * Выполняет преобразование результата
     */
    fun <R> map(transform: (T?) -> R?): ValidationResult<R> {
        return ValidationResult(success, errorMessage, transform(validatedData))
    }

    /**
     * Выполняет плоское преобразование результата
     */
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

/**
 * Результат поиска данных
 */
data class SearchResult<T>(
    private val success: Boolean,
    private val errorMessage: String?,
    private val resultType: ResultType = ResultType.SUCCESS,
    private val foundData: T? = null
) : BaseOperationResult(success, errorMessage) {

    /**
     * Возвращает найденный объект
     */
    fun getFoundData(): T? = foundData

    /**
     * Возвращает тип результата
     */
    fun getResultType(): ResultType = resultType

    /**
     * Преобразует результат в другой тип
     */
    fun <R> map(transform: (T?) -> R?): SearchResult<R> {
        return if (isSuccess() && foundData != null) {
            success(transform(foundData)!!)
        } else if (resultType == ResultType.NOT_FOUND) {
            notFound()
        } else {
            SearchResult(false, errorMessage, ResultType.ERROR, null)
        }
    }

    /**
     * Выполняет плоское преобразование результата
     */
    fun <R> flatMap(transform: (T?) -> SearchResult<R>): SearchResult<R> {
        return if (isSuccess() && foundData != null) {
            transform(foundData)
        } else if (resultType == ResultType.NOT_FOUND) {
            notFound()
        } else {
            SearchResult(false, errorMessage, ResultType.ERROR, null)
        }
    }

    /**
     * Типы результатов поиска
     */
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

/**
 * Результат создания объекта из строки
 */
data class CreationResult<T>(
    private val success: Boolean,
    private val errorMessage: String?,
    private val createdData: T? = null
) : BaseOperationResult(success, errorMessage) {

    /**
     * Возвращает созданный объект
     */
    fun getCreatedData(): T? = createdData

    /**
     * Преобразует результат в другой тип
     */
    fun <R> map(transform: (T?) -> R?): CreationResult<R> {
        return CreationResult(success, errorMessage, transform(createdData))
    }

    /**
     * Выполняет плоское преобразование результата
     */
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

/**
 * Результат сетевой операции
 */
data class NetworkResult<T>(
    private val success: Boolean,
    private val errorMessage: String?,
    private val responseData: T? = null
) : BaseOperationResult(success, errorMessage) {

    /**
     * Возвращает данные ответа
     */
    fun getResponseData(): T? = responseData

    /**
     * Преобразует результат в другой тип
     */
    fun <R> map(transform: (T?) -> R?): NetworkResult<R> {
        return NetworkResult(success, errorMessage, transform(responseData))
    }

    /**
     * Выполняет плоское преобразование результата
     */
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

/**
 * Результат перехода состояния визарда
 */
data class StateTransitionResult<T>(
    private val success: Boolean,
    private val newState: T,
    private val errorMessage: String? = null
) : BaseOperationResult(success, errorMessage) {

    /**
     * Возвращает новое состояние
     */
    fun getNewState(): T = newState

    /**
     * Преобразует состояние в другой тип
     */
    fun <R> map(transform: (T) -> R): StateTransitionResult<R> {
        return StateTransitionResult(success, transform(newState), errorMessage)
    }

    companion object {
        fun <T> success(state: T): StateTransitionResult<T> = StateTransitionResult(true, state, null)
        fun <T> error(state: T, message: String): StateTransitionResult<T> = StateTransitionResult(false, state, message)
    }
}

/**
 * Результат обработки штрихкода
 */
data class BarcodeProcessResult<T>(
    private val success: Boolean,
    private val errorMessage: String?,
    private val processingRequired: Boolean = true,
    private val resultData: T? = null
) : BaseOperationResult(success, errorMessage) {

    /**
     * Возвращает признак необходимости обработки штрихкода
     */
    fun isProcessingRequired(): Boolean = processingRequired

    /**
     * Возвращает данные результата
     */
    fun getResultData(): T? = resultData

    /**
     * Преобразует результат в другой тип
     */
    fun <R> map(transform: (T?) -> R?): BarcodeProcessResult<R> {
        return BarcodeProcessResult(success, errorMessage, processingRequired, transform(resultData))
    }

    companion object {
        fun <T> success(data: T): BarcodeProcessResult<T> = BarcodeProcessResult(true, null, true, data)
        fun <T> ignored(): BarcodeProcessResult<T> = BarcodeProcessResult(true, null, false, null)
        fun <T> error(message: String): BarcodeProcessResult<T> = BarcodeProcessResult(false, message, true, null)
    }
}