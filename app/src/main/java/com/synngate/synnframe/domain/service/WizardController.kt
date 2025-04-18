package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.WmsAction
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

            // Заранее добавляем действия WMS в результаты
            val initialResults = WizardResultModel()

            // Устанавливаем действие WMS по умолчанию
            val defaultWmsAction = taskType.factLineActionGroups.firstOrNull()?.wmsAction ?: WmsAction.RECEIPT
            initialResults.wmsAction = defaultWmsAction

            _wizardState.value = WizardState(
                taskId = task.id,
                taskType = taskType,
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

    // Обновляем метод обработки результатов шага
    fun processStepResult(result: Any?) {
        val currentState = _wizardState.value ?: return
        val currentStep = currentState.currentStep ?: return

        // Для null результата (нажатие "Назад")
        if (result == null) {
            // ВАЖНО: здесь ключевое изменение
            Timber.d("Processing BACK navigation: currentStepIndex=${currentState.currentStepIndex}, stepsSize=${currentState.steps.size}")

            // Проверка на итоговый экран
            if (currentState.isCompleted || currentState.currentStepIndex >= currentState.steps.size) {
                // Переход к последнему шагу с предыдущего экрана
                val lastStepIndex = currentState.steps.size - 1
                if (lastStepIndex >= 0) {
                    Timber.d("Returning from summary to last step: $lastStepIndex")

                    // Создаем полностью новое состояние с нужным индексом
                    _wizardState.value = WizardState(
                        taskId = currentState.taskId,
                        taskType = currentState.taskType,
                        steps = currentState.steps,
                        currentStepIndex = lastStepIndex,
                        results = currentState.results,
                        startedAt = currentState.startedAt,
                        errors = currentState.errors,
                        isInitialized = true
                    )

                    Timber.d("New wizardState set with index: ${_wizardState.value?.currentStepIndex}")
                }
            } else if (currentState.canGoBack) {
                val newIndex = currentState.currentStepIndex - 1
                Timber.d("Going back to step: $newIndex")
                _wizardState.value = currentState.copy(
                    currentStepIndex = newIndex
                )
            }
            return
        }

        when (result) {
            null -> {
                // Обработка навигации назад
                if (currentState.canGoBack) {
                    _wizardState.value = currentState.copy(
                        currentStepIndex = currentState.currentStepIndex - 1
                    )
                }
                return
            }

            is StepResult -> {
                when (result) {
                    is StepResult.Data -> handleDataResult(currentState, currentStep, result.value)
                    is StepResult.Back -> handleBackResult(currentState)
                    is StepResult.Skip -> handleSkipResult(currentState, currentStep, result.value)
                    is StepResult.Cancel -> cancel()
                }
            }

            // ВАЖНО: WizardResultModel теперь обрабатывается напрямую
            is WizardResultModel -> {
                // Прямое обновление результатов
                if (currentStep.validator(result)) {
                    _wizardState.value = currentState.copy(
                        currentStepIndex = currentState.currentStepIndex + 1,
                        results = result
                    )
                } else {
                    val errors = currentState.errors.toMutableMap()
                    errors[currentStep.id] = "Недопустимое значение"
                    _wizardState.value = currentState.copy(errors = errors)
                }
            }

            else -> handleDataResult(currentState, currentStep, result)
        }
    }

    private fun handleBackResult(currentState: WizardState) {
        if (currentState.canGoBack) {
            _wizardState.value = currentState.copy(
                currentStepIndex = currentState.currentStepIndex - 1
            )
        }
    }

    private fun handleSkipResult(currentState: WizardState, currentStep: WizardStep, value: Any?) {
        // Если есть значение, которое нужно сохранить при пропуске шага
        val updatedResults = if (value != null) {
            // Обновляем модель в зависимости от типа результата
            when (value) {
                is TaskProduct -> currentState.results.withStorageProduct(value)
                is BinX -> currentState.results.withPlacementBin(value)
                is Pallet -> {
                    // Определяем тип паллеты на основе целевого поля текущей группы
                    val targetFieldType = getTargetFieldType(currentStep)
                    when (targetFieldType) {
                        TaskXLineFieldType.STORAGE_PALLET -> currentState.results.withStoragePallet(value)
                        TaskXLineFieldType.PLACEMENT_PALLET -> currentState.results.withPlacementPallet(value)
                        else -> currentState.results // Не обновляем, если не определен тип
                    }
                }
                is WmsAction -> currentState.results.withWmsAction(value)
                else -> {
                    // Сохраняем в additionalData для других типов
                    val updated = currentState.results.copy()
                    updated.additionalData["skip_${currentStep.id}"] = value
                    updated
                }
            }
        } else {
            // Если нет значения, просто сохраняем текущие результаты
            currentState.results
        }

        // Переходим к следующему шагу
        _wizardState.value = currentState.copy(
            currentStepIndex = currentState.currentStepIndex + 1,
            results = updatedResults
        )
    }

    // Вспомогательная функция для получения целевого поля из шага
    private fun getTargetFieldType(step: WizardStep): TaskXLineFieldType? {
        val action = step.action ?: return null
        val currentState = _wizardState.value ?: return null
        val taskType = currentState.taskType ?: return null

        // Ищем группу действий, к которой принадлежит текущее действие
        val group = taskType.factLineActionGroups.find { group ->
            group.actions.any { it.id == action.id }
        }

        return group?.targetFieldType
    }

    private fun handleDataResult(currentState: WizardState, currentStep: WizardStep, value: Any) {
        // Обновляем результаты в зависимости от типа данных
        val updatedResults = when (value) {
            is TaskProduct -> currentState.results.copy(storageProduct = value)
            is BinX -> currentState.results.copy(placementBin = value)
            is Pallet -> {
                // Определяем тип паллеты на основе целевого поля текущей группы
                val targetField = currentStep.action?.let { action ->
                    val groupId = action.id // Или другой способ получения ID группы
                    currentState.taskType?.factLineActionGroups?.find { it.id == groupId }?.targetFieldType
                }

                when (targetField) {
                    TaskXLineFieldType.STORAGE_PALLET -> currentState.results.copy(storagePallet = value)
                    TaskXLineFieldType.PLACEMENT_PALLET -> currentState.results.copy(placementPallet = value)
                    else -> {
                        // Если не удалось определить, выбираем по логике "сначала хранение, потом размещение"
                        if (currentState.results.storagePallet == null)
                            currentState.results.copy(storagePallet = value)
                        else
                            currentState.results.copy(placementPallet = value)
                    }
                }
            }
            is WmsAction -> currentState.results.copy(wmsAction = value)
            else -> {
                // Сохраняем в additionalData для других типов
                val updated = currentState.results.copy()
                updated.additionalData[currentStep.id] = value
                updated
            }
        }

        // Проверка валидации и переход к следующему шагу
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
            // ИСПРАВЛЕНО: удалено stepResults
            _wizardState.value = state.copy(
                currentStepIndex = 0,
                results = WizardResultModel(),
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

    // WizardController.kt
// Добавьте этот новый метод

    fun returnFromSummaryScreen() {
        val currentState = _wizardState.value ?: return

        Timber.d("Explicit return from summary screen requested")

        // Проверяем, что есть хотя бы один шаг
        if (currentState.steps.isEmpty()) {
            Timber.w("Cannot return from summary: no steps available")
            return
        }

        // Устанавливаем индекс на последний шаг
        val lastStepIndex = currentState.steps.size - 1

        Timber.d("Setting currentStepIndex from ${currentState.currentStepIndex} to $lastStepIndex")

        // Создаем полностью новое состояние
        val newState = WizardState(
            taskId = currentState.taskId,
            taskType = currentState.taskType,
            steps = currentState.steps,
            currentStepIndex = lastStepIndex,
            results = currentState.results,
            startedAt = currentState.startedAt,
            errors = currentState.errors,
            isInitialized = true
        )

        // Важно: непосредственно устанавливаем новое состояние
        _wizardState.value = newState

        Timber.d("New state set: currentStepIndex=${_wizardState.value?.currentStepIndex}, isCompleted=${_wizardState.value?.isCompleted}")
    }
}