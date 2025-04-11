package com.synngate.synnframe.presentation.ui.wizard.component

import androidx.compose.runtime.Composable
import com.synngate.synnframe.domain.entity.taskx.FactLineActionGroup
import com.synngate.synnframe.domain.entity.taskx.FactLineXAction
import com.synngate.synnframe.domain.model.wizard.WizardContext

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
    fun validateStepResult(action: FactLineXAction, results: Map<String, Any?>): Boolean = true
}