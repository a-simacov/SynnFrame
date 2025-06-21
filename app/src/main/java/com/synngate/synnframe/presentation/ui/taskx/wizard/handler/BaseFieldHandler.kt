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
        val context = buildValidationContext(state, plannedObject)

        val validationResult = validationService.validate(
            rule = step.validationRules,
            value = obj,
            context = context
        )

        return when (validationResult) {
            is com.synngate.synnframe.domain.service.ValidationResult.Success -> ValidationResult.success(obj)
            is com.synngate.synnframe.domain.service.ValidationResult.Error -> {
                Timber.d("Validation error: ${validationResult.message}")
                ValidationResult.error(validationResult.message)
            }
        }
    }

    private fun buildValidationContext(state: ActionWizardState, plannedObject: T?): Map<String, Any> {
        val context = mutableMapOf<String, Any>()

        // Добавляем taskId для возможной подстановки в API endpoints
        if (state.taskId.isNotEmpty()) {
            context["taskId"] = state.taskId
        }

        // Добавляем планируемые объекты если есть
        if (plannedObject != null) {
            context["planItems"] = listOf(plannedObject)
        }

        return context
    }

    override suspend fun handleBarcode(barcode: String, state: ActionWizardState, step: ActionStepTemplate): SearchResult<T> {
        try {
            val plannedObject = getPlannedObject(state, step)
            if (plannedObject != null && matchesPlannedObject(barcode, plannedObject)) {
                Timber.d("Found planned object by barcode: $barcode")
                return SearchResult.success(plannedObject)
            }

            val creationResult = createFromString(barcode)
            if (!creationResult.isSuccess()) {
                return SearchResult.error(creationResult.getErrorMessage()
                    ?: "Failed to create object from barcode: $barcode")
            }

            val createdObject = creationResult.getCreatedData()
                ?: return SearchResult.error("Failed to create object from barcode: $barcode")

            val validationResult = validateObject(createdObject, state, step)
            if (!validationResult.isSuccess()) {
                return SearchResult.error(validationResult.getErrorMessage()
                    ?: "Object failed validation")
            }

            return SearchResult.success(createdObject)
        } catch (e: Exception) {
            Timber.e(e, "Error processing barcode: $barcode")
            return SearchResult.error("Processing error: ${e.message}")
        }
    }

    protected abstract fun matchesPlannedObject(barcode: String, plannedObject: T): Boolean
}