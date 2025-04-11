package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskXLineFieldType
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import java.time.LocalDateTime

/**
 * Состояние визарда создания строки факта
 */
data class WizardState(
    val taskId: String = "",
    val steps: List<WizardStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val results: Map<String, Any?> = emptyMap(),
    val startedAt: LocalDateTime = LocalDateTime.now(),
    val errors: Map<String, String> = emptyMap(),
    val isInitialized: Boolean = false
) {
    val currentStep: WizardStep?
        get() = if (currentStepIndex < steps.size) steps[currentStepIndex] else null

    val isCompleted: Boolean
        get() = currentStepIndex >= steps.size

    val progress: Float
        get() = if (steps.isEmpty()) 0f else currentStepIndex.toFloat() / steps.size

    val canGoBack: Boolean
        get() = currentStepIndex > 0 && currentStep?.canNavigateBack ?: true

    /**
     * Получает данные для создания строки факта
     */
    fun getFactLineData(): Map<TaskXLineFieldType, Any?> {
        val data = mutableMapOf<TaskXLineFieldType, Any?>()

        // Обрабатываем специальные типы результатов
        results.forEach { (key, value) ->
            when {
                value is TaskProduct ->
                    data[TaskXLineFieldType.STORAGE_PRODUCT] = value
                value is Pallet && key.contains("STORAGE_PALLET") ->
                    data[TaskXLineFieldType.STORAGE_PALLET] = value
                value is Pallet && key.contains("PLACEMENT_PALLET") ->
                    data[TaskXLineFieldType.PLACEMENT_PALLET] = value
                value is BinX ->
                    data[TaskXLineFieldType.PLACEMENT_BIN] = value
                value is WmsAction ->
                    data[TaskXLineFieldType.WMS_ACTION] = value
            }
        }

        return data
    }
}