package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationRule
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationType
import timber.log.Timber

class ValidationService(
    private val validationApiService: ValidationApiService? = null
) {

    fun validate(
        rule: ValidationRule,
        value: Any?,
        context: Map<String, Any> = emptyMap()
    ): ValidationResult {
        for (ruleItem in rule.rules) {
            when (ruleItem.type) {
                ValidationType.FROM_PLAN -> {
                    val planItems = context["planItems"] as? List<*>
                    if (planItems != null && value != null) {
                        if (!isPlanItem(value, planItems)) {
                            return ValidationResult.Error(ruleItem.errorMessage)
                        }
                    }
                }

                ValidationType.NOT_EMPTY -> {
                    if (value == null || (value is String && value.isBlank())) {
                        return ValidationResult.Error(ruleItem.errorMessage)
                    }
                }

                ValidationType.MATCHES_REGEX -> {
                    if (ruleItem.parameter != null && value != null) {
                        try {
                            // Преобразуем значение в строку для проверки регулярным выражением
                            val stringValue = valueToString(value)
                            val regex = ruleItem.parameter.toRegex()
                            if (!stringValue.matches(regex)) {
                                return ValidationResult.Error(ruleItem.errorMessage)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Invalid regex pattern: ${ruleItem.parameter}")
                            return ValidationResult.Error("Invalid regex pattern: ${e.message}")
                        }
                    }
                }

                ValidationType.API_REQUEST -> {
                    // Для API валидации возвращаем специальный результат
                    return ValidationResult.ApiValidationRequired(ruleItem)
                }
            }
        }

        return ValidationResult.Success
    }

    /**
     * Асинхронная валидация (включая API запросы)
     */
    suspend fun validateAsync(
        rule: ValidationRule,
        value: Any?,
        context: Map<String, Any> = emptyMap()
    ): ValidationResult {
        for (ruleItem in rule.rules) {
            when (ruleItem.type) {
                ValidationType.FROM_PLAN -> {
                    val planItems = context["planItems"] as? List<*>
                    if (planItems != null && value != null) {
                        if (!isPlanItem(value, planItems)) {
                            return ValidationResult.Error(ruleItem.errorMessage)
                        }
                    }
                }

                ValidationType.NOT_EMPTY -> {
                    if (value == null || (value is String && value.isBlank())) {
                        return ValidationResult.Error(ruleItem.errorMessage)
                    }
                }

                ValidationType.MATCHES_REGEX -> {
                    if (ruleItem.parameter != null && value != null) {
                        try {
                            // Преобразуем значение в строку для проверки регулярным выражением
                            val stringValue = valueToString(value)
                            val regex = ruleItem.parameter.toRegex()
                            if (!stringValue.matches(regex)) {
                                return ValidationResult.Error(ruleItem.errorMessage)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Invalid regex pattern: ${ruleItem.parameter}")
                            return ValidationResult.Error("Invalid regex pattern: ${e.message}")
                        }
                    }
                }

                ValidationType.API_REQUEST -> {
                    if (validationApiService != null && ruleItem.apiEndpoint != null) {
                        val valueAsString = valueToString(value)

                        // Используем suspend функцию вместо runBlocking
                        val (isValid, errorMessage) = validationApiService.validate(ruleItem.apiEndpoint, valueAsString, context)

                        if (!isValid) {
                            val finalErrorMessage = errorMessage ?: ruleItem.errorMessage
                            Timber.w("API Validation failed: $finalErrorMessage")
                            return ValidationResult.Error(finalErrorMessage)
                        }
                    } else {
                        Timber.w("API validation failed: validationApiService is null or apiEndpoint is missing")
                        return ValidationResult.Error(
                            ruleItem.errorMessage.ifEmpty { "Api validation settings error" }
                        )
                    }
                }
            }
        }

        return ValidationResult.Success
    }

    private fun valueToString(value: Any?): String {
        return when (value) {
            is String -> value
            is Number -> value.toString()
            is Boolean -> value.toString()
            is BinX -> value.code
            is Pallet -> value.code
            is Product -> value.id
            is TaskProduct -> value.product.id
            else -> value?.toString() ?: ""
        }
    }

    private fun isPlanItem(item: Any, planItems: List<*>): Boolean {
        return planItems.any { planItem ->
            when {
                // Случай TaskProduct с Product из planItems
                // Сравниваем только ID товара, игнорируя срок годности и статус
                item is TaskProduct && planItem is Product ->
                    item.product.id == planItem.id

                // Стандартные случаи сравнения
                item is BinX && planItem is BinX ->
                    item.code == planItem.code

                item is Pallet && planItem is Pallet ->
                    item.code == planItem.code

                item is TaskProduct && planItem is TaskProduct ->
                    item.product.id == planItem.product.id

                item is Product && planItem is Product ->
                    item.id == planItem.id

                else -> item == planItem
            }
        }
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
    data class ApiValidationRequired(val ruleItem: com.synngate.synnframe.domain.entity.taskx.validation.ValidationRuleItem) : ValidationResult()

    val isSuccess: Boolean
        get() = this is Success
}