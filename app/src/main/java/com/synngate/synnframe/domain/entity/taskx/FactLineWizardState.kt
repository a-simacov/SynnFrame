package com.synngate.synnframe.domain.entity.taskx

import com.synngate.synnframe.util.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class FactLineWizardState(
    val taskId: String,                         // ID задания
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
    fun isCurrentGroupCompleted(groups: List<FactLineActionGroup>): Boolean {
        if (currentGroupIndex >= groups.size) return false
        val currentGroup = groups[currentGroupIndex]
        return currentActionIndex >= currentGroup.actions.size
    }

    // Проверка завершения всего мастера
    fun isCompleted(groups: List<FactLineActionGroup>): Boolean {
        return currentGroupIndex >= groups.size
    }

    // Получение текущего действия
    fun getCurrentAction(groups: List<FactLineActionGroup>): FactLineXAction? {
        if (currentGroupIndex >= groups.size) return null
        val currentGroup = groups[currentGroupIndex]
        if (currentActionIndex >= currentGroup.actions.size) return null
        return currentGroup.actions[currentActionIndex]
    }

    // Создание факт-строки из текущего состояния
    fun createFactLine(taskId: String): FactLineX? {
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
}