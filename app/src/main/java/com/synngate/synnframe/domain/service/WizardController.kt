package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.FactLineX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXLineFieldType
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.domain.model.wizard.WizardState
import com.synngate.synnframe.domain.usecase.wizard.FactLineWizardUseCases
import com.synngate.synnframe.presentation.ui.wizard.builder.WizardBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

class WizardController(
    private val factLineWizardUseCases: FactLineWizardUseCases
) {
    private val _wizardState = MutableStateFlow<WizardState?>(null)
    val wizardState: StateFlow<WizardState?> = _wizardState.asStateFlow()

    suspend fun initialize(task: TaskX, builder: WizardBuilder) {
        try {
            // Сначала сбрасываем текущее состояние и ждем обработки
            _wizardState.value = null

            val taskType = factLineWizardUseCases.getTaskType(task.taskTypeId)
                ?: throw IllegalStateException("Task type was not found: ${task.taskTypeId}")

            val steps = builder.buildSteps(taskType)
            if (steps.isEmpty()) {
                throw IllegalStateException("Steps for wizard were not created")
            }

            // Заранее добавляем действия WMS в результаты
            val initialResults = mutableMapOf<String, Any?>()
            taskType.factLineActionGroups.forEach { group ->
                initialResults["WMS_ACTION_${group.id}"] = group.wmsAction

                // Добавляем по типу целевого поля для надежности
                when (group.targetFieldType) {
                    TaskXLineFieldType.STORAGE_PRODUCT ->
                        initialResults["STORAGE_WMS_ACTION"] = group.wmsAction
                    TaskXLineFieldType.PLACEMENT_BIN ->
                        initialResults["PLACEMENT_WMS_ACTION"] = group.wmsAction
                    TaskXLineFieldType.PLACEMENT_PALLET ->
                        initialResults["PLACEMENT_PALLET_WMS_ACTION"] = group.wmsAction
                    TaskXLineFieldType.STORAGE_PALLET ->
                        initialResults["STORAGE_PALLET_WMS_ACTION"] = group.wmsAction
                    else -> { /* Игнорируем */ }
                }
            }

            _wizardState.value = WizardState(
                taskId = task.id,
                steps = steps,
                results = initialResults,
                isInitialized = true
            )
        } catch (e: Exception) {
            Timber.e(e, "Wizard init error")
            _wizardState.value = null
            throw e
        }
    }

    fun processStepResult(result: Any?) {
        val currentState = _wizardState.value ?: return
        val currentStep = currentState.currentStep

        if (result == null) {
            if (currentState.canGoBack) {
                _wizardState.value = currentState.copy(
                    currentStepIndex = currentState.currentStepIndex - 1
                )
            }
            return
        }

        if (currentStep == null)
            return

        val updatedResults = currentState.results.toMutableMap()
        updatedResults[currentStep.id] = result

        if (!currentStep.validator(updatedResults)) {
            val errors = currentState.errors.toMutableMap()
            errors[currentStep.id] = "Недопустимое значение"
            _wizardState.value = currentState.copy(errors = errors)
            return
        }

        _wizardState.value = currentState.copy(
            currentStepIndex = currentState.currentStepIndex + 1,
            results = updatedResults
        )
    }

    suspend fun completeWizard(): Result<TaskX> {
        val state = _wizardState.value ?: return Result.failure(
            IllegalStateException("Wizard state was not initialized")
        )

        if (!state.isCompleted) {
            return Result.failure(IllegalStateException("Wizard was not completed"))
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

            // Проверка наличия обязательных данных
            if (storageProduct == null) {
                return Result.failure(IllegalStateException("Product was not specified"))
            }

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

            val result = factLineWizardUseCases.addFactLine(factLine)

            _wizardState.value = state.copy(
                currentStepIndex = 0,
                results = emptyMap(),
                startedAt = LocalDateTime.now()
            )

            return result
        } catch (e: Exception) {
            Timber.e(e, "Error on creating fact line")
            return Result.failure(e)
        }
    }

    fun cancel() {
        _wizardState.value = null

        GlobalScope.launch(Dispatchers.IO) {
            factLineWizardUseCases.clearCache()
        }
    }

    fun reset() {
        _wizardState.value = null

        GlobalScope.launch(Dispatchers.IO) {
            factLineWizardUseCases.clearCache()
        }
    }
}