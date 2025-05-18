package com.synngate.synnframe.presentation.ui.wizard.action.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.presentation.di.Disposable
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactory
import timber.log.Timber
import java.util.Collections
import java.util.WeakHashMap

/**
 * Базовый абстрактный класс для всех фабрик шагов действий.
 * Обеспечивает кэширование ViewModels и их автоматическое освобождение при сборке мусора.
 */
abstract class BaseActionStepFactory<T> : ActionStepFactory {

    // Используем WeakHashMap для автоматического освобождения ViewModels
    // когда на них больше нет ссылок
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
            Timber.d("Creating new ViewModel for step ${step.id} and action ${action.id}")
            getStepViewModel(step, action, context, this)
        }
    }

    /**
     * Освобождает ViewModel для конкретного шага и действия
     */
    override fun releaseViewModel(step: ActionStep, action: PlannedAction) {
        val cacheKey = generateCacheKey(step, action)
        val viewModel = viewModelCache.remove(cacheKey)

        if (viewModel != null) {
            Timber.d("Removing ViewModel for step ${step.id} and action ${action.id}")

            if (viewModel is Disposable) {
                viewModel.dispose()
            }
        }
    }

    /**
     * Очищает весь кэш ViewModels
     */
    override fun clearCache() {
        // Сначала вызываем dispose() для всех ViewModels, реализующих Disposable
        viewModelCache.values.forEach { viewModel ->
            if (viewModel is Disposable) {
                viewModel.dispose()
            }
        }

        Timber.d("Clearing ViewModel cache of size ${viewModelCache.size}")
        viewModelCache.clear()
    }

    /**
     * Освобождает все ресурсы, используемые фабрикой
     */
    override fun dispose() {
        clearCache()
        Timber.d("Disposing ${this.javaClass.simpleName}")
    }

    /**
     * Генерирует ключ для кэша на основе шага и действия
     */
    private fun generateCacheKey(step: ActionStep, action: PlannedAction): String {
        return "${step.id}_${action.id}"
    }

    /**
     * Создает ViewModel для указанного шага действия
     */
    protected abstract fun getStepViewModel(
        step: ActionStep,
        action: PlannedAction,
        context: ActionContext,
        factory: ActionStepFactory
    ): BaseStepViewModel<T>

    /**
     * Определяет UI-представление для шага действия
     */
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