package com.synngate.synnframe.presentation.ui.taskx.dto

import kotlinx.serialization.Serializable

@Serializable
data class TaskXResponseDto(
    val task: TaskDto,
    val taskType: TaskTypeDto
)

@Serializable
data class TaskDto(
    val id: String,
    val taskTypeId: String,
    val executorId: String? = null,
    val name: String,
    val barcode: String,
    val createdAt: String,
    val startedAt: String? = null,
    val status: String,
    val plannedActions: List<PlannedActionDto> = emptyList(),
    val factActions: List<FactActionDto> = emptyList()
)

@Serializable
data class TaskTypeDto(
    val id: String,
    val name: String,
    val wmsOperation: String,
    val regularActionsExecutionOrder: String? = null,
    val searchActionFieldsTypes: List<SearchActionFieldTypeDto> = emptyList(),
    val availableActionsTemplates: List<ActionTemplateDto> = emptyList()
)

@Serializable
data class PlannedActionDto(
    val id: String,
    val order: Int,
    val actionTemplateId: String,
    val completionOrderType: String,
    val manuallyCompleted: Boolean = false,
    val manuallyCompletedAt: String? = null,
    val quantity: Float = 0f,
    val storageProductClassifier: ProductDto? = null,
    val storageProduct: TaskProductDto? = null,
    val storageBin: BinDto? = null,
    val storagePallet: PalletDto? = null,
    val placementBin: BinDto? = null,
    val placementPallet: PalletDto? = null
)

@Serializable
data class FactActionDto(
    val id: String,
    val taskId: String,
    val plannedActionId: String? = null,
    val actionTemplateId: String? = null,
    val storageProductClassifier: ProductDto? = null,
    val storageProduct: TaskProductDto? = null,
    val storageBin: BinDto? = null,
    val storagePallet: PalletDto? = null,
    val quantity: Float = 0f,
    val placementBin: BinDto? = null,
    val placementPallet: PalletDto? = null,
    val wmsAction: String,
    val startedAt: String,
    val completedAt: String
)

@Serializable
data class ActionTemplateDto(
    val id: String,
    val name: String,
    val wmsAction: String,
    val allowMultipleFactActions: Boolean = false,
    val allowManualActionCompletion: Boolean = false,
    val syncWithServer: Boolean = true,
    val actionCompletionMethod: String? = null,
    val actionCompletionCondition: String? = null,
    val actionSteps: List<ActionStepDto> = emptyList()
)

@Serializable
data class ActionStepDto(
    val id: String,
    val order: Int,
    val name: String,
    val promptText: String = "",
    val factActionField: String,
    val isRequired: Boolean = true,
    val serverSelectionEndpoint: String = "",
    val inputAdditionalProps: Boolean = false,
    val bufferUsage: String? = null,
    val saveToTaskBuffer: Boolean = false,
    val validationRules: ValidationRuleDto? = null,
    val autoAdvance: Boolean? = null,
    val commands: List<StepCommandDto> = emptyList(),
    val visibilityCondition: String? = null
)

@Serializable
data class SearchActionFieldTypeDto(
    val actionField: String,
    val isRemoteSearch: Boolean,
    val endpoint: String = "",
    val saveToTaskBuffer: Boolean = false
)

@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    val articleNumber: String? = null,
    val weight: Float = 0.0f
)

@Serializable
data class TaskProductDto(
    val id: String,
    val expirationDate: String? = null,
    val status: String,
    val product: ProductDto
)

@Serializable
data class BinDto(
    val code: String,
    val zone: String
)

@Serializable
data class PalletDto(
    val code: String,
    val isClosed: Boolean = false
)

@Serializable
data class ValidationRuleDto(
    val name: String = "",
    val rules: List<ValidationRuleItemDto> = emptyList()
)

@Serializable
data class ValidationRuleItemDto(
    val type: String,
    val parameter: String? = null,
    val errorMessage: String = "",
    val apiEndpoint: String? = null
)