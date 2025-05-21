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

/**
 * Интерфейс для фабрик шагов, поддерживающих автозаполнение и автоматический переход
 */
interface AutoCompleteCapableFactory {

    /**
     * Возвращает имя поля для автозаполнения
     */
    fun getAutoCompleteFieldName(step: ActionStep): String?

    /**
     * Проверяет, включено ли автозаполнение для шага
     */
    fun isAutoCompleteEnabled(step: ActionStep): Boolean

    /**
     * Проверяет, требуется ли подтверждение для автоматического перехода
     */
    fun requiresConfirmation(step: ActionStep, fieldName: String): Boolean

    /**
     * Проверяет, должен ли шаг быть автоматически завершен после автозаполнения
     */
    fun shouldAutoComplete(step: ActionStep): Boolean = true

    /**
     * Возвращает список типов объектов, которые могут быть автоматически заполнены
     */
    fun getSupportedAutoFillTypes(step: ActionStep): List<String> = emptyList()
}