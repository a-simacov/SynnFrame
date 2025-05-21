package com.synngate.synnframe.presentation.ui.wizard.action.pallet

import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.action.ActionObjectType
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.service.PalletLookupService
import timber.log.Timber

class PalletSelectionViewModel(
    step: ActionStep,
    action: PlannedAction,
    context: ActionContext,
    private val palletLookupService: PalletLookupService,
    validationService: ValidationService,
    stepFactory: ActionStepFactory? = null
) : BaseStepViewModel<Pallet>(step, action, context, validationService, stepFactory) {

    private val isStorageStep = action.actionTemplate.storageSteps.any { it.id == step.id }

    private val plannedPallet = if (isStorageStep) action.storagePallet else action.placementPallet
    private val planPallets = listOfNotNull(plannedPallet)

    private var selectedPallet: Pallet? = null

    var palletCodeInput = ""
        private set

    var searchQuery = ""
        private set
    var filteredPallets = emptyList<Pallet>()
        private set
    var showPalletsList = false
        private set
    var isCreatingPallet = false
        private set

    var showCameraScannerDialog = false
        private set

    init {
        if (plannedPallet != null) {
            filteredPallets = listOf(plannedPallet)
        }
    }

    override fun isValidType(result: Any): Boolean {
        return result is Pallet
    }

    override fun onResultLoadedFromContext(result: Pallet) {
        selectedPallet = result
    }

    override fun applyAutoFill(data: Any): Boolean {
        if (data is Pallet) {
            try {
                Timber.d("Автозаполнение паллеты: ${data.code}")
                selectPallet(data)
                return true
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при автозаполнении паллеты: ${e.message}")
                return false
            }
        }
        return super.applyAutoFill(data)
    }

    override fun processBarcode(barcode: String) {
        executeWithErrorHandling("обработки кода паллеты") {
            palletLookupService.processBarcode(
                barcode = barcode,
                expectedBarcode = plannedPallet?.code,
                onResult = { found, data ->
                    if (found && data is Pallet) {
                        selectPallet(data)
                        updatePalletCodeInput("")
                    } else {
                        setError("Паллета с кодом '$barcode' не найдена")
                    }
                },
                onError = { message ->
                    Timber.e("PalletSelectionViewModel: ошибка при поиске паллеты: $message")
                    setError(message)
                }
            )
        }
    }

    override fun createValidationContext(): Map<String, Any> {
        val baseContext = super.createValidationContext().toMutableMap()

        plannedPallet?.let { baseContext["plannedPallet"] = it }
        if (planPallets.isNotEmpty()) {
            baseContext["planPallets"] = planPallets
        }

        return baseContext
    }

    override fun validateBasicRules(data: Pallet?): Boolean {
        if (data == null) return false

        if (plannedPallet != null && plannedPallet.code != data.code) {
            setError("Паллета не соответствует плану")
            return false
        }

        return true
    }

    fun updatePalletCodeInput(input: String) {
        palletCodeInput = input
        updateAdditionalData("palletCodeInput", input)
    }

    fun searchByPalletCode() {
        if (palletCodeInput.isNotEmpty()) {
            processBarcode(palletCodeInput)
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        filterPallets()
    }

    fun filterPallets() {
        executeWithErrorHandling("поиска паллет") {
            val pallets = palletLookupService.searchEntities(searchQuery)
            filteredPallets = pallets
            updateAdditionalData("filteredPallets", filteredPallets)
        }
    }

    fun createNewPallet() {
        executeWithErrorHandling("создания паллеты") {
            isCreatingPallet = true

            val result = palletLookupService.createNewPallet()

            if (result.isSuccess) {
                val newPallet = result.getOrNull()
                if (newPallet != null) {
                    selectPallet(newPallet)
                } else {
                    setError("Не удалось создать паллету: пустой результат")
                }
            } else {
                val exception = result.exceptionOrNull()
                setError("Не удалось создать паллету: ${exception?.message}")
            }

            isCreatingPallet = false
        }
    }

    fun selectPallet(pallet: Pallet) {
        selectedPallet = pallet

        markObjectForSaving(ActionObjectType.PALLET, pallet)

        if (stepFactory is AutoCompleteCapableFactory) {
            handleFieldUpdate("selectedPallet", pallet)
        } else {
            setData(pallet)
        }
    }

    fun toggleCameraScannerDialog(show: Boolean) {
        showCameraScannerDialog = show
        updateAdditionalData("showCameraScannerDialog", show)
    }

    fun hideCameraScannerDialog() {
        toggleCameraScannerDialog(false)
    }

    fun getPlanPallets(): List<Pallet> {
        return planPallets
    }

    fun hasPlanPallets(): Boolean {
        return planPallets.isNotEmpty()
    }

    fun isStoragePalletStep(): Boolean {
        return isStorageStep
    }

    fun getSelectedPallet(): Pallet? {
        return selectedPallet
    }

    fun hasSelectedPallet(): Boolean {
        return selectedPallet != null
    }

    fun isSelectedPalletMatchingPlan(): Boolean {
        val selected = selectedPallet ?: return false
        return plannedPallet != null && selected.code == plannedPallet.code
    }

    fun manuallyCompleteStep() {
        val pallet = selectedPallet
        if (pallet != null) {
            completeStep(pallet)
        } else {
            setError("Необходимо выбрать паллету")
        }
    }
}