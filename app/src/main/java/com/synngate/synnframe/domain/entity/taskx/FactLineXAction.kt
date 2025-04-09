package com.synngate.synnframe.domain.entity.taskx

import kotlinx.serialization.Serializable

@Serializable
data class FactLineXAction(
    val id: String,
    val name: String,
    val actionType: FactLineXActionType,        // Тип действия
    val order: Int,                           // Порядок выполнения
    val selectionCondition: ObjectSelectionCondition = ObjectSelectionCondition.ANY, // Условие выбора
    val promptText: String,                   // Текст подсказки
    val additionalParams: Map<String, String> = emptyMap() // Дополнительные параметры
)