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

/**
 * ViewModel для шага выбора ячейки
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

    // Сохраняем выбранный бин для безопасного доступа
    private var selectedBin: BinX? = null

    // Состояние поля ввода кода ячейки - публичное свойство с private setter
    var binCodeInput = ""
        private set

    // Состояние поиска и списка ячеек
    var searchQuery = ""
        private set
    var filteredBins = emptyList<BinX>()
        private set
    var showBinsList = false
        private set

    // Состояние диалогов
    var showCameraScannerDialog = false
        private set

    init {
        // Если есть запланированная ячейка, добавляем её в filteredBins
        if (plannedBin != null) {
            filteredBins = listOf(plannedBin)
            // Также устанавливаем её как выбранную по умолчанию
            selectedBin = plannedBin
            setData(plannedBin)
        }

        // Инициализация из предыдущего контекста
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
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка инициализации из контекста: ${e.message}")
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

    /**
     * Валидация данных перед завершением шага
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
     * Обновление ввода кода ячейки - публичный метод, доступный из UI
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
                // Поиск в репозитории, если он доступен, или локальная фильтрация
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

    /**
     * Выбор ячейки из списка
     */
    fun selectBin(bin: BinX) {
        selectedBin = bin
        setData(bin)
    }

    /**
     * Отображение/скрытие списка ячеек
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
     * Получение запланированных ячеек
     */
    fun getPlanBins(): List<BinX> {
        return planBins
    }

    /**
     * Проверка наличия запланированных ячеек
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
}
