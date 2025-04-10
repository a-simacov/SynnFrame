package com.synngate.synnframe.presentation.ui.taskx.wizard

import androidx.compose.runtime.Composable
import java.util.UUID

/**
 * Строитель для создания мастера
 */
class WizardBuilder {
    private val steps = mutableListOf<WizardStep>()

    fun step(title: String, block: StepBuilder.() -> Unit) {
        val stepBuilder = StepBuilder(title)
        stepBuilder.block()
        steps.add(stepBuilder.build())
    }

    fun build(): List<WizardStep> = steps
}

/**
 * Строитель для создания шага мастера
 */
class StepBuilder(private val title: String) {
    private var content: @Composable (WizardContext) -> Unit = {}
    private var validator: (Map<String, Any?>) -> Boolean = { true }
    private var canNavigateBack: Boolean = true
    private var id: String = UUID.randomUUID().toString()

    fun id(id: String) {
        this.id = id
    }

    fun content(block: @Composable (WizardContext) -> Unit) {
        this.content = block
    }

    fun validate(validator: (Map<String, Any?>) -> Boolean) {
        this.validator = validator
    }

    fun canNavigateBack(canNavigateBack: Boolean) {
        this.canNavigateBack = canNavigateBack
    }

    fun build(): WizardStep {
        return WizardStep(
            id = id,
            title = title,
            content = content,
            validator = validator,
            canNavigateBack = canNavigateBack
        )
    }
}

/**
 * Функция для создания мастера с использованием DSL
 */
fun buildWizard(block: WizardBuilder.() -> Unit): List<WizardStep> {
    val builder = WizardBuilder()
    builder.block()
    return builder.build()
}