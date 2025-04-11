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
    var storageProduct: TaskProduct? = null,
    var storagePallet: Pallet? = null,
    var placementPallet: Pallet? = null,
    var placementBin: BinX? = null,
    var wmsAction: WmsAction? = null,

    // Дополнительные метаданные (при необходимости)
    val additionalData: MutableMap<String, Any?> = mutableMapOf()
)