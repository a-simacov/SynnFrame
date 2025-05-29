package com.synngate.synnframe.presentation.ui.taskx.wizard.handler

import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.CreationResult
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.SearchResult
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.ValidationResult

interface FieldHandler<T> {

    suspend fun handleBarcode(barcode: String, state: ActionWizardState, step: ActionStepTemplate): SearchResult<T>

    suspend fun validateObject(obj: T, state: ActionWizardState, step: ActionStepTemplate): ValidationResult<T>

    suspend fun createFromString(value: String): CreationResult<T>

    fun supportsType(obj: Any): Boolean
}