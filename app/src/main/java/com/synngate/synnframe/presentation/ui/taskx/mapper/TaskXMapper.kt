package com.synngate.synnframe.presentation.ui.taskx.mapper

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXStatus
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.domain.entity.taskx.WmsOperation
import com.synngate.synnframe.domain.entity.taskx.action.ActionTemplate
import com.synngate.synnframe.domain.entity.taskx.action.FactAction
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationRule
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationRuleItem
import com.synngate.synnframe.domain.entity.taskx.validation.ValidationType
import com.synngate.synnframe.presentation.ui.taskx.dto.ActionStepDto
import com.synngate.synnframe.presentation.ui.taskx.dto.ActionTemplateDto
import com.synngate.synnframe.presentation.ui.taskx.dto.BinDto
import com.synngate.synnframe.presentation.ui.taskx.dto.FactActionDto
import com.synngate.synnframe.presentation.ui.taskx.dto.PalletDto
import com.synngate.synnframe.presentation.ui.taskx.dto.PlannedActionDto
import com.synngate.synnframe.presentation.ui.taskx.dto.ProductDto
import com.synngate.synnframe.presentation.ui.taskx.dto.SearchActionFieldTypeDto
import com.synngate.synnframe.presentation.ui.taskx.dto.TaskDto
import com.synngate.synnframe.presentation.ui.taskx.dto.TaskProductDto
import com.synngate.synnframe.presentation.ui.taskx.dto.TaskTypeDto
import com.synngate.synnframe.presentation.ui.taskx.dto.TaskXResponseDto
import com.synngate.synnframe.presentation.ui.taskx.dto.ValidationRuleDto
import com.synngate.synnframe.presentation.ui.taskx.dto.ValidationRuleItemDto
import com.synngate.synnframe.presentation.ui.taskx.entity.ActionStepTemplate
import com.synngate.synnframe.presentation.ui.taskx.entity.SearchActionFieldType
import com.synngate.synnframe.presentation.ui.taskx.enums.ActionCompletionCondition
import com.synngate.synnframe.presentation.ui.taskx.enums.ActionCompletionMethod
import com.synngate.synnframe.presentation.ui.taskx.enums.BufferUsage
import com.synngate.synnframe.presentation.ui.taskx.enums.CompletionOrderType
import com.synngate.synnframe.presentation.ui.taskx.enums.FactActionField
import com.synngate.synnframe.presentation.ui.taskx.enums.RegularActionsExecutionOrder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TaskXMapper {

    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun mapTaskXResponse(response: TaskXResponseDto): TaskX {
        val taskType = mapTaskType(response.taskType)
        val task = mapTask(response.task, taskType)

        // Заполняем actionTemplate в PlannedAction из availableActionsTemplates
        val updatedPlannedActions = task.plannedActions.map { plannedAction ->
            val actionTemplate = taskType.availableActionsTemplates.find {
                it.id == plannedAction.actionTemplateId
            }
            plannedAction.copy(actionTemplate = actionTemplate)
        }

        return task.copy(
            taskType = taskType,
            plannedActions = updatedPlannedActions
        )
    }

    private fun mapTask(dto: TaskDto, taskType: TaskTypeX): TaskX {
        return TaskX(
            id = dto.id,
            barcode = dto.barcode,
            name = dto.name,
            taskType = taskType,
            executorId = dto.executorId,
            status = TaskXStatus.fromString(dto.status),
            createdAt = parseDateTime(dto.createdAt),
            startedAt = dto.startedAt?.let { parseDateTime(it) },
            lastModifiedAt = null, // Не приходит с сервера
            completedAt = null, // Не приходит с сервера
            plannedActions = dto.plannedActions.map { mapPlannedAction(it) },
            factActions = dto.factActions.map { mapFactAction(it) }
        )
    }

    private fun mapTaskType(dto: TaskTypeDto): TaskTypeX {
        return TaskTypeX(
            id = dto.id,
            name = dto.name,
            wmsOperation = WmsOperation.fromString(dto.wmsOperation),
            regularActionsExecutionOrder = dto.regularActionsExecutionOrder?.let {
                RegularActionsExecutionOrder.valueOf(it)
            } ?: RegularActionsExecutionOrder.STRICT,
            searchActionFieldsTypes = dto.searchActionFieldsTypes.map { mapSearchActionFieldType(it) },
            availableActionsTemplates = dto.availableActionsTemplates.map { mapActionTemplate(it) }
        )
    }

    private fun mapPlannedAction(dto: PlannedActionDto): PlannedAction {
        return PlannedAction(
            id = dto.id,
            order = dto.order,
            actionTemplateId = dto.actionTemplateId,
            completionOrderType = CompletionOrderType.valueOf(dto.completionOrderType),
            storageProductClassifier = dto.storageProductClassifier?.let { mapProduct(it) },
            storageProduct = dto.storageProduct?.let { mapTaskProduct(it) },
            storagePallet = dto.storagePallet?.let { mapPallet(it) },
            storageBin = dto.storageBin?.let { mapBin(it) },
            quantity = dto.quantity,
            placementPallet = dto.placementPallet?.let { mapPallet(it) },
            placementBin = dto.placementBin?.let { mapBin(it) },
            manuallyCompleted = dto.manuallyCompleted,
            manuallyCompletedAt = dto.manuallyCompletedAt?.let { parseDateTime(it) }
        )
    }

    private fun mapFactAction(dto: FactActionDto): FactAction {
        return FactAction(
            id = dto.id,
            taskId = dto.taskId,
            plannedActionId = dto.plannedActionId,
            actionTemplateId = dto.actionTemplateId,
            storageProductClassifier = dto.storageProductClassifier?.let { mapProduct(it) },
            storageProduct = dto.storageProduct?.let { mapTaskProduct(it) },
            storagePallet = dto.storagePallet?.let { mapPallet(it) },
            storageBin = dto.storageBin?.let { mapBin(it) },
            wmsAction = WmsAction.fromString(dto.wmsAction),
            quantity = dto.quantity,
            placementPallet = dto.placementPallet?.let { mapPallet(it) },
            placementBin = dto.placementBin?.let { mapBin(it) },
            startedAt = parseDateTime(dto.startedAt),
            completedAt = parseDateTime(dto.completedAt)
        )
    }

    private fun mapActionTemplate(dto: ActionTemplateDto): ActionTemplate {
        return ActionTemplate(
            id = dto.id,
            name = dto.name,
            wmsAction = WmsAction.fromString(dto.wmsAction),
            allowMultipleFactActions = dto.allowMultipleFactActions,
            allowManualActionCompletion = dto.allowManualActionCompletion,
            syncWithServer = dto.syncWithServer,
            actionCompletionMethod = dto.actionCompletionMethod?.let {
                when (it) {
                    "AFTER_LAST_STEP" -> ActionCompletionMethod.AFTER_LAST_STEP
                    "AFTER_CONFIRMATION" -> ActionCompletionMethod.AFTER_CONFIRMATION
                    else -> ActionCompletionMethod.AFTER_LAST_STEP
                }
            } ?: ActionCompletionMethod.AFTER_LAST_STEP,
            actionCompletionCondition = dto.actionCompletionCondition?.let {
                when (it) {
                    "ON_COMPLETION" -> ActionCompletionCondition.ON_COMPLETION
                    "PLAN_ACHIEVED" -> ActionCompletionCondition.PLAN_ACHIEVED
                    else -> ActionCompletionCondition.ON_COMPLETION
                }
            } ?: ActionCompletionCondition.ON_COMPLETION,
            actionSteps = dto.actionSteps.map { mapActionStep(it) }
        )
    }

    private fun mapActionStep(dto: ActionStepDto): ActionStepTemplate {
        return ActionStepTemplate(
            id = dto.id,
            order = dto.order,
            name = dto.name,
            promptText = dto.promptText,
            factActionField = FactActionField.valueOf(dto.factActionField),
            isRequired = dto.isRequired,
            serverSelectionEndpoint = dto.serverSelectionEndpoint,
            inputAdditionalProps = dto.inputAdditionalProps,
            bufferUsage = dto.bufferUsage?.let {
                when (it) {
                    "ALWAYS" -> BufferUsage.ALWAYS
                    "DEFAULT" -> BufferUsage.DEFAULT
                    "NEVER" -> BufferUsage.NEVER
                    "CLEAR" -> BufferUsage.CLEAR
                    else -> BufferUsage.NEVER
                }
            } ?: BufferUsage.NEVER,
            saveToTaskBuffer = dto.saveToTaskBuffer,
            validationRules = dto.validationRules?.let { mapValidationRule(it) },
            autoAdvance = dto.autoAdvance ?: true
        )
    }

    private fun mapSearchActionFieldType(dto: SearchActionFieldTypeDto): SearchActionFieldType {
        return SearchActionFieldType(
            actionField = FactActionField.valueOf(dto.actionField),
            isRemoteSearch = dto.isRemoteSearch,
            endpoint = dto.endpoint,
            saveToTaskBuffer = dto.saveToTaskBuffer
        )
    }

    private fun mapProduct(dto: ProductDto): Product {
        return Product(
            id = dto.id,
            name = dto.name,
            articleNumber = dto.articleNumber ?: "",
        )
    }

    private fun mapTaskProduct(dto: TaskProductDto): TaskProduct {
        return TaskProduct(
            id = dto.id,
            product = mapProduct(dto.product),
            expirationDate = dto.expirationDate?.let { parseDateTime(it) },
            status = ProductStatus.fromString(dto.status)
        )
    }

    private fun mapBin(dto: BinDto): BinX {
        return BinX(
            code = dto.code,
            zone = dto.zone
        )
    }

    private fun mapPallet(dto: PalletDto): Pallet {
        return Pallet(
            code = dto.code,
            isClosed = dto.isClosed
        )
    }

    private fun mapValidationRule(dto: ValidationRuleDto): ValidationRule {
        return ValidationRule(
            name = dto.name,
            rules = dto.rules.map { mapValidationRuleItem(it) }
        )
    }

    private fun mapValidationRuleItem(dto: ValidationRuleItemDto): ValidationRuleItem {
        return ValidationRuleItem(
            type = ValidationType.valueOf(dto.type),
            parameter = dto.parameter,
            errorMessage = dto.errorMessage,
            apiEndpoint = dto.apiEndpoint
        )
    }

    private fun parseDateTime(dateTimeString: String): LocalDateTime {
        return try {
            LocalDateTime.parse(dateTimeString, dateTimeFormatter)
        } catch (e: Exception) {
            LocalDateTime.now() // Fallback
        }
    }
}