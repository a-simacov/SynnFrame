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
    val trueLabel: String = "Да",
    val falseLabel: String = "Нет"
) {
    companion object {
        val DEFAULT = BooleanLabelPair("Да", "Нет")
        val ON_OFF = BooleanLabelPair("Вкл", "Выкл")
        val ENABLED_DISABLED = BooleanLabelPair("Включено", "Выключено")
        val ACTIVE_INACTIVE = BooleanLabelPair("Активно", "Неактивно")
        val ALLOW_DENY = BooleanLabelPair("Разрешить", "Запретить")
        val FORWARD_BACKWARD = BooleanLabelPair("Вперед", "Назад")
        val ACCEPT_REJECT = BooleanLabelPair("Принять", "Отклонить")
        val SUCCESS_FAIL = BooleanLabelPair("Успешно", "Неуспешно")
    }
}