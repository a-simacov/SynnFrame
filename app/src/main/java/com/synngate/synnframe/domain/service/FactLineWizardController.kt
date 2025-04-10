package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.FactLineActionGroup
import com.synngate.synnframe.domain.entity.taskx.FactLineWizardState
import com.synngate.synnframe.domain.entity.taskx.FactLineX
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXLineFieldType
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

/**
 * Контроллер для управления процессом создания строки факта
 */
class FactLineWizardController(
    private val taskXUseCases: TaskXUseCases
) {
    private val _wizardState = MutableStateFlow<FactLineWizardState?>(null)
    val wizardState: StateFlow<FactLineWizardState?> = _wizardState.asStateFlow()

    /**
     * Инициализация мастера для задания
     */
    suspend fun initialize(task: TaskX) {
        val taskType = taskXUseCases.getTaskType(task.taskTypeId) ?: return

        if (taskType.factLineActionGroups.isEmpty()) {
            Timber.w("Для типа задания ${taskType.name} не определены действия для добавления строки факта")
            return
        }

        _wizardState.value = FactLineWizardState(
            taskId = task.id,
            currentGroupIndex = 0,
            currentActionIndex = 0,
            startedAt = LocalDateTime.now()
        )
    }

    /**
     * Обработка результата текущего шага
     */
    suspend fun processStepResult(result: Any?) {
        val currentState = _wizardState.value ?: return
        val taskType = taskXUseCases.getTaskType(
            taskXUseCases.getTaskById(currentState.taskId)?.taskTypeId ?: ""
        ) ?: return

        val groups = taskType.factLineActionGroups

        if (result == null) {
            // Переход к предыдущему шагу
            navigateBack(currentState, groups)
        } else {
            // Сохранение результата и переход к следующему шагу
            saveResult(currentState, groups, result)
        }
    }

    /**
     * Переход назад к предыдущему шагу
     */
    private fun navigateBack(currentState: FactLineWizardState, groups: List<FactLineActionGroup>) {
        var newGroupIndex = currentState.currentGroupIndex
        var newActionIndex = currentState.currentActionIndex - 1

        if (newActionIndex < 0) {
            newGroupIndex--
            if (newGroupIndex >= 0) {
                val prevGroup = groups[newGroupIndex]
                newActionIndex = prevGroup.actions.size - 1
            }
        }

        // Не позволяем выйти за пределы первого шага
        if (newGroupIndex < 0) {
            newGroupIndex = 0
            newActionIndex = 0
        }

        _wizardState.value = currentState.copy(
            currentGroupIndex = newGroupIndex,
            currentActionIndex = newActionIndex
        )
    }

    /**
     * Сохранение результата текущего шага
     */
    private fun saveResult(
        currentState: FactLineWizardState,
        groups: List<FactLineActionGroup>,
        result: Any
    ) {
        if (currentState.currentGroupIndex >= groups.size) return

        val currentGroup = groups[currentState.currentGroupIndex]
        val targetField = currentGroup.targetFieldType

        // Обновляем результаты в зависимости от типа поля
        val updatedState = when (targetField) {
            TaskXLineFieldType.STORAGE_PRODUCT -> currentState.copy(storageProduct = result as? com.synngate.synnframe.domain.entity.taskx.TaskProduct)
            TaskXLineFieldType.STORAGE_PALLET -> currentState.copy(storagePallet = result as? com.synngate.synnframe.domain.entity.taskx.Pallet)
            TaskXLineFieldType.PLACEMENT_PALLET -> currentState.copy(placementPallet = result as? com.synngate.synnframe.domain.entity.taskx.Pallet)
            TaskXLineFieldType.PLACEMENT_BIN -> currentState.copy(placementBin = result as? com.synngate.synnframe.domain.entity.taskx.BinX)
        }

        // Переходим к следующему шагу
        var newGroupIndex = currentState.currentGroupIndex
        var newActionIndex = currentState.currentActionIndex + 1

        // Если все действия в текущей группе выполнены, переходим к следующей группе
        if (newActionIndex >= currentGroup.actions.size) {
            newGroupIndex++
            newActionIndex = 0
        }

        _wizardState.value = updatedState.copy(
            currentGroupIndex = newGroupIndex,
            currentActionIndex = newActionIndex
        )
    }

    /**
     * Создание объекта строки факта на основе текущего состояния
     */
    suspend fun createFactLine(): FactLineX? {
        val state = _wizardState.value ?: return null
        val task = taskXUseCases.getTaskById(state.taskId) ?: return null
        val taskType = taskXUseCases.getTaskType(task.taskTypeId) ?: return null

        // Определяем wmsAction из текущей группы действий или используем стандартное значение
        val wmsAction = if (state.currentGroupIndex < taskType.factLineActionGroups.size) {
            taskType.factLineActionGroups[0].wmsAction // Берем из первой группы для примера
        } else {
            state.wmsAction ?: com.synngate.synnframe.domain.entity.taskx.WmsAction.RECEIPT
        }

        return FactLineX(
            id = UUID.randomUUID().toString(),
            taskId = state.taskId,
            storageProduct = state.storageProduct,
            storagePallet = state.storagePallet,
            wmsAction = wmsAction,
            placementPallet = state.placementPallet,
            placementBin = state.placementBin,
            startedAt = state.startedAt,
            completedAt = LocalDateTime.now()
        )
    }

    /**
     * Добавление строки факта и завершение мастера
     */
    suspend fun completeWizard(): Result<TaskX> {
        val factLine = createFactLine() ?: return Result.failure(IllegalStateException("Не удалось создать строку факта"))
        val result = taskXUseCases.addFactLine(factLine)

        // Закрываем мастер в любом случае
        _wizardState.value = null

        return result
    }

    /**
     * Отмена мастера
     */
    fun cancelWizard() {
        _wizardState.value = null
    }
}