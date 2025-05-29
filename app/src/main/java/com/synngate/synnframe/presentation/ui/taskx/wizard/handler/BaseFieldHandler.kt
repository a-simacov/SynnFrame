package com.synngate.synnframe.presentation.ui.taskx.wizard.handler

import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.SearchResult
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.ValidationResult
import timber.log.Timber

abstract class BaseFieldHandler<T : Any>(
    protected val validationService: ValidationService
) : FieldHandler<T> {

    abstract fun getPlannedObject(state: ActionWizardState, step: ActionStepTemplate): T?

    override suspend fun validateObject(obj: T, state: ActionWizardState, step: ActionStepTemplate): ValidationResult<T> {
        if (step.validationRules == null) {
            return ValidationResult.success(obj)
        }

        val plannedObject = getPlannedObject(state, step)
        val context = if (plannedObject != null) {
            mapOf("planItems" to listOf(plannedObject))
        } else {
            emptyMap()
        }

        val validationResult = validationService.validate(
            rule = step.validationRules,
            value = obj,
            context = context
        )

        return when (validationResult) {
            is com.synngate.synnframe.domain.service.ValidationResult.Success -> ValidationResult.success(obj)
            is com.synngate.synnframe.domain.service.ValidationResult.Error -> {
                Timber.d("Ошибка валидации: ${validationResult.message}")
                ValidationResult.error(validationResult.message)
            }
        }
    }

    override suspend fun handleBarcode(barcode: String, state: ActionWizardState, step: ActionStepTemplate): SearchResult<T> {
        try {
            val plannedObject = getPlannedObject(state, step)
            if (plannedObject != null && matchesPlannedObject(barcode, plannedObject)) {
                Timber.d("Найден плановый объект по штрих-коду: $barcode")
                return SearchResult.success(plannedObject)
            }

            val creationResult = createFromString(barcode)
            if (!creationResult.isSuccess()) {
                return SearchResult.error(creationResult.getErrorMessage()
                    ?: "Не удалось создать объект из штрих-кода: $barcode")
            }

            val createdObject = creationResult.getCreatedData()
                ?: return SearchResult.error("Не удалось создать объект из штрих-кода: $barcode")

            val validationResult = validateObject(createdObject, state, step)
            if (!validationResult.isSuccess()) {
                return SearchResult.error(validationResult.getErrorMessage()
                    ?: "Объект не прошел валидацию")
            }

            return SearchResult.success(createdObject)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обработке штрих-кода: $barcode")
            return SearchResult.error("Ошибка обработки: ${e.message}")
        }
    }

    protected abstract fun matchesPlannedObject(barcode: String, plannedObject: T): Boolean
}