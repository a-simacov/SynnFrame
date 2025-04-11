package com.synngate.synnframe.domain.model.wizard

import androidx.compose.runtime.Composable
import com.synngate.synnframe.domain.entity.taskx.FactLineXAction

/**
 * Модель шага визарда
 */
data class WizardStep(
    val id: String,
    val title: String,
    val action: FactLineXAction? = null,
    val content: @Composable (WizardContext) -> Unit,
    val validator: (Map<String, Any?>) -> Boolean = { true },
    val canNavigateBack: Boolean = true,
    val isAutoComplete: Boolean = false
)

/**
 * Контекст для компонента шага
 */
data class WizardContext(
    val results: Map<String, Any?>,
    // Обновленные сигнатуры методов
    val onComplete: (Any?) -> Unit,
    val onBack: () -> Unit,
    // Новые методы
    val onSkip: (Any?) -> Unit,
    val onCancel: () -> Unit
)