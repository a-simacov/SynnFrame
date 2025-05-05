package com.synngate.synnframe.domain.entity.taskx.action

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import com.synngate.synnframe.util.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class PlannedAction(
    val id: String,
    val order: Int,
    val actionTemplate: ActionTemplate,
    val storageProduct: TaskProduct? = null,
    val storagePallet: Pallet? = null,
    val wmsAction: WmsAction,
    val placementPallet: Pallet? = null,
    val placementBin: BinX? = null,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,
    val isFinalAction: Boolean = false,

    // Новые поля для поддержки множественных фактических действий
    val progressType: ProgressType = ProgressType.SIMPLE,
    val manuallyCompleted: Boolean = false,

    // Поля для записи информации о ручном завершении
    @Serializable(with = LocalDateTimeSerializer::class)
    val manuallyCompletedAt: LocalDateTime? = null,
) {
    // Проверка возможности кликнуть по действию
    fun isClickable(): Boolean = !isSkipped

    // Расчет прогресса выполнения (от 0.0 до 1.0)
    fun calculateProgress(factActions: List<FactAction>): Float {
        // Для обычных действий возвращаем бинарный статус
        if (progressType == ProgressType.SIMPLE) {
            return if (isCompleted || manuallyCompleted) 1f else 0f
        }

        // Для действий с учетом количества
        if (progressType == ProgressType.QUANTITY && storageProduct != null) {
            // Суммируем количество из всех связанных фактических действий
            val relatedFacts = factActions.filter { it.plannedActionId == id }
            val completedQuantity = relatedFacts.sumOf {
                it.storageProduct?.quantity?.toDouble() ?: 0.0
            }.toFloat()

            val plannedQuantity = storageProduct.quantity

            // Если плановое количество отсутствует или равно нулю
            if (plannedQuantity <= 0f) {
                return if (isCompleted || manuallyCompleted || relatedFacts.isNotEmpty()) 1f else 0f
            }

            // Вычисляем процент выполнения
            return (completedQuantity / plannedQuantity).coerceIn(0f, 1f)
        }

        // По умолчанию используем стандартный статус
        return if (isCompleted || manuallyCompleted) 1f else 0f
    }

    // Определение, фактически завершено ли действие
    fun isActionCompleted(factActions: List<FactAction>): Boolean {
        // Если действие явно отмечено как завершенное через isCompleted
        if (isCompleted) return true

        // Если действие отмечено как завершенное вручную
        if (manuallyCompleted) return true

        // Для действий с учетом количества проверяем прогресс
        if (progressType == ProgressType.QUANTITY && storageProduct != null) {
            val progress = calculateProgress(factActions)
            return progress >= 1f
        }

        return false
    }

    // Проверка, может ли действие иметь несколько фактических действий
    fun canHaveMultipleFactActions(): Boolean {
        return progressType == ProgressType.QUANTITY
    }

    // Получение запланированного количества (для действий с учетом количества)
    fun getPlannedQuantity(): Float {
        return storageProduct?.quantity ?: 0f
    }

    // Получение выполненного количества на основе фактических действий
    fun getCompletedQuantity(factActions: List<FactAction>): Float {
        val relatedFacts = factActions.filter { it.plannedActionId == id }
        return relatedFacts.sumOf {
            it.storageProduct?.quantity?.toDouble() ?: 0.0
        }.toFloat()
    }

    // Получение оставшегося количества для выполнения
    fun getRemainingQuantity(factActions: List<FactAction>): Float {
        if (progressType != ProgressType.QUANTITY || storageProduct == null) {
            return 0f
        }

        val plannedQuantity = getPlannedQuantity()
        val completedQuantity = getCompletedQuantity(factActions)

        return (plannedQuantity - completedQuantity).coerceAtLeast(0f)
    }
}