package com.synngate.synnframe.presentation.ui.wizard.action.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.TaskContextManager
import com.synngate.synnframe.presentation.di.Disposable
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactory
import java.util.Collections
import java.util.WeakHashMap

abstract class BaseActionStepFactory<T: Any>(
    protected val taskContextManager: TaskContextManager?
) : ActionStepFactory {

    private val viewModelCache = Collections.synchronizedMap(
        WeakHashMap<String, BaseStepViewModel<T>>()
    )

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
        val cacheKey = generateCacheKey(step, action)

        return viewModelCache.getOrPut(cacheKey) {
            getStepViewModel(step, action, context, this)
        }
    }

    override fun releaseViewModel(step: ActionStep, action: PlannedAction) {
        val cacheKey = generateCacheKey(step, action)
        val viewModel = viewModelCache.remove(cacheKey)

        if (viewModel != null) {
            if (viewModel is Disposable) {
                viewModel.dispose()
            }
        }
    }

    /**
     * Очищает весь кэш ViewModels
     */
    override fun clearCache() {
        viewModelCache.values.forEach { viewModel ->
            if (viewModel is Disposable) {
                viewModel.dispose()
            }
        }

        viewModelCache.clear()
    }

    override fun dispose() {
        clearCache()
    }

    private fun generateCacheKey(step: ActionStep, action: PlannedAction): String {
        return "${step.id}_${action.id}"
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