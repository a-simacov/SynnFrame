package com.synngate.synnframe.presentation.ui.wizard.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationRule
import timber.log.Timber

sealed class ValidationResult {

    object Success : ValidationResult()

    data class Error(val message: String) : ValidationResult()
}

interface ValidationService {

    fun validate(
        value: Any?,
        rule: ValidationRule,
        context: Map<String, Any> = emptyMap()
    ): ValidationResult
}

class ValidationServiceImpl : ValidationService {
    override fun validate(
        value: Any?,
        rule: ValidationRule,
        context: Map<String, Any>
    ): ValidationResult {
        if (value == null) {
            return ValidationResult.Error("Значение не может быть пустым")
        }

        try {
            val isValidType = when (value) {
                is Product, is TaskProduct -> validateProduct(value, rule, context)
                is Pallet -> validatePallet(value, rule, context)
                is BinX -> validateBin(value, rule, context)
                is String -> validateString(value, rule, context)
                is Number -> validateNumber(value, rule, context)
                else -> {
                    ValidationResult.Error("Неподдерживаемый тип данных")
                }
            }

            return isValidType
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при валидации данных")
            return ValidationResult.Error("Ошибка валидации: ${e.message}")
        }
    }

    private fun validateProduct(
        value: Any,
        rule: ValidationRule,
        context: Map<String, Any>
    ): ValidationResult {
        val planItems = context["planItems"] as? List<*>
        if (planItems != null && planItems.isNotEmpty()) {
            val inPlan = when (value) {
                is Product -> planItems.any { it is Product && it.id == value.id }
                is TaskProduct -> planItems.any {
                    it is TaskProduct && it.product.id == value.product.id
                }
                else -> false
            }

            if (!inPlan) {
                return ValidationResult.Error("Продукт не соответствует плану")
            }
        }

        // Дополнительные проверки для TaskProduct
        if (value is TaskProduct) {
            if (value.quantity <= 0f) {
                return ValidationResult.Error("Количество должно быть больше нуля")
            }
        }

        return ValidationResult.Success
    }

    private fun validatePallet(
        value: Pallet,
        rule: ValidationRule,
        context: Map<String, Any>
    ): ValidationResult {
        // Проверка по плану
        val planItems = context["planItems"] as? List<*>
        if (planItems != null && planItems.isNotEmpty()) {
            val inPlan = planItems.any { it is Pallet && it.code == value.code }
            if (!inPlan) {
                return ValidationResult.Error("Паллета не соответствует плану")
            }
        }

        return ValidationResult.Success
    }

    private fun validateBin(
        value: BinX,
        rule: ValidationRule,
        context: Map<String, Any>
    ): ValidationResult {
        val planItems = context["planItems"] as? List<*>
        if (planItems != null && planItems.isNotEmpty()) {
            val inPlan = planItems.any { it is BinX && it.code == value.code }
            if (!inPlan) {
                return ValidationResult.Error("Ячейка не соответствует плану")
            }
        }

        return ValidationResult.Success
    }

    private fun validateString(
        value: String,
        rule: ValidationRule,
        context: Map<String, Any>
    ): ValidationResult {
        if (value.isBlank()) {
            return ValidationResult.Error("Строка не может быть пустой")
        }

        // Здесь можно добавить проверку регулярных выражений и другие правила

        return ValidationResult.Success
    }

    private fun validateNumber(
        value: Number,
        rule: ValidationRule,
        context: Map<String, Any>
    ): ValidationResult {
        val floatValue = value.toFloat()

        if (floatValue <= 0) {
            return ValidationResult.Error("Значение должно быть больше нуля")
        }

        // Здесь можно добавить проверку диапазона и другие правила

        return ValidationResult.Success
    }
}