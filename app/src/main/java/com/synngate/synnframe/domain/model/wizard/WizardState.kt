package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskXLineFieldType
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import timber.log.Timber
import java.time.LocalDateTime

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
     * Получает данные для создания строки факта на основе последних результатов каждого типа
     */
    fun getFactLineData(): Map<TaskXLineFieldType, Any?> {
        val data = mutableMapOf<TaskXLineFieldType, Any?>()

        // Получаем записи последнего состояния каждого типа объекта
        // Сначала получаем все записи для каждого типа
        val productEntries = results.entries
            .filter { it.value is TaskProduct }
            .sortedBy { it.key } // Сортируем по ключу, предполагая что более поздние имеют больший ID

        val storagePalletEntries = results.entries
            .filter { it.value is Pallet && (it.key.contains("STORAGE", ignoreCase = true)) }
            .sortedBy { it.key }

        val placementPalletEntries = results.entries
            .filter { it.value is Pallet && (it.key.contains("PLACEMENT", ignoreCase = true)) }
            .sortedBy { it.key }

        val unspecifiedPalletEntries = results.entries
            .filter {
                it.value is Pallet &&
                        !it.key.contains("STORAGE", ignoreCase = true) &&
                        !it.key.contains("PLACEMENT", ignoreCase = true)
            }
            .sortedBy { it.key }

        val binEntries = results.entries
            .filter { it.value is BinX }
            .sortedBy { it.key }

        val actionEntries = results.entries
            .filter { it.value is WmsAction }
            .sortedBy { it.key }

        // Берем последний объект каждого типа (последний элемент в отсортированном списке)
        productEntries.lastOrNull()?.let { (key, value) ->
            if (value is TaskProduct) {
                data[TaskXLineFieldType.STORAGE_PRODUCT] = value
                Timber.d("Using latest product: ${value.product.name}, quantity: ${value.quantity}")
            }
        }

        storagePalletEntries.lastOrNull()?.let { (key, value) ->
            if (value is Pallet) {
                data[TaskXLineFieldType.STORAGE_PALLET] = value
                Timber.d("Using latest storage pallet: ${value.code}, closed: ${value.isClosed}")
            }
        }

        placementPalletEntries.lastOrNull()?.let { (key, value) ->
            if (value is Pallet) {
                data[TaskXLineFieldType.PLACEMENT_PALLET] = value
                Timber.d("Using latest placement pallet: ${value.code}, closed: ${value.isClosed}")
            }
        }

        // Если нет явно указанных паллет размещения, используем неопределенные
        if (!data.containsKey(TaskXLineFieldType.PLACEMENT_PALLET)) {
            unspecifiedPalletEntries.lastOrNull()?.let { (key, value) ->
                if (value is Pallet) {
                    data[TaskXLineFieldType.PLACEMENT_PALLET] = value
                    Timber.d("Using latest unspecified pallet as placement: ${value.code}, closed: ${value.isClosed}")
                }
            }
        }

        binEntries.lastOrNull()?.let { (key, value) ->
            if (value is BinX) {
                data[TaskXLineFieldType.PLACEMENT_BIN] = value
                Timber.d("Using latest bin: ${value.code}")
            }
        }

        actionEntries.lastOrNull()?.let { (key, value) ->
            if (value is WmsAction) {
                data[TaskXLineFieldType.WMS_ACTION] = value
                Timber.d("Using latest WMS action: $value")
            }
        }

        // Если нет действия WMS, устанавливаем значение по умолчанию
        if (!data.containsKey(TaskXLineFieldType.WMS_ACTION)) {
            data[TaskXLineFieldType.WMS_ACTION] = WmsAction.RECEIPT
            Timber.d("Using default WMS action: RECEIPT")
        }

        Timber.d("Final data for fact line: $data")
        return data
    }
}