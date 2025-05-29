package com.synngate.synnframe.presentation.ui.taskx.wizard.handler

import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
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
    override suspend fun validateObject(obj: T, state: ActionWizardState, step: ActionStepTemplate): Pair<Boolean, String?> {
        // Если нет правил валидации, считаем объект валидным
        if (step.validationRules == null) {
            return Pair(true, null)
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
            is com.synngate.synnframe.domain.service.ValidationResult.Success -> Pair(true, null)
            is com.synngate.synnframe.domain.service.ValidationResult.Error -> {
                Timber.d("Ошибка валидации: ${validationResult.message}")
                Pair(false, validationResult.message)
            }
        }
    }

    /**
     * Общая реализация обработки штрих-кода:
     * 1. Проверяем, совпадает ли с плановым объектом
     * 2. Если нет, создаем новый объект
     * 3. Валидируем результат
     */
    override suspend fun handleBarcode(barcode: String, state: ActionWizardState, step: ActionStepTemplate): Pair<T?, String?> {
        try {
            // Проверяем, есть ли плановый объект и совпадает ли штрихкод с ним
            val plannedObject = getPlannedObject(state, step)
            if (plannedObject != null && matchesPlannedObject(barcode, plannedObject)) {
                Timber.d("Найден плановый объект по штрих-коду: $barcode")
                return Pair(plannedObject, null)
            }

            // Создаем объект из штрихкода
            val (createdObject, createError) = createFromString(barcode)
            if (createdObject == null) {
                return Pair(null, createError ?: "Не удалось создать объект из штрих-кода: $barcode")
            }

            // Валидируем созданный объект
            val (isValid, validationError) = validateObject(createdObject, state, step)
            if (!isValid) {
                return Pair(null, validationError ?: "Объект не прошел валидацию")
            }

            return Pair(createdObject, null)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при обработке штрих-кода: $barcode")
            return Pair(null, "Ошибка обработки: ${e.message}")
        }
    }

    /**
     * Проверяет, соответствует ли штрихкод плановому объекту
     */
    protected abstract fun matchesPlannedObject(barcode: String, plannedObject: T): Boolean
}