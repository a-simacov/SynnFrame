package com.synngate.synnframe.domain.service

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.FactLineWizardState
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.TaskX
import com.synngate.synnframe.domain.entity.taskx.TaskXLineFieldType
import com.synngate.synnframe.domain.usecase.taskx.TaskXUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.time.LocalDateTime

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
            groups = taskType.factLineActionGroups, // Передаем группы в состояние
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

        if (result == null) {
            // Переход к предыдущему шагу
            navigateBack(currentState)
        } else {
            // Сохранение результата и переход к следующему шагу
            saveResult(currentState, result)
        }
    }

    /**
     * Переход назад к предыдущему шагу
     */
    private fun navigateBack(currentState: FactLineWizardState) {
        var newGroupIndex = currentState.currentGroupIndex
        var newActionIndex = currentState.currentActionIndex - 1

        if (newActionIndex < 0) {
            newGroupIndex--
            if (newGroupIndex >= 0) {
                val prevGroup = currentState.groups[newGroupIndex]
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
    private fun saveResult(currentState: FactLineWizardState, result: Any) {
        if (currentState.currentGroupIndex >= currentState.groups.size) return

        val currentGroup = currentState.groups[currentState.currentGroupIndex]
        val targetField = currentGroup.targetFieldType

        // Обновляем результаты в зависимости от типа поля
        val updatedState = when (targetField) {
            TaskXLineFieldType.STORAGE_PRODUCT -> currentState.copy(storageProduct = result as? TaskProduct)
            TaskXLineFieldType.STORAGE_PALLET -> currentState.copy(storagePallet = result as? Pallet)
            TaskXLineFieldType.PLACEMENT_PALLET -> currentState.copy(placementPallet = result as? Pallet)
            TaskXLineFieldType.PLACEMENT_BIN -> currentState.copy(placementBin = result as? BinX)
        }

        // Записываем действие WMS из текущей группы, если оно еще не задано
        val updatedWithWmsAction = if (updatedState.wmsAction == null) {
            updatedState.copy(wmsAction = currentGroup.wmsAction)
        } else {
            updatedState
        }

        // Переходим к следующему шагу
        var newGroupIndex = currentState.currentGroupIndex
        var newActionIndex = currentState.currentActionIndex + 1

        // Если все действия в текущей группе выполнены, переходим к следующей группе
        if (newActionIndex >= currentGroup.actions.size) {
            newGroupIndex++
            newActionIndex = 0
        }

        _wizardState.value = updatedWithWmsAction.copy(
            currentGroupIndex = newGroupIndex,
            currentActionIndex = newActionIndex
        )
    }

    /**
     * Добавление строки факта и завершение мастера
     */
    suspend fun completeWizard(): Result<TaskX> {
        val state = _wizardState.value ?: return Result.failure(IllegalStateException("Состояние мастера не инициализировано"))

        // Используем метод createFactLine из состояния
        val factLine = state.createFactLine()
            ?: return Result.failure(IllegalStateException("Не удалось создать строку факта"))

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