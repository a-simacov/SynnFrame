package com.synngate.synnframe.presentation.ui.taskx.entity

import kotlinx.serialization.Serializable

/**
 * Определяет способ отображения параметров команды
 */
@Serializable
enum class ParametersDisplayMode {
    DIALOG,    // Параметры отображаются в диалоге (по умолчанию)
    INLINE,    // Параметры отображаются непосредственно в шаге
    EXPANDABLE // Параметры отображаются в раскрывающейся панели под командой
}