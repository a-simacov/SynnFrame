package com.synngate.synnframe.presentation.ui.wizard.action.pallet

import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.ActionStepFactory
import com.synngate.synnframe.presentation.ui.wizard.action.AutoCompleteCapableFactory
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.service.PalletLookupService
import timber.log.Timber

/**
 * Оптимизированная ViewModel для шага выбора паллеты
 */
class PalletSelectionViewModel(
    step: ActionStep,
    action: PlannedAction,
    context: ActionContext,
    private val palletLookupService: PalletLookupService,
    validationService: ValidationService,
    stepFactory: ActionStepFactory? = null
) : BaseStepViewModel<Pallet>(step, action, context, validationService, stepFactory) {

    // Определяем, для какого типа объекта (хранения или размещения) нужна паллета
    private val isStorageStep = action.actionTemplate.storageSteps.any { it.id == step.id }

    // Данные о паллетах из плана
    private val plannedPallet = if (isStorageStep) action.storagePallet else action.placementPallet
    private val planPallets = listOfNotNull(plannedPallet)

    // Сохраняем выбранную паллету для безопасного доступа
    private var selectedPallet: Pallet? = null

    // Состояние поля ввода кода паллеты
    var palletCodeInput = ""
        private set

    // Состояние поиска и списка паллет
    var searchQuery = ""
        private set
    var filteredPallets = emptyList<Pallet>()
        private set
    var showPalletsList = false
        private set
    var isCreatingPallet = false
        private set

    // Состояние диалогов
    var showCameraScannerDialog = false
        private set

    init {
        // Если есть паллеты в плане, добавляем их в список
        if (plannedPallet != null) {
            filteredPallets = listOf(plannedPallet)
        }
    }

    /**
     * Проверка типа результата
     */
    override fun isValidType(result: Any): Boolean {
        return result is Pallet
    }

    /**
     * Переопределяем для загрузки паллеты из контекста
     */
    override fun onResultLoadedFromContext(result: Pallet) {
        selectedPallet = result
    }

    /**
     * Обработка штрих-кода
     */
    override fun processBarcode(barcode: String) {
        executeWithErrorHandling("обработки кода паллеты") {
            palletLookupService.processBarcode(
                barcode = barcode,
                // При запланированной паллете проверяем соответствие
                expectedBarcode = plannedPallet?.code,
                onResult = { found, data ->
                    if (found && data is Pallet) {
                        selectPallet(data)
                        // Очищаем поле ввода
                        updatePalletCodeInput("")
                    } else {
                        setError("Паллета с кодом '$barcode' не найдена")
                    }
                },
                onError = { message ->
                    setError(message)
                }
            )
        }
    }

    /**
     * Создаем расширенный контекст для валидации API
     */
    override fun createValidationContext(): Map<String, Any> {
        val baseContext = super.createValidationContext().toMutableMap()

        // Добавляем планируемую паллету для валидации
        plannedPallet?.let { baseContext["plannedPallet"] = it }
        if (planPallets.isNotEmpty()) {
            baseContext["planPallets"] = planPallets
        }

        return baseContext
    }

    /**
     * Валидация данных
     */
    override fun validateBasicRules(data: Pallet?): Boolean {
        if (data == null) return false

        // Если есть ограничение по плану, проверяем соответствие
        if (plannedPallet != null && plannedPallet.code != data.code) {
            setError("Паллета не соответствует плану")
            return false
        }

        return true
    }

    /**
     * Обновление ввода кода паллеты
     */
    fun updatePalletCodeInput(input: String) {
        palletCodeInput = input
        updateAdditionalData("palletCodeInput", input)
    }

    /**
     * Выполнение поиска по коду паллеты
     */
    fun searchByPalletCode() {
        if (palletCodeInput.isNotEmpty()) {
            processBarcode(palletCodeInput)
        }
    }

    /**
     * Обновление поискового запроса и фильтрация списка
     */
    fun updateSearchQuery(query: String) {
        searchQuery = query
        filterPallets()
    }

    /**
     * Фильтрация списка паллет
     */
    fun filterPallets() {
        executeWithErrorHandling("поиска паллет") {
            // Поиск через сервис
            val pallets = if (searchQuery.isEmpty() && plannedPallet != null) {
                listOf(plannedPallet)
            } else {
                palletLookupService.searchPallets(searchQuery)
            }
            filteredPallets = pallets
            updateAdditionalData("filteredPallets", filteredPallets)
        }
    }

    /**
     * Создание новой паллеты
     */
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

    /**
     * Выбор паллеты с поддержкой автоперехода
     */
    fun selectPallet(pallet: Pallet) {
        selectedPallet = pallet

        if (stepFactory is AutoCompleteCapableFactory) {
            handleFieldUpdate("selectedPallet", pallet)
        } else {
            setData(pallet)
        }
    }

    /**
     * Отображение/скрытие списка паллет
     */
    fun togglePalletsList(show: Boolean) {
        showPalletsList = show
        updateAdditionalData("showPalletsList", show)

        // При отображении списка обновляем данные
        if (show) {
            filterPallets()
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
     * Получение запланированных паллет
     */
    fun getPlanPallets(): List<Pallet> {
        return planPallets
    }

    /**
     * Проверка наличия запланированных паллет
     */
    fun hasPlanPallets(): Boolean {
        return planPallets.isNotEmpty()
    }

    /**
     * Проверяет, является ли шаг для выбора паллеты хранения или размещения
     */
    fun isStoragePalletStep(): Boolean {
        return isStorageStep
    }

    /**
     * Получение выбранной паллеты
     */
    fun getSelectedPallet(): Pallet? {
        return selectedPallet
    }

    /**
     * Проверка, выбрана ли паллета
     */
    fun hasSelectedPallet(): Boolean {
        return selectedPallet != null
    }

    /**
     * Проверяет, соответствует ли выбранная паллета плану
     */
    fun isSelectedPalletMatchingPlan(): Boolean {
        val selected = selectedPallet ?: return false
        return plannedPallet != null && selected.code == plannedPallet.code
    }

    /**
     * ИСПРАВЛЕНО: Добавлен метод для ручного завершения шага
     * Вызывается при нажатии кнопки "Вперед"
     */
    fun manuallyCompleteStep() {
        val pallet = selectedPallet
        if (pallet != null) {
            Timber.d("Вручную завершаем шаг с паллетой: ${pallet.code}")
            completeStep(pallet)
        } else {
            Timber.w("Попытка завершить шаг без выбранной паллеты")
            setError("Необходимо выбрать паллету")
        }
    }
}