package com.synngate.synnframe.domain.entity.taskx.action

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.WmsAction
import kotlinx.serialization.Serializable

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
    val plannedQuantity: Float = storageProduct?.quantity ?: 0f
) {
    /**
     * Проверяет, можно ли кликнуть на действие
     */
    fun isClickable(): Boolean = !isCompleted && !isSkipped

//    /**
//     * Получает плановое количество товара
//     * Если продукт не указан, возвращает 0
//     */
//    fun getPlannedQuantity(): Float {
//        return plannedQuantity
//    }

    /**
     * Получает выполненное количество из связанных фактических действий
     * @param factActions список фактических действий
     * @return сумма выполненных количеств
     */
    fun getCompletedQuantity(factActions: List<FactAction>): Float {
        // Фильтруем фактические действия, связанные с этим плановым
        val relatedFacts = factActions.filter { it.plannedActionId == id }

        // Суммируем количества из фактических действий
        return relatedFacts.sumOf {
            (it.storageProduct?.quantity ?: 0f).toDouble()
        }.toFloat()
    }

    /**
     * Рассчитывает процент выполнения действия
     * @param factActions список фактических действий
     * @return процент выполнения от 0.0 до 1.0
     */
    fun getCompletionProgress(factActions: List<FactAction>): Float {
        val planned = plannedQuantity
        if (planned <= 0f) return if (isCompleted) 1f else 0f

        val completed = getCompletedQuantity(factActions)
        return (completed / planned).coerceIn(0f, 1f)
    }

    /**
     * Проверяет, частично ли выполнено действие
     * @param factActions список фактических действий
     * @return true если действие частично выполнено
     */
    fun isPartiallyCompleted(factActions: List<FactAction>): Boolean {
        if (isCompleted || isSkipped) return false

        val completed = getCompletedQuantity(factActions)
        return completed > 0 && !isCompleted
    }

    /**
     * Проверяет, доступно ли действие для выполнения
     * @return true если действие не пропущено и не полностью завершено
     */
    fun isAvailableForExecution(): Boolean {
        return !isSkipped && !isCompleted
    }
}