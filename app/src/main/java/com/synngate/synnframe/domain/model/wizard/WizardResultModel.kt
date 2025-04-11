package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.WmsAction

/**
 * Структурированная модель для хранения результатов визарда
 */
data class WizardResultModel(
    // Основные объекты для строки факта
    val storageProduct: TaskProduct? = null,
    val storagePallet: Pallet? = null,
    val placementPallet: Pallet? = null,
    val placementBin: BinX? = null,
    val wmsAction: WmsAction? = null,

    // Дополнительные метаданные (при необходимости)
    val additionalData: Map<String, Any?> = emptyMap()
) {
    /**
     * Создает копию с обновленным продуктом хранения
     */
    fun withStorageProduct(product: TaskProduct?): WizardResultModel {
        return copy(storageProduct = product)
    }

    /**
     * Создает копию с обновленной паллетой хранения
     */
    fun withStoragePallet(pallet: Pallet?): WizardResultModel {
        return copy(storagePallet = pallet)
    }

    /**
     * Создает копию с обновленной паллетой размещения
     */
    fun withPlacementPallet(pallet: Pallet?): WizardResultModel {
        return copy(placementPallet = pallet)
    }

    /**
     * Создает копию с обновленной ячейкой размещения
     */
    fun withPlacementBin(bin: BinX?): WizardResultModel {
        return copy(placementBin = bin)
    }

    /**
     * Создает копию с обновленным действием WMS
     */
    fun withWmsAction(action: WmsAction?): WizardResultModel {
        return copy(wmsAction = action)
    }

    /**
     * Создает копию с добавленными дополнительными данными
     */
    fun withAdditionalData(key: String, value: Any?): WizardResultModel {
        val updatedData = additionalData.toMutableMap()
        updatedData[key] = value
        return copy(additionalData = updatedData)
    }
}