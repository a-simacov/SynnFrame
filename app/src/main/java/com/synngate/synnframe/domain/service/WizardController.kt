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

/**
 * Контроллер для управления процессом создания строки факта
 */
class WizardController(
    private val factLineWizardUseCases: FactLineWizardUseCases
) {
    private val _wizardState = MutableStateFlow<WizardState?>(null)
    val wizardState: StateFlow<WizardState?> = _wizardState.asStateFlow()

    /**
     * Инициализация мастера на основе типа задания
     */
    suspend fun initialize(task: TaskX, builder: WizardBuilder) {
        // Сначала сбрасываем текущее состояние
        _wizardState.value = null

        // Небольшая задержка для обработки сброса состояния
        kotlinx.coroutines.delay(200)

        val taskType = factLineWizardUseCases.getTaskType(task.taskTypeId) ?: return

        // Используем билдер для создания шагов мастера
        val steps = builder.buildSteps(taskType)

        if (steps.isEmpty()) {
            Timber.w("Для типа задания ${taskType.name} не определены действия для добавления строки факта")
            return
        }

        // Заранее добавляем действия WMS в результаты
        val initialResults = mutableMapOf<String, Any?>()
        taskType.factLineActionGroups.forEach { group ->
            // Используем ID группы + строку "WMS_ACTION" как ключ
            initialResults["WMS_ACTION_${group.id}"] = group.wmsAction

            // Для большей надежности также добавим по типу целевого поля
            when (group.targetFieldType) {
                TaskXLineFieldType.STORAGE_PRODUCT ->
                    initialResults["STORAGE_WMS_ACTION"] = group.wmsAction
                TaskXLineFieldType.PLACEMENT_BIN ->
                    initialResults["PLACEMENT_WMS_ACTION"] = group.wmsAction
                TaskXLineFieldType.PLACEMENT_PALLET ->
                    initialResults["PLACEMENT_PALLET_WMS_ACTION"] = group.wmsAction
                TaskXLineFieldType.STORAGE_PALLET ->
                    initialResults["STORAGE_PALLET_WMS_ACTION"] = group.wmsAction
                else -> { /* Игнорируем неизвестные типы */ }
            }
        }

        // Создаем новое состояние с предустановленными WMS-действиями
        _wizardState.value = WizardState(
            taskId = task.id,
            steps = steps,
            results = initialResults,
            isInitialized = true
        )

        Timber.d("Инициализирован визард с ${steps.size} шагами и предустановленными WMS-действиями")
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
        updatedResults[currentStep.id] = result

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
            Timber.d("Создание строки факта из данных: $factLineData")

            // Получаем данные для строки факта
            val storageProduct = factLineData[TaskXLineFieldType.STORAGE_PRODUCT] as? TaskProduct
            val storagePallet = factLineData[TaskXLineFieldType.STORAGE_PALLET] as? Pallet
            val placementPallet = factLineData[TaskXLineFieldType.PLACEMENT_PALLET] as? Pallet
            val placementBin = factLineData[TaskXLineFieldType.PLACEMENT_BIN] as? BinX
            val wmsAction = factLineData[TaskXLineFieldType.WMS_ACTION] as? WmsAction ?: WmsAction.RECEIPT

            // Проверка наличия обязательных данных
            if (storageProduct == null) {
                Timber.w("Отсутствует товар в данных строки факта")
                return Result.failure(IllegalStateException("Товар не указан"))
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

            Timber.i("Добавление строки факта: ${factLine.id} для задания ${state.taskId}")

            // Добавление строки факта в задание
            val result = factLineWizardUseCases.addFactLine(factLine)

            // Не сбрасываем состояние, чтобы можно было продолжить добавление
            // Просто сбрасываем индекс шага на начало
            _wizardState.value = state.copy(
                currentStepIndex = 0,
                results = emptyMap(),
                startedAt = LocalDateTime.now()
            )

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

        // Запускаем очистку кэша асинхронно
        // Используем GlobalScope только здесь, так как WizardController не имеет собственного CoroutineScope
        GlobalScope.launch(Dispatchers.IO) {
            factLineWizardUseCases.clearCache()
        }
    }

    /**
     * Полный сброс состояния контроллера
     */
    fun reset() {
        _wizardState.value = null
    }
}