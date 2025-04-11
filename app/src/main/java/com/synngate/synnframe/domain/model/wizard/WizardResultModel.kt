package com.synngate.synnframe.domain.model.wizard

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.TaskProduct
import com.synngate.synnframe.domain.entity.taskx.WmsAction

/**
 * Типизированная модель данных для результатов визарда
 */
data class WizardResultModel(
    var storageProduct: TaskProduct? = null,
    var storagePallet: Pallet? = null,
    var placementPallet: Pallet? = null,
    var placementBin: BinX? = null,
    var wmsAction: WmsAction? = null,

    val additionalData: MutableMap<String, Any?> = mutableMapOf()
) {
    // Методы для создания копий с обновленными полями
    fun withStorageProduct(product: TaskProduct): WizardResultModel {
        return copy(storageProduct = product)
    }

    fun withStoragePallet(pallet: Pallet): WizardResultModel {
        return copy(storagePallet = pallet)
    }

    fun withPlacementPallet(pallet: Pallet): WizardResultModel {
        return copy(placementPallet = pallet)
    }

    fun withPlacementBin(bin: BinX): WizardResultModel {
        return copy(placementBin = bin)
    }

    fun withWmsAction(action: WmsAction): WizardResultModel {
        return copy(wmsAction = action)
    }
}