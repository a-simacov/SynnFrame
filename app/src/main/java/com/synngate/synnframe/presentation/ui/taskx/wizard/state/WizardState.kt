package com.synngate.synnframe.presentation.ui.taskx.wizard.state

/**
 * Перечисление возможных состояний визарда действий
 */
enum class WizardState {
    LOADING,         // Загрузка данных визарда
    STEP,            // Отображение шага визарда
    SUMMARY,         // Отображение сводной информации
    SENDING,         // Отправка данных на сервер
    SUCCESS,         // Успешное завершение
    ERROR,           // Состояние ошибки
    EXIT_DIALOG      // Диалог подтверждения выхода
}