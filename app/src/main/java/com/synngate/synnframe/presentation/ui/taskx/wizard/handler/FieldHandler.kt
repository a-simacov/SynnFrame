package com.synngate.synnframe.presentation.ui.taskx.wizard.handler

import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.CreationResult
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.SearchResult
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.ValidationResult

/**
 * Универсальный интерфейс для обработчиков полей в визарде действий.
 * Обеспечивает единообразную обработку разных типов объектов.
 *
 * @param T Тип объекта, с которым работает обработчик
 */
interface FieldHandler<T> {
    /**
     * Обрабатывает штрих-код и пытается найти объект соответствующего типа
     *
     * @param barcode Отсканированный штрих-код
     * @param state Текущее состояние визарда
     * @param step Текущий шаг визарда
     * @return Результат поиска объекта по штрихкоду
     */
    suspend fun handleBarcode(barcode: String, state: ActionWizardState, step: ActionStepTemplate): SearchResult<T>

    /**
     * Валидирует объект на соответствие правилам текущего шага
     *
     * @param obj Объект для валидации
     * @param state Текущее состояние визарда
     * @param step Текущий шаг визарда
     * @return Результат валидации объекта
     */
    suspend fun validateObject(obj: T, state: ActionWizardState, step: ActionStepTemplate): ValidationResult<T>

    /**
     * Создает объект из строкового представления (для случаев ручного ввода)
     *
     * @param value Строковое значение
     * @return Результат создания объекта
     */
    suspend fun createFromString(value: String): CreationResult<T>

    /**
     * Проверяет, поддерживает ли обработчик данный тип объекта
     *
     * @param obj Объект для проверки
     * @return true, если обработчик может работать с этим объектом
     */
    fun supportsType(obj: Any): Boolean
}