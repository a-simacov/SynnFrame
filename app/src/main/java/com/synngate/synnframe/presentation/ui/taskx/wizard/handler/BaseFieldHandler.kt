package com.synngate.synnframe.presentation.ui.taskx.wizard.handler

import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.SearchResult
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.ValidationResult
import timber.log.Timber

/**
 * Базовый класс для обработчиков полей, реализует общую логику
 *
 * @param T Тип объекта, с которым работает обработчик
 * @param validationService Сервис для валидации объектов
 */
abstract class BaseFieldHandler<T : Any>(
    protected val validationService: ValidationService
) : FieldHandler<T> {

    /**
     * Получает планируемый объект для данного шага
     *
     * @param state Текущее состояние визарда
     * @param step Текущий шаг
     * @return Плановый объект или null, если его нет
     */
    abstract fun getPlannedObject(state: ActionWizardState, step: ActionStepTemplate): T?

    /**
     * Валидирует объект согласно правилам текущего шага
     */
    override suspend fun validateObject(obj: T, state: ActionWizardState, step: ActionStepTemplate): ValidationResult<T> {
        // Если нет правил валидации, считаем объект валидным
        if (step.validationRules == null) {
            return ValidationResult.success(obj)
        }

        val plannedObject = getPlannedObject(state, step)
        val context = if (plannedObject != null) {
            mapOf("planItems" to listOf(plannedObject))
        } else {
            emptyMap()
        }

        // Используем ValidationService для проверки
        val validationResult = validationService.validate(
            rule = step.validationRules,
            value = obj,
            context = context
        )

        return when (validationResult) {
            is com.synngate.synnframe.domain.service.ValidationResult.Success -> ValidationResult.success(obj)
            is com.synngate.synnframe.domain.service.ValidationResult.Error -> {
                Timber.d("Ошибка валидации: ${validationResult.message}")
                ValidationResult.error(validationResult.message)
            }
        }
    }

    /**
     * Общая реализация обработки штрих-кода:
     * 1. Проверяем, совпадает ли с плановым объектом
     * 2. Если нет, создаем новый объект
     * 3. Валидируем результат
     */
    override suspend fun handleBarcode(barcode: String, state: ActionWizardState, step: ActionStepTemplate): SearchResult<T> {
        try {
            // Проверяем, есть ли плановый объект и совпадает ли штрихкод с ним
            val plannedObject = getPlannedObject(state, step)
            if (plannedObject != null && matchesPlannedObject(barcode, plannedObject)) {
                Timber.d("Найден плановый объект по штрих-коду: $barcode")
                return SearchResult.success(plannedObject)
            }

            // Создаем объект из штрихкода
            val creationResult = createFromString(barcode)
            if (!creationResult.isSuccess()) {
                return SearchResult.error(creationResult.getErrorMessage()
                    ?: "Не удалось создать объект из штрих-кода: $barcode")
            }

            val createdObject = creationResult.getCreatedData()
                ?: return SearchResult.error("Не удалось создать объект из штрих-кода: $barcode")

            // Валидируем созданный объект
            val validationResult = validateObject(createdObject, state, step)
            if (!validationResult.isSuccess()) {
                return SearchResult.error(validationResult.getErrorMessage()
                    ?: "Объект не прошел валидацию")
            }

            return SearchResult.success(createdObject)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обработке штрих-кода: $barcode")
            return SearchResult.error("Ошибка обработки: ${e.message}")
        }
    }

    /**
     * Проверяет, соответствует ли штрихкод плановому объекту
     */
    protected abstract fun matchesPlannedObject(barcode: String, plannedObject: T): Boolean
}