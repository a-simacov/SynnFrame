package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.runtime.Composable
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.di.Disposable
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel

interface ActionStepFactory : Disposable {

    @Composable
    fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    )

    fun getViewModel(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ): BaseStepViewModel<*>?

    fun validateStepResult(step: ActionStep, value: Any?): Boolean = true

    fun clearCache()

    fun releaseViewModel(step: ActionStep, action: PlannedAction)
}

interface AutoCompleteCapableFactory {

    fun getAutoCompleteFieldName(step: ActionStep): String?

    fun isAutoCompleteEnabled(step: ActionStep): Boolean

    fun requiresConfirmation(step: ActionStep, fieldName: String): Boolean
}