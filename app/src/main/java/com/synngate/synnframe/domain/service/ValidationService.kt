package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationRule
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationType
import timber.log.Timber

/**
 * Сервис для проверки данных по правилам валидации
 */
class ValidationService {
    /**
     * Проверяет значение по правилам валидации
     * @param rule Правило валидации
     * @param value Проверяемое значение
     * @param context Контекст валидации (дополнительные данные)
     * @return Результат валидации с возможной ошибкой
     */
    fun validate(
        rule: ValidationRule,
        value: Any?,
        context: Map<String, Any> = emptyMap()
    ): ValidationResult {
        Timber.d("Validating value: $value with rule: ${rule.name}")

        // Для каждого элемента правил проверяем условие
        for (ruleItem in rule.rules) {
            when (ruleItem.type) {
                ValidationType.FROM_PLAN -> {
                    // Проверка, что объект из плана
                    val planItems = context["planItems"] as? List<*>
                    if (planItems != null && value != null) {
                        if (!isPlanItem(value, planItems)) {
                            Timber.w("Validation failed: ${ruleItem.errorMessage}")
                            return ValidationResult.Error(ruleItem.errorMessage)
                        }
                    }
                }
                ValidationType.NOT_EMPTY -> {
                    // Проверка, что значение не пустое
                    if (value == null || (value is String && value.isBlank())) {
                        Timber.w("Validation failed: ${ruleItem.errorMessage}")
                        return ValidationResult.Error(ruleItem.errorMessage)
                    }
                }
                ValidationType.MATCHES_REGEX -> {
                    // Проверка на соответствие регулярному выражению
                    if (value is String && ruleItem.parameter != null) {
                        try {
                            val regex = ruleItem.parameter.toRegex()
                            if (!value.matches(regex)) {
                                Timber.w("Validation failed: ${ruleItem.errorMessage}")
                                return ValidationResult.Error(ruleItem.errorMessage)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Invalid regex pattern: ${ruleItem.parameter}")
                            return ValidationResult.Error("Invalid regex pattern: ${e.message}")
                        }
                    }
                }
            }
        }

        // Если все проверки пройдены, возвращаем успех
        Timber.d("Validation succeeded")
        return ValidationResult.Success
    }

    /**
     * Проверяет, находится ли элемент в списке элементов плана
     */
    private fun isPlanItem(item: Any, planItems: List<*>): Boolean {
        return planItems.any { planItem ->
            when {
                // Для объектов BinX (сравниваем по коду)
                item is BinX && planItem is BinX ->
                    item.code == planItem.code

                // Для объектов Pallet (сравниваем по коду)
                item is Pallet && planItem is Pallet ->
                    item.code == planItem.code

                // Для объектов TaskProduct (сравниваем по id продукта)
                item is TaskProduct && planItem is TaskProduct ->
                    item.product.id == planItem.product.id

                // Для объектов Product (сравниваем по id)
                item is Product && planItem is Product ->
                    item.id == planItem.id

                // Для других типов - прямое сравнение
                else -> item == planItem
            }
        }
    }
}

/**
 * Результат валидации
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()

    val isSuccess: Boolean
        get() = this is Success
}