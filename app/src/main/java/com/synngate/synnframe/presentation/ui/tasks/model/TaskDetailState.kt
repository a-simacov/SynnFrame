package com.synngate.synnframe.presentation.ui.tasks.model

import com.synngate.synnframe.domain.entity.FactLineAction
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.ProductUnit
import com.synngate.synnframe.domain.entity.Task
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskPlanLine

data class TaskLineItem(
    val planLine: TaskPlanLine,
    val factLine: TaskFactLine?,
    val product: Product?,
    val binName: String? = null
)

data class TaskDetailState(
    val taskId: String = "",

    val task: Task? = null,

    val taskLines: List<TaskLineItem> = emptyList(),

    val searchQuery: String = "",

    val scannedBarcode: String? = null,

    val scannedProduct: Product? = null,

    val selectedFactLine: TaskFactLine? = null,

    val selectedPlanQuantity: Float = 0f,

    val isScanDialogVisible: Boolean = false,

    val isFactLineDialogVisible: Boolean = false,

    val isCompleteConfirmationVisible: Boolean = false,

    val isLoading: Boolean = false,

    val isProcessing: Boolean = false,

    val error: String? = null,

    val isEditable: Boolean = false,

    val factLineDialogState: FactLineDialogState = FactLineDialogState(),

    val scanBarcodeDialogState: ScanBarcodeDialogState = ScanBarcodeDialogState(),

    val scannedUnit: ProductUnit? = null,

    val unitCoefficient: Float = 1f,

    val showDeleteConfirmation: Boolean = false,

    val isDeleting: Boolean = false,

    val isReuploading: Boolean = false,

    // Новые поля для последовательного ввода
    val currentScanHint: String = "",
    val temporaryProductId: String? = null,
    val temporaryProduct: Product? = null,
    val temporaryBinCode: String? = null,
    val formattedBinName: String? = null, // форматированное имя ячейки для отображения
    val temporaryQuantity: Float? = null,
    val isValidProduct: Boolean = true,
    val isValidBin: Boolean = true,

    val currentFactLineAction: FactLineAction? = null,
    val factLineActionIndex: Int = 0,
    val factLineActions: List<FactLineAction> = emptyList()
)