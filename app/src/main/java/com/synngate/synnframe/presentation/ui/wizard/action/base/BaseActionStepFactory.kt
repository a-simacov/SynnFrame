package com.synngate.synnframe.presentation.ui.wizard.action.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactory
import timber.log.Timber

/**
 * Базовый класс фабрики шагов визарда.
 *
 * @param T тип данных, используемый на данном шаге
 */
abstract class BaseActionStepFactory<T> : ActionStepFactory {

    @Composable
    override fun createComponent(step: ActionStep, action: PlannedAction, context: ActionContext) {
        val viewModel = remember(step.id, action.id) {
            getStepViewModel(step, action, context)
        }

        // Получаем состояние из ViewModel
        val state by viewModel.state.collectAsState()

        // Вызываем абстрактный метод для отображения содержимого шага
        StepContent(
            state = state,
            viewModel = viewModel,
            step = step,
            action = action,
            context = context
        )
    }

    /**
     * Создает или возвращает ViewModel для шага.
     * Подклассы должны реализовать этот метод для своей конкретной логики.
     */
    protected abstract fun getStepViewModel(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ): BaseStepViewModel<T>

    /**
     * Отображает содержимое шага на основе состояния ViewModel.
     * Подклассы должны реализовать этот метод для своего UI.
     */
    @Composable
    protected abstract fun StepContent(
        state: StepViewState<T>,
        viewModel: BaseStepViewModel<T>,
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    )

    /**
     * Проверяет результат шага на соответствие типу.
     * По умолчанию делегирует проверку базовой реализации validateData в ViewModel.
     */
    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        // Базовая реализация, которую можно переопределить в подклассах
        if (value == null) {
            Timber.d("Validation failed: null value for step ${step.id}")
            return false
        }

        // Проверка типа будет осуществляться в конкретных реализациях
        return true
    }
}