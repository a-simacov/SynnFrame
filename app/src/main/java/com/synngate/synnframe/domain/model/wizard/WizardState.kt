package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskXLineFieldType
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import timber.log.Timber
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
                // Проверяем тип значения для объектов
                value is TaskProduct -> {
                    data[TaskXLineFieldType.STORAGE_PRODUCT] = value
                    Timber.d("Found product: ${value.product.name}")
                }
                value is Pallet && (key.contains("STORAGE", ignoreCase = true)) -> {
                    data[TaskXLineFieldType.STORAGE_PALLET] = value
                    Timber.d("Found storage pallet: ${value.code}")
                }
                value is Pallet && (key.contains("PLACEMENT", ignoreCase = true)) -> {
                    data[TaskXLineFieldType.PLACEMENT_PALLET] = value
                    Timber.d("Found placement pallet: ${value.code}")
                }
                value is Pallet && !data.containsKey(TaskXLineFieldType.PLACEMENT_PALLET) &&
                        !data.containsKey(TaskXLineFieldType.STORAGE_PALLET) -> {
                    // Если не определили тип паллеты, считаем ее паллетой размещения
                    data[TaskXLineFieldType.PLACEMENT_PALLET] = value
                    Timber.d("Found pallet of unknown type: ${value.code}")
                }
                value is BinX -> {
                    data[TaskXLineFieldType.PLACEMENT_BIN] = value
                    Timber.d("Found bin: ${value.code}")
                }
                value is WmsAction -> {
                    // Если нашли действие WMS, сохраняем его, но не переписываем уже установленное
                    if (!data.containsKey(TaskXLineFieldType.WMS_ACTION)) {
                        data[TaskXLineFieldType.WMS_ACTION] = value
                        Timber.d("Found action WMS: $value")
                    }
                }
            }
        }

        // Проверяем, что у нас есть WMS-действие, если нет - устанавливаем RECEIPT по умолчанию
        if (!data.containsKey(TaskXLineFieldType.WMS_ACTION)) {
            data[TaskXLineFieldType.WMS_ACTION] = WmsAction.RECEIPT
            Timber.d("Default WMS action was set: RECEIPT")
        }

        Timber.d("Fact row data was collected: $data")
        return data
    }
}