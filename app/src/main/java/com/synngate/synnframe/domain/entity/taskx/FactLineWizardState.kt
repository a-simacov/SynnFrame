package com.synngate.synnframe.domain.entity.taskx

import com.synngate.synnframe.util.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class FactLineWizardState(
    val taskId: String,                         // ID задания
    val groups: List<FactLineActionGroup> = emptyList(), // Добавляем группы действий мастера
    val currentGroupIndex: Int = 0,             // Текущий индекс группы действий
    val currentActionIndex: Int = 0,            // Текущий индекс действия в группе
    // Промежуточные результаты для каждого поля
    val storageProduct: TaskProduct? = null,     // Товар хранения
    val storagePallet: Pallet? = null,           // Паллета хранения
    val placementPallet: Pallet? = null,         // Паллета размещения
    val placementBin: BinX? = null,              // Ячейка размещения
    val wmsAction: WmsAction? = null,            // Действие WMS
    // Дополнительные данные для UI
    @Serializable(with = LocalDateTimeSerializer::class)
    val startedAt: LocalDateTime = LocalDateTime.now(), // Время начала работы с мастером
    val errors: Map<String, String> = emptyMap()  // Ошибки валидации по полям
) {
    // Проверка завершения текущей группы
    fun isCurrentGroupCompleted(): Boolean {
        if (currentGroupIndex >= groups.size) return true
        val currentGroup = groups[currentGroupIndex]
        return currentActionIndex >= currentGroup.actions.size
    }

    // Проверка завершения всего мастера
    fun isCompleted(): Boolean {
        return currentGroupIndex >= groups.size
    }

    // Создание факт-строки из текущего состояния
    fun createFactLine(): FactLineX? {
        // Определим wmsAction из текущей группы действий или используем существующее значение
        val effectiveWmsAction = wmsAction ?: return null

        return FactLineX(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            storageProduct = storageProduct,
            storagePallet = storagePallet,
            wmsAction = effectiveWmsAction,
            placementPallet = placementPallet,
            placementBin = placementBin,
            startedAt = startedAt,
            completedAt = LocalDateTime.now()
        )
    }

    // Добавляем следующие методы в класс FactLineWizardState

    // Получение промежуточных результатов в виде карты
    fun getIntermediateResults(): Map<String, Any?> {
        val results = mutableMapOf<String, Any?>()

        // Добавляем все заполненные поля
        storageProduct?.let { results["STORAGE_PRODUCT"] = it }
        storagePallet?.let { results["STORAGE_PALLET"] = it }
        placementPallet?.let { results["PLACEMENT_PALLET"] = it }
        placementBin?.let { results["PLACEMENT_BIN"] = it }
        wmsAction?.let { results["WMS_ACTION"] = it }

        return results
    }

    // Проверка, можно ли вернуться на предыдущий шаг
    fun canGoBack(): Boolean {
        return currentGroupIndex > 0 || currentActionIndex > 0
    }

    // Проверка, является ли текущий шаг последним
    fun isLastStep(): Boolean {
        return isCurrentGroupCompleted() && currentGroupIndex >= groups.size - 1
    }

    // Получение значения прогресса (от 0.0f до 1.0f)
    fun getProgressValue(): Float {
        if (groups.isEmpty()) return 0f

        var completedSteps = 0
        var totalSteps = 0

        groups.forEachIndexed { groupIndex, group ->
            val actionsCount = group.actions.size
            totalSteps += actionsCount

            if (groupIndex < currentGroupIndex) {
                // Предыдущие группы полностью завершены
                completedSteps += actionsCount
            } else if (groupIndex == currentGroupIndex) {
                // Текущая группа
                completedSteps += currentActionIndex
            }
        }

        return if (totalSteps > 0) {
            completedSteps.toFloat() / totalSteps
        } else {
            0f
        }
    }

    // Получение текущего действия
    fun getCurrentAction(): FactLineXAction? {
        if (currentGroupIndex >= groups.size) return null
        val currentGroup = groups[currentGroupIndex]
        if (currentActionIndex >= currentGroup.actions.size) return null
        return currentGroup.actions[currentActionIndex]
    }
}