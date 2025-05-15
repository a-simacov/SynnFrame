// Заменяет com.synngate.synnframe.presentation.ui.wizard.action.base.BaseActionStepFactory
package com.synngate.synnframe.presentation.ui.wizard.action.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactory

abstract class BaseActionStepFactory<T> : ActionStepFactory {

    private val viewModelCache = mutableMapOf<String, BaseStepViewModel<T>>()

    @Composable
    override fun createComponent(step: ActionStep, action: PlannedAction, context: ActionContext) {
        val viewModel = remember(step.id, action.id) {
            getStepViewModel(step, action, context, this)
        }

        val cacheKey = "${step.id}_${action.id}"
        viewModelCache[cacheKey] = viewModel

        val state by viewModel.state.collectAsState()

        StepContent(
            state = state,
            viewModel = viewModel,
            step = step,
            action = action,
            context = context
        )
    }

    override fun getViewModel(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    ): BaseStepViewModel<*>? {
        val cacheKey = "${step.id}_${action.id}"

        return viewModelCache[cacheKey] ?: getStepViewModel(step, action, context, this)
    }

    protected abstract fun getStepViewModel(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext,
        factory: ActionStepFactory
    ): BaseStepViewModel<T>

    @Composable
    protected abstract fun StepContent(
        state: StepViewState<T>,
        viewModel: BaseStepViewModel<T>,
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext
    )

    override fun validateStepResult(step: ActionStep, value: Any?): Boolean {
        return value != null
    }
}