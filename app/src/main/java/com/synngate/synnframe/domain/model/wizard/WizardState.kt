package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.taskx.TaskTypeX
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import timber.log.Timber
import java.time.LocalDateTime

data class WizardState(
    val taskId: String = "",
    val taskType: TaskTypeX? = null, // Добавлено поле taskType
    val steps: List<WizardStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val results: WizardResultModel = WizardResultModel(),
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
     * Получает данные для создания строки факта из структурированной модели
     */
    fun getFactLineData(): Map<TaskXLineFieldType, Any?> {
        val data = mutableMapOf<TaskXLineFieldType, Any?>()

        results.storageProduct?.let {
            data[TaskXLineFieldType.STORAGE_PRODUCT] = it
            Timber.d("Using storage product: ${it.product.name}, quantity: ${it.quantity}")
        }

        results.storagePallet?.let {
            data[TaskXLineFieldType.STORAGE_PALLET] = it
            Timber.d("Using storage pallet: ${it.code}")
        }

        results.placementPallet?.let {
            data[TaskXLineFieldType.PLACEMENT_PALLET] = it
            Timber.d("Using placement pallet: ${it.code}")
        }

        results.placementBin?.let {
            data[TaskXLineFieldType.PLACEMENT_BIN] = it
            Timber.d("Using placement bin: ${it.code}")
        }

        results.wmsAction?.let {
            data[TaskXLineFieldType.WMS_ACTION] = it
            Timber.d("Using WMS action: $it")
        }

        // Если нет действия WMS, устанавливаем значение по умолчанию
        if (!data.containsKey(TaskXLineFieldType.WMS_ACTION)) {
            data[TaskXLineFieldType.WMS_ACTION] = WmsAction.RECEIPT
            Timber.d("Using default WMS action: RECEIPT")
        }

        return data
    }
}