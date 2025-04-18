package com.synngate.synnframe.data.repository

import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.ActionTemplate
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationRule
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationRuleItem
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationType
import com.synngate.synnframe.domain.repository.ActionTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MockActionTemplateRepository : ActionTemplateRepository {

    private val templatesFlow = MutableStateFlow<Map<String, ActionTemplate>>(createInitialTemplates())

    override fun getActionTemplates(): Flow<List<ActionTemplate>> {
        return templatesFlow.map { it.values.toList() }
    }

    override suspend fun getActionTemplateById(id: String): ActionTemplate? {
        return templatesFlow.value[id]
    }

    override suspend fun addActionTemplate(template: ActionTemplate) {
        val updatedTemplates = templatesFlow.value.toMutableMap()
        updatedTemplates[template.id] = template
        templatesFlow.value = updatedTemplates
    }

    override suspend fun updateActionTemplate(template: ActionTemplate) {
        addActionTemplate(template)
    }

    override suspend fun deleteActionTemplate(id: String) {
        val updatedTemplates = templatesFlow.value.toMutableMap()
        updatedTemplates.remove(id)
        templatesFlow.value = updatedTemplates
    }

    // Создание тестовых шаблонов действий
    private fun createInitialTemplates(): Map<String, ActionTemplate> {
        val templates = mutableMapOf<String, ActionTemplate>()

        // Подготовим правила валидации
        val fromPlanValidationRule = ValidationRule(
            name = "Из плана",
            rules = listOf(
                ValidationRuleItem(
                    type = ValidationType.FROM_PLAN,
                    errorMessage = "Выберите объект из плана"
                ),
                ValidationRuleItem(
                    type = ValidationType.NOT_EMPTY,
                    errorMessage = "Объект не должен быть пустым"
                )
            )
        )

        val notEmptyValidationRule = ValidationRule(
            name = "Заполнена",
            rules = listOf(
                ValidationRuleItem(
                    type = ValidationType.NOT_EMPTY,
                    errorMessage = "Объект не должен быть пустым"
                )
            )
        )

        // Шаблон "Взять определенную паллету из определенной ячейки"
        val takePalletTemplate = ActionTemplate(
            id = "template_take_pallet",
            name = "Взять определенную паллету из определенной ячейки",
            wmsAction = WmsAction.TAKE_FROM,
            storageObjectType = ActionObjectType.PALLET,
            placementObjectType = ActionObjectType.BIN,
            storageSteps = listOf(
                ActionStep(
                    id = "step_select_planned_pallet",
                    order = 1,
                    name = "Выберите паллету из запланированного действия",
                    promptText = "Выберите паллету из запланированного действия",
                    objectType = ActionObjectType.PALLET,
                    validationRules = fromPlanValidationRule,
                    isRequired = true,
                    canSkip = false
                )
            ),
            placementSteps = listOf(
                ActionStep(
                    id = "step_select_planned_bin",
                    order = 2,
                    name = "Выберите ячейку из запланированного действия",
                    promptText = "Выберите ячейку из запланированного действия",
                    objectType = ActionObjectType.BIN,
                    validationRules = fromPlanValidationRule,
                    isRequired = true,
                    canSkip = false
                )
            )
        )

        // Шаблон "Подтвердить наличие товара"
        val confirmProductTemplate = ActionTemplate(
            id = "template_confirm_product",
            name = "Подтвердить наличие товара",
            wmsAction = WmsAction.ASSERT,
            storageObjectType = ActionObjectType.CLASSIFIER_PRODUCT,
            placementObjectType = null,
            storageSteps = listOf(
                ActionStep(
                    id = "step_confirm_product",
                    order = 1,
                    name = "Подтвердите наличие товара",
                    promptText = "Подтвердите наличие товара",
                    objectType = ActionObjectType.CLASSIFIER_PRODUCT,
                    validationRules = fromPlanValidationRule,
                    isRequired = true,
                    canSkip = false
                )
            ),
            placementSteps = emptyList()
        )

        // Шаблон "Положить определенную паллету в ячейку хранения"
        val putPalletTemplate = ActionTemplate(
            id = "template_put_pallet",
            name = "Положить определенную паллету в ячейку хранения",
            wmsAction = WmsAction.PUT_INTO,
            storageObjectType = ActionObjectType.PALLET,
            placementObjectType = ActionObjectType.BIN,
            storageSteps = listOf(
                ActionStep(
                    id = "step_select_planned_pallet",
                    order = 1,
                    name = "Выберите паллету из запланированного действия",
                    promptText = "Выберите паллету из запланированного действия",
                    objectType = ActionObjectType.PALLET,
                    validationRules = fromPlanValidationRule,
                    isRequired = true,
                    canSkip = false
                )
            ),
            placementSteps = listOf(
                ActionStep(
                    id = "step_select_bin",
                    order = 2,
                    name = "Выберите ячейку",
                    promptText = "Выберите ячейку",
                    objectType = ActionObjectType.BIN,
                    validationRules = notEmptyValidationRule,
                    isRequired = true,
                    canSkip = false
                )
            )
        )

        // Добавляем шаблоны в результирующую карту
        templates[takePalletTemplate.id] = takePalletTemplate
        templates[confirmProductTemplate.id] = confirmProductTemplate
        templates[putPalletTemplate.id] = putPalletTemplate

        return templates
    }
}