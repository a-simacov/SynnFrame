package com.synngate.synnframe.presentation.ui.wizard.action.bin

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.taskx.BinX
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.service.BinLookupService
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Обновленная ViewModel для шага выбора ячейки
 */
class BinSelectionViewModel(
    step: ActionStep,
    action: PlannedAction,
    context: ActionContext,
    private val binLookupService: BinLookupService,
    validationService: ValidationService
) : BaseStepViewModel<BinX>(step, action, context, validationService) {

    // Данные о ячейках из плана
    private val plannedBin = action.placementBin
    private val planBins = listOfNotNull(plannedBin)
    private val zoneFilter = plannedBin?.zone

    // Сохраняем выбранную ячейку для безопасного доступа
    private var selectedBin: BinX? = null

    // Состояние поля ввода кода ячейки
    var binCodeInput = ""
        private set

    // Состояние поиска и списка ячеек
    var searchQuery = ""
        private set
    var filteredBins = emptyList<BinX>()
        private set
    var showBinsList = false
        private set

    // Состояние диалога сканера
    var showCameraScannerDialog = false
        private set

    init {
        // Инициализация списка фильтрованных ячеек
        if (plannedBin != null) {
            filteredBins = listOf(plannedBin)
        }

        // Инициализация из контекста
        initFromContext()
    }

    /**
     * Инициализация из данных контекста
     */
    private fun initFromContext() {
        if (context.hasStepResult) {
            try {
                val result = context.getCurrentStepResult()
                if (result is BinX) {
                    selectedBin = result
                    setData(result)
                    Timber.d("Initialized from context: bin=${result.code}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error initializing from context: ${e.message}")
            }
        }
    }

    /**
     * Проверка типа результата
     */
    override fun isValidType(result: Any): Boolean {
        return result is BinX
    }

    /**
     * Обработка штрих-кода
     */
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
                            selectBin(data)
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
                Timber.e(e, "Error processing barcode: $barcode")
                setError("Ошибка: ${e.message}")
                setLoading(false)
            }
        }
    }

    /**
     * Валидация данных
     */
    override fun validateBasicRules(data: BinX?): Boolean {
        if (data == null) return false

        // Если есть ограничение по плану, проверяем соответствие
        if (plannedBin != null && plannedBin.code != data.code) {
            setError("Ячейка не соответствует плану")
            return false
        }

        return true
    }

    /**
     * Обновление ввода кода ячейки
     */
    fun updateBinCodeInput(input: String) {
        binCodeInput = input
        updateAdditionalData("binCodeInput", input)
    }

    /**
     * Выполнение поиска по коду ячейки
     */
    fun searchByBinCode() {
        if (binCodeInput.isNotEmpty()) {
            processBarcode(binCodeInput)
        }
    }

    /**
     * Обновление поискового запроса и фильтрация списка
     */
    fun updateSearchQuery(query: String) {
        searchQuery = query
        filterBins()
    }

    /**
     * Фильтрация списка ячеек
     */
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
                Timber.e(e, "Error filtering bins: ${e.message}")
                setError("Ошибка поиска: ${e.message}")
                setLoading(false)
            }
        }
    }

    /**
     * Выбор ячейки с поддержкой автоперехода
     */
    fun selectBin(bin: BinX) {
        selectedBin = bin

        // Обновляем состояние и проверяем автопереход
        if (stepFactory is AutoCompleteCapableFactory) {
            handleFieldUpdate("selectedBin", bin)
        } else {
            setData(bin)
        }
    }

    /**
     * Управление видимостью диалога сканера
     */
    fun toggleCameraScannerDialog(show: Boolean) {
        showCameraScannerDialog = show
        updateAdditionalData("showCameraScannerDialog", show)
    }

    /**
     * Скрытие диалога сканера
     */
    fun hideCameraScannerDialog() {
        toggleCameraScannerDialog(false)
    }

    /**
     * Управление видимостью списка ячеек
     */
    fun toggleBinsList(show: Boolean) {
        showBinsList = show
        updateAdditionalData("showBinsList", show)

        // При отображении списка обновляем данные
        if (show) {
            filterBins()
        }
    }

    /**
     * Получение ячеек из плана
     */
    fun getPlanBins(): List<BinX> {
        return planBins
    }

    /**
     * Проверка наличия ячеек в плане
     */
    fun hasPlanBins(): Boolean {
        return planBins.isNotEmpty()
    }

    /**
     * Получение выбранной ячейки
     */
    fun getSelectedBin(): BinX? {
        return selectedBin
    }

    /**
     * Проверка, выбрана ли ячейка
     */
    fun hasSelectedBin(): Boolean {
        return selectedBin != null
    }

    /**
     * Проверяет, соответствует ли выбранная ячейка плану
     */
    fun isSelectedBinMatchingPlan(): Boolean {
        val selected = selectedBin ?: return false
        return plannedBin != null && selected.code == plannedBin.code
    }
}