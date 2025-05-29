package com.synngate.synnframe.presentation.ui.taskx.wizard.handler

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.wizard.model.ActionWizardState
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

    override suspend fun createFromString(value: String): Pair<BinX?, String?> {
        if (value.isBlank()) {
            return Pair(null, "Код ячейки не может быть пустым")
        }

        try {
            // В реальном приложении здесь может быть логика получения информации о ячейке из БД или API
            val bin = BinX(code = value, zone = "")
            return Pair(bin, null)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при создании ячейки из строки: $value")
            return Pair(null, "Ошибка при создании ячейки: ${e.message}")
        }
    }

    /**
     * ИСПРАВЛЕНИЕ: Добавлена дополнительная проверка соответствия плану
     */
    override suspend fun validateObject(obj: BinX, state: ActionWizardState, step: ActionStepTemplate): Pair<Boolean, String?> {
        // Сначала проверяем с помощью стандартной валидации правил
        val (baseValidationResult, baseErrorMessage) = super.validateObject(obj, state, step)
        if (!baseValidationResult) {
            return Pair(false, baseErrorMessage)
        }

        // Дополнительная проверка: если есть плановый объект, проверяем точное соответствие
        val plannedObject = getPlannedObject(state, step)
        if (plannedObject != null) {
            // Проверяем, совпадает ли код ячейки
            if (obj.code != plannedObject.code) {
                val binType = if (isStorage) "хранения" else "размещения"
                return Pair(false, "Ячейка $binType не соответствует плану. Ожидается: ${plannedObject.code}")
            }
        }

        return Pair(true, null)
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