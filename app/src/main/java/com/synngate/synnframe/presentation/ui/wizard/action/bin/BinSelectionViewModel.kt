package com.synngate.synnframe.presentation.ui.wizard.action.bin

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.service.BinLookupService
import kotlinx.coroutines.launch
import timber.log.Timber

class BinSelectionViewModel(
    step: ActionStep,
    action: PlannedAction,
    context: ActionContext,
    private val binLookupService: BinLookupService,
    validationService: ValidationService
) : BaseStepViewModel<BinX>(step, action, context, validationService) {

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

        initFromContext()
    }

    private fun initFromContext() {
        if (context.hasStepResult) {
            try {
                val result = context.getCurrentStepResult()
                if (result is BinX) {
                    selectedBin = result
                    setData(result)
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка инициализации из контекста: ${e.message}")
            }
        }
    }

    override fun isValidType(result: Any): Boolean {
        return result is BinX
    }

    override fun processBarcode(barcode: String) {
        viewModelScope.launch {
            try {
                setLoading(true)
                setError(null)

                binLookupService.processBarcode(
                    barcode = barcode,
                    // При запланированной ячейке проверяем соответствие
                    expectedBarcode = plannedBin?.code,
                    onResult = { found, data ->
                        if (found && data is BinX) {
                            selectedBin = data
                            setData(data)
                            // Очищаем поле ввода
                            updateBinCodeInput("")
                        } else {
                            setError("Ячейка с кодом '$barcode' не найдена")
                        }
                        setLoading(false)
                    },
                    onError = { message ->
                        setError(message)
                        setLoading(false)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при обработке штрихкода: $barcode")
                setError("Ошибка: ${e.message}")
                setLoading(false)
            }
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
        viewModelScope.launch {
            try {
                setLoading(true)
                val bins = if (searchQuery.isEmpty() && plannedBin != null) {
                    listOf(plannedBin)
                } else {
                    binLookupService.searchBins(searchQuery, zoneFilter)
                }
                filteredBins = bins
                updateAdditionalData("filteredBins", filteredBins)
                setLoading(false)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при фильтрации ячеек: ${e.message}")
                setError("Ошибка поиска: ${e.message}")
                setLoading(false)
            }
        }
    }

    fun selectBin(bin: BinX) {
        selectedBin = bin
        setData(bin)
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