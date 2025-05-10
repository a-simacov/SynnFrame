package com.synngate.synnframe.presentation.ui.wizard.action.pallet

import androidx.lifecycle.viewModelScope
import com.synngate.synnframe.domain.entity.taskx.Pallet
import com.synngate.synnframe.domain.entity.taskx.action.ActionStep
import com.synngate.synnframe.domain.entity.taskx.action.PlannedAction
import com.synngate.synnframe.domain.model.wizard.ActionContext
import com.synngate.synnframe.domain.service.ValidationService
import com.synngate.synnframe.presentation.ui.wizard.action.base.BaseStepViewModel
import com.synngate.synnframe.presentation.ui.wizard.service.PalletLookupService
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel для шага выбора паллеты
 */
class PalletSelectionViewModel(
    step: ActionStep,
    action: PlannedAction,
    context: ActionContext,
    private val palletLookupService: PalletLookupService,
    validationService: ValidationService
) : BaseStepViewModel<Pallet>(step, action, context, validationService) {

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
        // ВАЖНО: Не устанавливаем паллету из плана как выбранную
        // Просто добавляем запланированную паллету в список
        if (plannedPallet != null) {
            filteredPallets = listOf(plannedPallet)
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
                if (result is Pallet) {
                    selectedPallet = result
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
        return result is Pallet
    }

    /**
     * Обработка штрих-кода
     */
    override fun processBarcode(barcode: String) {
        viewModelScope.launch {
            try {
                setLoading(true)
                setError(null)

                palletLookupService.processBarcode(
                    barcode = barcode,
                    // При запланированной паллете проверяем соответствие
                    expectedBarcode = plannedPallet?.code,
                    onResult = { found, data ->
                        if (found && data is Pallet) {
                            selectedPallet = data
                            setData(data)
                            // Очищаем поле ввода
                            updatePalletCodeInput("")
                        } else {
                            setError("Паллета с кодом '$barcode' не найдена")
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
     * Создаем расширенный контекст для валидации API
     */
    override fun createValidationContext(): Map<String, Any> {
        val baseContext = super.createValidationContext().toMutableMap()

        // Добавляем планируемую паллету и список паллет плана для валидации
        // Используем safe call, чтобы избежать добавления null-значений
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
        viewModelScope.launch {
            try {
                setLoading(true)
                // Поиск в репозитории, если он доступен, или локальная фильтрация
                val pallets = if (searchQuery.isEmpty() && plannedPallet != null) {
                    listOf(plannedPallet)
                } else {
                    palletLookupService.searchPallets(searchQuery)
                }
                filteredPallets = pallets
                updateAdditionalData("filteredPallets", filteredPallets)
                setLoading(false)
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при фильтрации паллет: ${e.message}")
                setError("Ошибка поиска: ${e.message}")
                setLoading(false)
            }
        }
    }

    /**
     * Создание новой паллеты
     */
    fun createNewPallet() {
        viewModelScope.launch {
            try {
                isCreatingPallet = true
                setLoading(true)
                setError(null)

                val result = palletLookupService.createNewPallet()

                if (result.isSuccess) {
                    val newPallet = result.getOrNull()
                    if (newPallet != null) {
                        selectedPallet = newPallet
                        setData(newPallet)
                        Timber.d("Создана новая паллета: ${newPallet.code}")
                    } else {
                        setError("Не удалось создать паллету: пустой результат")
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    setError("Не удалось создать паллету: ${exception?.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при создании паллеты: ${e.message}")
                setError("Ошибка создания паллеты: ${e.message}")
            } finally {
                isCreatingPallet = false
                setLoading(false)
            }
        }
    }

    /**
     * Выбор паллеты из списка
     */
    fun selectPallet(pallet: Pallet) {
        selectedPallet = pallet
        setData(pallet)
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
}