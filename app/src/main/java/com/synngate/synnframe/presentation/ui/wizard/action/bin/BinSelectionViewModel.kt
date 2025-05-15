package com.synngate.synnframe.presentation.ui.wizard.action.bin

import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.service.BinLookupService

class BinSelectionViewModel(
    step: ActionStep,
    action: PlannedAction,
    context: ActionContext,
    private val binLookupService: BinLookupService,
    validationService: ValidationService,
    stepFactory: ActionStepFactory? = null
) : BaseStepViewModel<BinX>(step, action, context, validationService, stepFactory) {

    private val plannedBin = action.placementBin
    private val planBins = listOfNotNull(plannedBin)
    private val zoneFilter = plannedBin?.zone

    private var selectedBin: BinX? = null

    var binCodeInput = ""
        private set

    var searchQuery = ""
        private set
    var filteredBins = emptyList<BinX>()
        private set
    var showBinsList = false
        private set

    var showCameraScannerDialog = false
        private set

    init {
        if (plannedBin != null) {
            filteredBins = listOf(plannedBin)
        }
    }

    override fun isValidType(result: Any): Boolean {
        return result is BinX
    }

    override fun onResultLoadedFromContext(result: BinX) {
        selectedBin = result
    }

    override fun processBarcode(barcode: String) {
        executeWithErrorHandling("обработки кода ячейки") {
            binLookupService.processBarcode(
                barcode = barcode,
                expectedBarcode = plannedBin?.code,
                onResult = { found, data ->
                    if (found && data is BinX) {
                        selectBin(data)
                        updateBinCodeInput("")
                    } else {
                        setError("Ячейка с кодом '$barcode' не найдена")
                    }
                },
                onError = { message ->
                    setError(message)
                }
            )
        }
    }

    override fun validateBasicRules(data: BinX?): Boolean {
        if (data == null) return false

        if (plannedBin != null && plannedBin.code != data.code) {
            setError("Ячейка не соответствует плану")
            return false
        }

        return true
    }

    fun updateBinCodeInput(input: String) {
        binCodeInput = input
        updateAdditionalData("binCodeInput", input)
    }

    fun searchByBinCode() {
        if (binCodeInput.isNotEmpty()) {
            processBarcode(binCodeInput)
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        filterBins()
    }

    fun filterBins() {
        executeWithErrorHandling("поиска ячеек") {
            val bins = if (searchQuery.isEmpty() && plannedBin != null) {
                listOf(plannedBin)
            } else {
                binLookupService.searchBins(searchQuery, zoneFilter)
            }
            filteredBins = bins
            updateAdditionalData("filteredBins", filteredBins)
        }
    }

    fun selectBin(bin: BinX) {
        selectedBin = bin

        if (stepFactory is AutoCompleteCapableFactory) {
            handleFieldUpdate("selectedBin", bin)
        } else {
            setData(bin)
        }
    }

    fun toggleCameraScannerDialog(show: Boolean) {
        showCameraScannerDialog = show
        updateAdditionalData("showCameraScannerDialog", show)
    }

    fun hideCameraScannerDialog() {
        toggleCameraScannerDialog(false)
    }

    fun getPlanBins(): List<BinX> {
        return planBins
    }

    fun hasPlanBins(): Boolean {
        return planBins.isNotEmpty()
    }

    fun getSelectedBin(): BinX? {
        return selectedBin
    }

    fun hasSelectedBin(): Boolean {
        return selectedBin != null
    }

    fun isSelectedBinMatchingPlan(): Boolean {
        val selected = selectedBin ?: return false
        return plannedBin != null && selected.code == plannedBin.code
    }
}