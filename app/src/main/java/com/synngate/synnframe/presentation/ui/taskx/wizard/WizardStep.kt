package com.synngate.synnframe.presentation.ui.taskx.wizard

import androidx.compose.runtime.Composable

/**
 * Представляет отдельный шаг визарда
 */
data class WizardStep(
    val id: String,
    val title: String,
    val content: @Composable (WizardContext) -> Unit,
    val validator: (Map<String, Any?>) -> Boolean = { true },
    val canNavigateBack: Boolean = true
)

/**
 * Контекст выполнения шага мастера
 */
class WizardContext(
    val results: Map<String, Any?>,
    val onComplete: (Any?) -> Unit,
    val onBack: () -> Unit
)