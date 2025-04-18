package com.synngate.synnframe.domain.service.validation

import com.synngate.synnframe.domain.entity.taskx.validation.ValidationRule
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationType
import timber.log.Timber

/**
 * Реализация сервиса валидации
 */
class ValidationServiceImpl : ValidationService {

    override fun validate(rule: ValidationRule, value: Any?, context: Map<String, Any>): ValidationResult {
        // Проверяем каждое правило валидации
        for (ruleItem in rule.rules) {
            val result = when (ruleItem.type) {
                ValidationType.FROM_PLAN -> validateFromPlan(value, context)
                ValidationType.NOT_EMPTY -> validateNotEmpty(value)
                ValidationType.MATCHES_REGEX -> validateRegex(value, ruleItem.parameter)
            }

            if (!result) {
                Timber.d("Validation failed: ${ruleItem.errorMessage}")
                return ValidationResult.failure(ruleItem.errorMessage)
            }
        }

        return ValidationResult.success()
    }

    /**
     * Валидация "из плана" - проверяет, что объект находится в списке объектов из плана
     */
    private fun validateFromPlan(value: Any?, context: Map<String, Any>): Boolean {
        // Для проверки "из плана" нужно сравнить объект из значения
        // с объектами из плана в контексте

        // Получаем список объектов из плана
        @Suppress("UNCHECKED_CAST")
        val planObjects = context["planObjects"] as? List<Any> ?: return false

        // Если value равно null, то проверка не прошла
        if (value == null) return false

        // Проверяем, есть ли объект в списке плановых объектов
        // Сравнение через equals, поэтому важно корректно реализовать equals в объектах
        return planObjects.any { planObject ->
            (planObject::class == value::class && planObject == value)
        }
    }

    /**
     * Валидация "не пусто" - проверяет, что объект не null и не пустой
     */
    private fun validateNotEmpty(value: Any?): Boolean {
        return when (value) {
            null -> false
            is String -> value.isNotEmpty()
            is Collection<*> -> value.isNotEmpty()
            is Map<*, *> -> value.isNotEmpty()
            is Array<*> -> value.isNotEmpty()
            else -> true // Для остальных типов считаем, что не null = не пусто
        }
    }

    /**
     * Валидация по регулярному выражению - применима только к строкам
     */
    private fun validateRegex(value: Any?, pattern: String?): Boolean {
        if (value == null || pattern == null) return false

        return when (value) {
            is String -> value.matches(Regex(pattern))
            else -> false // Регулярные выражения применимы только к строкам
        }
    }
}