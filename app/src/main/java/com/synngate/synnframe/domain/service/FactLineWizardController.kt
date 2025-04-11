package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskXLineFieldType
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.domain.model.wizard.WizardStep
import com.synngate.synnframe.domain.usecase.wizard.FactLineWizardUseCases
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime

/**
 * Контроллер для управления процессом создания строки факта
 */
class FactLineWizardController(
    private val factLineWizardUseCases: FactLineWizardUseCases,
    private val factLineWizardViewModel: FactLineWizardViewModel
) {
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
         * Получает результаты для создания строки факта
         */
        fun getFactLineData(): Map<TaskXLineFieldType, Any?> {
            val data = mutableMapOf<TaskXLineFieldType, Any?>()

            // Собираем данные из результатов
            results.forEach { (key, value) ->
                when {
                    key.contains("STORAGE_PRODUCT") && value is TaskProduct ->
                        data[TaskXLineFieldType.STORAGE_PRODUCT] = value
                    key.contains("STORAGE_PALLET") && value is Pallet ->
                        data[TaskXLineFieldType.STORAGE_PALLET] = value
                    key.contains("PLACEMENT_PALLET") && value is Pallet ->
                        data[TaskXLineFieldType.PLACEMENT_PALLET] = value
                    key.contains("PLACEMENT_BIN") && value is BinX ->
                        data[TaskXLineFieldType.PLACEMENT_BIN] = value
                    key.contains("WMS_ACTION") && value is WmsAction ->
                        data[TaskXLineFieldType.WMS_ACTION] = value
                }
            }

            return data
        }
    }

    private val _wizardState = MutableStateFlow<WizardState?>(null)

    /**
     * Отмена мастера
     */
    fun cancel() {
        _wizardState.value = null
        factLineWizardUseCases.clearCache()
    }
}