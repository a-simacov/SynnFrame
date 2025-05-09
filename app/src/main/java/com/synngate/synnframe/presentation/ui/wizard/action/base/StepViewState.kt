package com.synngate.synnframe.presentation.ui.wizard.action.base

/**
 * Базовая модель состояния для шагов визарда.
 * @param T тип данных, используемый на данном шаге
 * @property data основные данные состояния
 * @property isLoading флаг, указывающий на процесс загрузки
 * @property error сообщение об ошибке, null, если ошибок нет
 * @property additionalData дополнительные данные, которые могут быть полезны для шага
 */
data class StepViewState<T>(
    val data: T? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val additionalData: Map<String, Any> = emptyMap()
)