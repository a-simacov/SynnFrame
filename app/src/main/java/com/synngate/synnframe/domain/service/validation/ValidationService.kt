package com.synngate.synnframe.domain.service.validation

import com.synngate.synnframe.domain.entity.taskx.validation.ValidationRule

/**
 * Сервис для валидации объектов по правилам
 */
interface ValidationService {
    /**
     * Проверяет объект по правилам валидации
     * @param rule Правило валидации
     * @param value Проверяемый объект
     * @param context Контекст валидации (например, данные о задании)
     * @return Результат валидации с сообщением об ошибке в случае неудачи
     */
    fun validate(rule: ValidationRule, value: Any?, context: Map<String, Any>): ValidationResult
}

/**
 * Результат валидации
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
) {
    companion object {
        fun success() = ValidationResult(true)
        fun failure(errorMessage: String) = ValidationResult(false, errorMessage)
    }
}