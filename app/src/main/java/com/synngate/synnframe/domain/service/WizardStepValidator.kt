package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.AccountingModel
import com.synngate.synnframe.domain.entity.taskx.FactLineXAction
import com.synngate.synnframe.domain.entity.taskx.FactLineXActionType
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskXLineFieldType
import com.synngate.synnframe.domain.model.wizard.WizardResultModel
import com.synngate.synnframe.domain.model.wizard.WizardStep

/**
 * Класс для валидации шагов визарда
 */
class WizardStepValidator {
    /**
     * Проверяет корректность данных для текущего шага
     * @return Пара из результата валидации и сообщения об ошибке (если есть)
     */
    fun validateStep(step: WizardStep, results: WizardResultModel): Pair<Boolean, String?> {
        // Проверяем, что валидатор шага существует и возвращает true
        val isValid = step.validator(results)

        if (!isValid) {
            // Определение типа ошибки на основе типа действия
            val errorMessage = when (step.action?.actionType) {
                FactLineXActionType.SELECT_PRODUCT -> "Не выбран товар"
                FactLineXActionType.ENTER_QUANTITY -> "Некорректное количество"
                FactLineXActionType.SELECT_BIN -> "Не выбрана ячейка"
                FactLineXActionType.SELECT_PALLET -> "Не выбрана паллета"
                FactLineXActionType.CREATE_PALLET -> "Ошибка создания паллеты"
                FactLineXActionType.CLOSE_PALLET -> "Ошибка закрытия паллеты"
                FactLineXActionType.PRINT_LABEL -> "Ошибка печати этикетки"
                FactLineXActionType.ENTER_EXPIRATION_DATE -> "Некорректная дата срока годности"
                FactLineXActionType.SELECT_PRODUCT_STATUS -> "Не выбран статус товара"
                else -> "Проверка не пройдена"
            }

            return Pair(false, errorMessage)
        }

        return Pair(true, null)
    }

    /**
     * Проверяет, должен ли быть показан шаг на основе текущих результатов
     */
    fun shouldShowStep(step: WizardStep, results: WizardResultModel, taskType: TaskTypeX?): Boolean {
        // Условия для пропуска шагов по типу действия
        val actionType = step.action?.actionType ?: return true

        return when (actionType) {
            FactLineXActionType.ENTER_EXPIRATION_DATE -> {
                // Показываем шаг ввода срока годности только если товар учитывается по срокам
                val product = results.storageProduct?.product
                product?.accountingModel == AccountingModel.BATCH
            }
            FactLineXActionType.CLOSE_PALLET -> {
                // Показываем шаг закрытия паллеты только если паллета не закрыта
                val groupId = getGroupIdForAction(step.action, taskType)
                val group = groupId?.let {
                    taskType?.factLineActionGroups?.find { it.id == groupId }
                }
                val targetFieldType = group?.targetFieldType

                val pallet = when (targetFieldType) {
                    TaskXLineFieldType.STORAGE_PALLET -> results.storagePallet
                    TaskXLineFieldType.PLACEMENT_PALLET -> results.placementPallet
                    else -> null
                }

                pallet != null && !pallet.isClosed
            }
            else -> true // По умолчанию показываем все шаги
        }
    }

    /**
     * Находит ID группы для действия
     */
    private fun getGroupIdForAction(action: FactLineXAction, taskType: TaskTypeX?): String? {
        taskType?.factLineActionGroups?.forEach { group ->
            if (group.actions.any { it.id == action.id }) {
                return group.id
            }
        }
        return null
    }
}