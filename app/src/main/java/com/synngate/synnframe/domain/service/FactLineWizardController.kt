package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.FactLineX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.ProductStatus
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXLineFieldType
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.domain.usecase.wizard.FactLineWizardUseCases
import com.synngate.synnframe.presentation.ui.taskx.wizard.WizardFactory
import com.synngate.synnframe.presentation.ui.taskx.wizard.WizardStep
import com.synngate.synnframe.presentation.ui.wizard.FactLineWizardViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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
    val wizardState: StateFlow<WizardState?> = _wizardState.asStateFlow()

    /**
     * Инициализация мастера на основе типа задания
     */
    suspend fun initialize(task: TaskX) {
        val taskType = factLineWizardUseCases.getTaskType(task.taskTypeId) ?: return

        // Используем фабрику для создания шагов мастера
        val factory = WizardFactory(factLineWizardViewModel)
        val steps = factory.createWizardSteps(taskType)

        if (steps.isEmpty()) {
            Timber.w("Для типа задания ${taskType.name} не определены действия для добавления строки факта")
            return
        }

        _wizardState.value = WizardState(
            taskId = task.id,
            steps = steps,
            isInitialized = true
        )
    }

    /**
     * Обработка результата текущего шага
     */
    fun processStepResult(result: Any?) {
        val currentState = _wizardState.value ?: return
        val currentStep = currentState.currentStep

        Timber.d("Processing step result: ${currentStep?.title}, result: $result")

        if (result == null) {
            // Переход к предыдущему шагу
            if (currentState.canGoBack) {
                _wizardState.value = currentState.copy(
                    currentStepIndex = currentState.currentStepIndex - 1
                )
            }
            return
        }

        // Сохранение результата текущего шага
        if (currentStep == null)
            return
        val updatedResults = currentState.results.toMutableMap()

        // Обработка специальных типов результатов
        if (result is LocalDate) {
            // Обработка даты срока годности
            val storageProduct = updatedResults["STORAGE_PRODUCT"] as? TaskProduct
            if (storageProduct != null) {
                // Обновляем продукт с новым сроком годности
                val updatedProduct = storageProduct.copy(expirationDate = result)
                updatedResults["STORAGE_PRODUCT"] = updatedProduct
            }
        } else if (result is ProductStatus) {
            // Обработка статуса товара
            val storageProduct = updatedResults["STORAGE_PRODUCT"] as? TaskProduct
            if (storageProduct != null) {
                // Обновляем продукт с новым статусом
                val updatedProduct = storageProduct.copy(status = result)
                updatedResults["STORAGE_PRODUCT"] = updatedProduct
            }
        } else {
            // Стандартная обработка
            updatedResults[currentStep.id] = result
        }

        // Проверка валидации
        if (!currentStep.validator(updatedResults)) {
            val errors = currentState.errors.toMutableMap()
            errors[currentStep.id] = "Недопустимое значение"
            _wizardState.value = currentState.copy(errors = errors)
            return
        }

        // Переход к следующему шагу
        _wizardState.value = currentState.copy(
            currentStepIndex = currentState.currentStepIndex + 1,
            results = updatedResults
        )
    }

    /**
     * Создание строки факта из результатов мастера
     */
    suspend fun completeWizard(): Result<TaskX> {
        val state = _wizardState.value ?: return Result.failure(
            IllegalStateException("Состояние мастера не инициализировано")
        )

        if (!state.isCompleted) {
            return Result.failure(IllegalStateException("Мастер не завершен"))
        }

        try {
            // Создание строки факта из состояния мастера
            val factLineData = state.getFactLineData()

            // Получаем данные для строки факта
            val storageProduct = factLineData[TaskXLineFieldType.STORAGE_PRODUCT] as? TaskProduct
            val storagePallet = factLineData[TaskXLineFieldType.STORAGE_PALLET] as? Pallet
            val placementPallet = factLineData[TaskXLineFieldType.PLACEMENT_PALLET] as? Pallet
            val placementBin = factLineData[TaskXLineFieldType.PLACEMENT_BIN] as? BinX
            val wmsAction = factLineData[TaskXLineFieldType.WMS_ACTION] as? WmsAction ?: WmsAction.RECEIPT

            val factLine = FactLineX(
                id = UUID.randomUUID().toString(),
                taskId = state.taskId,
                storageProduct = storageProduct,
                storagePallet = storagePallet,
                wmsAction = wmsAction,
                placementPallet = placementPallet,
                placementBin = placementBin,
                startedAt = state.startedAt,
                completedAt = LocalDateTime.now()
            )

            // Добавление строки факта в задание
            val result = factLineWizardUseCases.addFactLine(factLine)

            // Сброс состояния мастера
            cancel()

            return result
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при создании строки факта")
            return Result.failure(e)
        }
    }

    /**
     * Отмена мастера
     */
    fun cancel() {
        _wizardState.value = null
        factLineWizardUseCases.clearCache()
    }
}