package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.FactLineX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXLineFieldType
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.domain.model.wizard.StepResult
import com.synngate.synnframe.domain.model.wizard.WizardResultModel
import com.synngate.synnframe.domain.model.wizard.WizardState
import com.synngate.synnframe.domain.model.wizard.WizardStep
import com.synngate.synnframe.domain.usecase.wizard.FactLineWizardUseCases
import com.synngate.synnframe.presentation.ui.wizard.builder.WizardBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    private val coroutineJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + coroutineJob)

    // Initialize метод остается примерно тем же, но будет использовать WizardResultModel
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

            // Создаем начальный результат
            val initialResults = WizardResultModel()
            val stepResults = mutableMapOf<String, Any?>()

            // Инициализируем WMS действия
            taskType.factLineActionGroups.forEach { group ->
                when (group.targetFieldType) {
                    TaskXLineFieldType.STORAGE_PRODUCT ->
                        initialResults.wmsAction = group.wmsAction
                    TaskXLineFieldType.PLACEMENT_BIN ->
                        initialResults.wmsAction = group.wmsAction
                    TaskXLineFieldType.PLACEMENT_PALLET ->
                        initialResults.wmsAction = group.wmsAction
                    TaskXLineFieldType.STORAGE_PALLET ->
                        initialResults.wmsAction = group.wmsAction
                    else -> { /* Игнорируем */ }
                }

                // Для обратной совместимости также добавляем в Map
                stepResults["WMS_ACTION_${group.id}"] = group.wmsAction
            }

            _wizardState.value = WizardState(
                taskId = task.id,
                steps = steps,
                results = initialResults,
                stepResults = stepResults,
                isInitialized = true
            )
        } catch (e: Exception) {
            Timber.e(e, "Wizard init error")
            _wizardState.value = null
            throw e
        }
    }

    // Обновляем метод обработки результатов шага
    fun processStepResult(result: Any?) {
        val currentState = _wizardState.value ?: return
        val currentStep = currentState.currentStep ?: return

        when (result) {
            // Навигация назад при null
            null -> {
                if (currentState.canGoBack) {
                    _wizardState.value = currentState.copy(
                        currentStepIndex = currentState.currentStepIndex - 1
                    )
                }
                return
            }

            // Обработка StepResult
            is StepResult -> processStepResultImpl(result, currentState, currentStep)

            // Обработка обычных объектов
            else -> processResultData(result, currentState, currentStep)
        }
    }

    private fun processResultData(data: Any, currentState: WizardState, currentStep: WizardStep) {
        // Копируем существующие результаты
        var updatedResultModel = currentState.results
        val updatedStepResults = currentState.stepResults.toMutableMap()

        // Обновляем соответствующее поле в зависимости от типа результата
        when (data) {
            is TaskProduct -> {
                updatedResultModel = updatedResultModel.withStorageProduct(data)
                updatedStepResults[currentStep.id] = data
                updatedStepResults["STORAGE_PRODUCT"] = data
            }
            is Pallet -> {
                // Определяем тип паллеты по целевому полю группы действий
                currentStep.action?.let { action ->
                    val group = action.additionalParams["groupId"]?.toString()

                    val targetFieldType = currentStep.action?.let { action ->
                        // Пытаемся найти группу для этого действия
                        taskType?.factLineActionGroups?.find { it.actions.any { a -> a.id == action.id } }?.targetFieldType
                    }

                    when (targetFieldType) {
                        TaskXLineFieldType.STORAGE_PALLET -> {
                            updatedResultModel = updatedResultModel.withStoragePallet(data)
                            updatedStepResults["STORAGE_PALLET"] = data
                        }
                        TaskXLineFieldType.PLACEMENT_PALLET -> {
                            updatedResultModel = updatedResultModel.withPlacementPallet(data)
                            updatedStepResults["PLACEMENT_PALLET"] = data
                        }
                        else -> {
                            // Если не можем определить по группе, определяем по текущему контексту
                            if (updatedResultModel.storageProduct != null && updatedResultModel.storagePallet == null) {
                                updatedResultModel = updatedResultModel.withStoragePallet(data)
                                updatedStepResults["STORAGE_PALLET"] = data
                            } else {
                                updatedResultModel = updatedResultModel.withPlacementPallet(data)
                                updatedStepResults["PLACEMENT_PALLET"] = data
                            }
                        }
                    }

                    // Сохраняем результат и по ID шага
                    updatedStepResults[currentStep.id] = data
                }
            }
            is BinX -> {
                updatedResultModel = updatedResultModel.withPlacementBin(data)
                updatedStepResults[currentStep.id] = data
                updatedStepResults["PLACEMENT_BIN"] = data
            }
            is WmsAction -> {
                updatedResultModel = updatedResultModel.withWmsAction(data)
                updatedStepResults[currentStep.id] = data
                updatedStepResults["WMS_ACTION"] = data
            }
            else -> {
                // Для всех остальных типов просто сохраняем
                updatedStepResults[currentStep.id] = data
            }
        }

        // Валидация результатов
        if (!currentStep.validator(updatedStepResults)) {
            val errors = currentState.errors.toMutableMap()
            errors[currentStep.id] = "Недопустимое значение"
            _wizardState.value = currentState.copy(errors = errors)
            return
        }

        // Обновляем состояние и переходим к следующему шагу
        _wizardState.value = currentState.copy(
            currentStepIndex = currentState.currentStepIndex + 1,
            results = updatedResultModel,
            stepResults = updatedStepResults
        )
    }

    // Завершение визарда - создание строки факта
    suspend fun completeWizard(): Result<TaskX> {
        val state = _wizardState.value ?: return Result.failure(
            IllegalStateException("Wizard state was not initialized")
        )

        if (!state.isCompleted) {
            return Result.failure(IllegalStateException("Wizard was not completed"))
        }

        try {
            // Создание строки факта из структурированного состояния мастера
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

            // Сбрасываем состояние визарда для следующей строки
            _wizardState.value = state.copy(
                currentStepIndex = 0,
                results = WizardResultModel(),
                stepResults = emptyMap(),
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

        coroutineScope.launch(Dispatchers.IO) {
            factLineWizardUseCases.clearCache()
        }
    }

    fun dispose() {
        coroutineJob.cancel()
    }
}