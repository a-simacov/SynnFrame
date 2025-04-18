package com.synngate.synnframe.presentation.ui.wizard.component

import androidx.compose.runtime.Composable
import com.synngate.synnframe.domain.model.wizard.WizardContext
import com.synngate.synnframe.domain.model.wizard.WizardResultModel

/**
 * Интерфейс фабрики компонентов шага визарда
 */
interface StepComponentFactory {
    /**
     * Создает компонент шага
     */
    @Composable
    fun createComponent(
        action: FactLineXAction,
        groupContext: FactLineActionGroup,
        wizardContext: WizardContext
    )

    /**
     * Проверяет результат шага
     */
    fun validateStepResult(action: FactLineXAction, results: WizardResultModel): Boolean = true
}