package com.synngate.synnframe.domain.model.wizard

import androidx.compose.runtime.Composable

/**
 * Класс, представляющий шаг визарда действий
 */
data class WizardStep(
    val id: String,
    val title: String,
    val content: @Composable (ActionContext) -> Unit,
    val canNavigateBack: Boolean = true,
    val isAutoComplete: Boolean = false,
    val shouldShow: (Map<String, Any>) -> Boolean = { true }
)