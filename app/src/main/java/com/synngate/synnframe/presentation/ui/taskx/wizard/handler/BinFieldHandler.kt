package com.synngate.synnframe.presentation.ui.taskx.wizard.handler

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.CreationResult
import com.synngate.synnframe.presentation.ui.taskx.wizard.result.ValidationResult
import timber.log.Timber

/**
 * Обработчик для полей типа ячейка (BinX)
 *
 * @param validationService Сервис валидации
 * @param isStorage Флаг, определяющий, работаем ли с ячейкой хранения (true) или размещения (false)
 */
class BinFieldHandler(
    validationService: ValidationService,
    private val isStorage: Boolean
) : BaseFieldHandler<BinX>(validationService) {

    override fun getPlannedObject(state: ActionWizardState, step: ActionStepTemplate): BinX? {
        return if (isStorage) {
            state.plannedAction?.storageBin
        } else {
            state.plannedAction?.placementBin
        }
    }

    override fun matchesPlannedObject(barcode: String, plannedObject: BinX): Boolean {
        return plannedObject.code == barcode
    }

    override suspend fun createFromString(value: String): CreationResult<BinX> {
        if (value.isBlank()) {
            return CreationResult.error("Код ячейки не может быть пустым")
        }

        try {
            // В реальном приложении здесь может быть логика получения информации о ячейке из БД или API
            val bin = BinX(code = value, zone = "")
            return CreationResult.success(bin)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при создании ячейки из строки: $value")
            return CreationResult.error("Ошибка при создании ячейки: ${e.message}")
        }
    }

    /**
     * Дополнительная проверка соответствия плану
     */
    override suspend fun validateObject(obj: BinX, state: ActionWizardState, step: ActionStepTemplate): ValidationResult<BinX> {
        // Сначала проверяем с помощью стандартной валидации правил
        val baseValidationResult = super.validateObject(obj, state, step)
        if (!baseValidationResult.isSuccess()) {
            return baseValidationResult
        }

        // Дополнительная проверка: если есть плановый объект, проверяем точное соответствие
        val plannedObject = getPlannedObject(state, step)
        if (plannedObject != null) {
            // Проверяем, совпадает ли код ячейки
            if (obj.code != plannedObject.code) {
                val binType = if (isStorage) "хранения" else "размещения"
                return ValidationResult.error("Ячейка $binType не соответствует плану. Ожидается: ${plannedObject.code}")
            }
        }

        return ValidationResult.success(obj)
    }

    override fun supportsType(obj: Any): Boolean {
        return obj is BinX
    }

    companion object {
        /**
         * Проверяет, соответствует ли поле типу обработчика
         */
        fun isApplicableField(field: FactActionField, isStorage: Boolean): Boolean {
            return (isStorage && field == FactActionField.STORAGE_BIN) ||
                    (!isStorage && field == FactActionField.ALLOCATION_BIN)
        }
    }
}