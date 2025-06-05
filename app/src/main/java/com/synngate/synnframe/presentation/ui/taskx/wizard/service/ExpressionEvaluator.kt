package com.synngate.synnframe.presentation.ui.taskx.wizard.service

import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import org.mvel2.MVEL
import timber.log.Timber

/**
 * Сервис для оценки выражений условий видимости шагов
 */
class ExpressionEvaluator {
    /**
     * Оценивает условие видимости шага
     *
     * @param condition Строковое выражение условия
     * @param state Текущее состояние визарда
     * @return true, если шаг должен быть виден, false если шаг должен быть скрыт
     */
    fun evaluateVisibilityCondition(condition: String?, state: ActionWizardState): Boolean {
        if (condition.isNullOrBlank()) return true

        try {
            // Подготавливаем контекст с доступными объектами для выражения
            val context = mapOf(
                "factAction" to state.factAction,
                "plannedAction" to state.plannedAction,
                "currentStep" to state.getCurrentStep(),
                "selectedObjects" to state.selectedObjects,
                "currentStepIndex" to state.currentStepIndex,
                "steps" to state.steps,
                "classifierProductInfo" to state.classifierProductInfo
            )

            // Оцениваем выражение с помощью MVEL
            val result = MVEL.eval(condition, context)
            return result as? Boolean ?: true
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при оценке выражения: $condition")
            return true // При ошибке шаг должен быть виден (безопасное поведение)
        }
    }

    /**
     * Находит индекс следующего видимого шага
     *
     * @param state Текущее состояние визарда
     * @param startIndex Индекс, с которого начать поиск
     * @return Индекс следующего видимого шага или null, если нет видимых шагов
     */
    fun findNextVisibleStepIndex(state: ActionWizardState, startIndex: Int = state.currentStepIndex + 1): Int? {
        var nextIndex = startIndex

        while (nextIndex < state.steps.size) {
            val step = state.steps[nextIndex]
            if (evaluateVisibilityCondition(step.visibilityCondition, state)) {
                return nextIndex
            }
            nextIndex++
        }

        return null
    }

    /**
     * Находит индекс предыдущего видимого шага
     *
     * @param state Текущее состояние визарда
     * @param startIndex Индекс, с которого начать поиск
     * @return Индекс предыдущего видимого шага или null, если нет видимых шагов
     */
    fun findPreviousVisibleStepIndex(state: ActionWizardState, startIndex: Int = state.currentStepIndex - 1): Int? {
        var prevIndex = startIndex

        while (prevIndex >= 0) {
            val step = state.steps[prevIndex]
            if (evaluateVisibilityCondition(step.visibilityCondition, state)) {
                return prevIndex
            }
            prevIndex--
        }

        return null
    }

    /**
     * Находит индекс первого видимого шага
     *
     * @param state Текущее состояние визарда
     * @return Индекс первого видимого шага или null, если нет видимых шагов
     */
    fun findFirstVisibleStepIndex(state: ActionWizardState): Int? {
        return findNextVisibleStepIndex(state, 0)
    }
}