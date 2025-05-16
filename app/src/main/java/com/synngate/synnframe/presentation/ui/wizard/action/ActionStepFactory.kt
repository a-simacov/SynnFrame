// Обновление интерфейса AutoCompleteCapableFactory
package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.runtime.Composable
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel

interface ActionStepFactory {

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
}

interface AutoCompleteCapableFactory {

    /**
     * Возвращает имя поля, при заполнении которого происходит автопереход
     */
    fun getAutoCompleteFieldName(step: ActionStep): String?

    fun isAutoCompleteEnabled(step: ActionStep): Boolean

    /**
     * Проверяет, требует ли поле подтверждения перед автопереходом
     */
    fun requiresConfirmation(step: ActionStep, fieldName: String): Boolean
}