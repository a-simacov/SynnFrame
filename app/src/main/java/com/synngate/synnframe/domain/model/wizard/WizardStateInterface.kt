package com.synngate.synnframe.domain.model.wizard

/**
 * Интерфейс, определяющий состояние визарда.
 * Все конкретные состояния должны реализовывать этот интерфейс.
 */
interface WizardState {
    /**
     * Уникальный идентификатор состояния
     */
    val id: String

    /**
     * Возвращает true, если состояние является конечным
     */
    val isTerminal: Boolean

    /**
     * Метод для преобразования текущего состояния в другое состояние
     * @param event Событие, которое вызывает переход
     * @return Новое состояние или null, если переход невозможен
     */
    fun handleEvent(event: WizardEvent): WizardState?
}

/**
 * Базовый класс для событий визарда
 */
sealed class WizardEvent {
    /**
     * Событие перехода к следующему шагу с результатом выполнения текущего шага
     */
    data class Next(val result: Any) : WizardEvent()

    /**
     * Событие возврата к предыдущему шагу
     */
    object Back : WizardEvent()

    /**
     * Событие отмены визарда
     */
    object Cancel : WizardEvent()

    /**
     * Событие завершения визарда
     */
    object Complete : WizardEvent()

    /**
     * Событие обработки штрих-кода
     */
    data class ProcessBarcode(val barcode: String) : WizardEvent()

    /**
     * Событие инициализации визарда
     */
    data class Initialize(val taskId: String, val actionId: String) : WizardEvent()
}