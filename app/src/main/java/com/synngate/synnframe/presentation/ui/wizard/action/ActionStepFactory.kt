package com.synngate.synnframe.presentation.ui.wizard.action

import androidx.compose.runtime.Composable
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext

/**
 * Интерфейс фабрики компонентов шага действия
 */
interface ActionStepFactory {
    /**
     * Создает компонент шага
     * @param step Шаг действия
     * @param action Запланированное действие
     * @param context Контекст шага
     */
    @Composable
    fun createComponent(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    )

    /**
     * Проверяет результат шага
     * @param step Шаг действия
     * @param value Значение результата
     */
    fun validateStepResult(step: ActionStep, value: Any?): Boolean = true
}

/**
 * Интерфейс для фабрик шагов, поддерживающих автоматический переход
 */
interface AutoCompleteCapableFactory {
    /**
     * Возвращает имя поля, при заполнении которого происходит автопереход
     */
    fun getAutoCompleteFieldName(step: ActionStep): String?

    /**
     * Проверяет, включен ли автопереход для данного шага
     */
    fun isAutoCompleteEnabled(step: ActionStep): Boolean

    /**
     * Проверяет, требует ли поле подтверждения перед автопереходом
     */
    fun requiresConfirmation(step: ActionStep, fieldName: String): Boolean
}