// Обновление интерфейса AutoCompleteCapableFactory
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
     * @param step Шаг действия
     * @return Название поля или null, если автопереход не поддерживается
     */
    fun getAutoCompleteFieldName(step: ActionStep): String?

    /**
     * Проверяет, включен ли автопереход для данного шага
     * @param step Шаг действия
     * @return true, если автопереход включен, иначе false
     */
    fun isAutoCompleteEnabled(step: ActionStep): Boolean

    /**
     * Проверяет, требует ли поле подтверждения перед автопереходом
     * @param step Шаг действия
     * @param fieldName Название поля
     * @return true, если требуется подтверждение, иначе false
     */
    fun requiresConfirmation(step: ActionStep, fieldName: String): Boolean
}

/**
 * Класс-помощник для работы с автопереходом
 */
object AutoTransitionHelper {
    /**
     * Проверяет, нужен ли автопереход для шага при изменении поля
     * @param factory Фабрика шагов
     * @param step Текущий шаг
     * @param fieldName Название измененного поля
     * @return true, если нужен автопереход, иначе false
     */
    fun shouldAutoTransition(factory: ActionStepFactory?, step: ActionStep, fieldName: String): Boolean {
        return factory is AutoCompleteCapableFactory &&
                factory.isAutoCompleteEnabled(step) &&
                factory.getAutoCompleteFieldName(step) == fieldName
    }

    /**
     * Проверяет, нужно ли подтверждение перед автопереходом
     * @param factory Фабрика шагов
     * @param step Текущий шаг
     * @param fieldName Название поля
     * @return true, если нужно подтверждение, иначе false
     */
    fun requiresConfirmation(factory: ActionStepFactory?, step: ActionStep, fieldName: String): Boolean {
        return (factory as? AutoCompleteCapableFactory)?.requiresConfirmation(step, fieldName) ?: false
    }
}