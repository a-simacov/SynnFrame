package com.synngate.synnframe.domain.model.wizard

sealed class StepResult {
    // Обычные данные результата шага
    data class Data(val value: Any) : StepResult()

    // Команда "Назад"
    object Back : StepResult()

    // Команда "Пропустить" с опциональными данными
    data class Skip(val value: Any? = null) : StepResult()

    // Команда "Отмена"
    object Cancel : StepResult()
}