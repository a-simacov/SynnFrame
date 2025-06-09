package com.synngate.synnframe.presentation.ui.taskx.entity

import kotlinx.serialization.Serializable

/**
 * Определяет способ отображения булевого параметра в UI
 */
@Serializable
enum class BooleanDisplayType {
    CHECKBOX,            // Стандартный флажок
    SWITCH,              // Переключатель (Switch)
    RADIO_BUTTONS,       // Радио-кнопки Да/Нет
    SEGMENTED_BUTTONS,   // Сегментированные кнопки (похожи на таблицу)
    TOGGLE_BUTTONS,      // Кнопки выбора
    DROPDOWN,            // Выпадающий список с двумя опциями
}

/**
 * Текстовые пары для разных типов булевых значений
 */
@Serializable
data class BooleanLabelPair(
    val trueLabel: String = "Yes",
    val falseLabel: String = "No"
) {
    companion object {
        val DEFAULT = BooleanLabelPair("Yes", "No")
        val ON_OFF = BooleanLabelPair("On", "Off")
        val ENABLED_DISABLED = BooleanLabelPair("Enabled", "Disabled")
        val ACTIVE_INACTIVE = BooleanLabelPair("Active", "Inactive")
        val ALLOW_DENY = BooleanLabelPair("Allow", "Deny")
        val FORWARD_BACKWARD = BooleanLabelPair("Forward", "Backward")
        val ACCEPT_REJECT = BooleanLabelPair("Accept", "Reject")
        val SUCCESS_FAIL = BooleanLabelPair("Success", "Fail")
    }
}