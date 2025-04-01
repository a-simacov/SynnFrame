package com.synngate.synnframe.presentation.ui.tasks

import com.synngate.synnframe.domain.entity.FactLineActionType
import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskPlanLine
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.service.SoundService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.settings.SettingsUseCases
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
import com.synngate.synnframe.domain.usecase.tasktype.TaskTypeUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.products.components.ScanResult
import com.synngate.synnframe.presentation.ui.tasks.model.FactLineDialogState
import com.synngate.synnframe.presentation.ui.tasks.model.ScanBarcodeDialogState
import com.synngate.synnframe.presentation.ui.tasks.model.ScanOrder
import com.synngate.synnframe.presentation.ui.tasks.model.TaskDetailEvent
import com.synngate.synnframe.presentation.ui.tasks.model.TaskDetailState
import com.synngate.synnframe.presentation.ui.tasks.model.TaskLineItem
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import com.synngate.synnframe.util.bin.BinFormatter
import com.synngate.synnframe.util.bin.BinValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.UUID

class TaskDetailViewModel(
    private val taskId: String,
    private val taskUseCases: TaskUseCases,
    private val productUseCases: ProductUseCases,
    private val userUseCases: UserUseCases,
    private val settingsUseCases: SettingsUseCases,
    private val taskTypeUseCases: TaskTypeUseCases,
    private val soundService: SoundService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<TaskDetailState, TaskDetailEvent>(TaskDetailState(taskId = taskId)) {

    private val scannedBarcodeCache = mutableMapOf<String, Product?>()
    private val _scanOrder = MutableStateFlow(ScanOrder.PRODUCT_FIRST)
    private var binValidator: BinValidator? = null
    private var binFormatter: BinFormatter? = null

    init {
        loadTask()
        initBinSettings()
    }

    fun loadTask() {
        scannedBarcodeCache.clear()
        launchIO {
            updateState { it.copy(isLoading = true) }

            try {
                val task = taskUseCases.getTaskById(taskId)

                if (task != null) {
                    // Загружаем тип задания
                    val taskType = taskTypeUseCases.getTaskTypeById(task.taskTypeId)

                    // Создаем обновленную версию задания с типом
                    val taskWithType = if (taskType != null) {
                        task.copy(taskType = taskType)
                    } else {
                        task
                    }

                    // Получаем информацию о продуктах для строк плана и факта
                    val productIds = (task.planLines.map { it.productId } +
                            task.factLines.map { it.productId }).toSet()
                    val productsList = productUseCases.getProductsByIds(productIds)
                    val products = productsList.associateBy { it.id }

                    // Комбинируем строки плана и факта
                    val taskLines = mutableListOf<TaskLineItem>()

                    // Добавляем все строки из плана
                    task.planLines.forEach { planLine ->
                        val factLine = task.factLines.find { it.productId == planLine.productId }
                        val product = products[planLine.productId]

                        // Форматируем имя ячейки, если она есть
                        val binCode = factLine?.binCode ?: planLine.binCode
                        val binName = if (binCode != null) {
                            binFormatter?.formatBinName(binCode) ?: binCode
                        } else null

                        taskLines.add(
                            TaskLineItem(
                                planLine = planLine,
                                factLine = factLine,
                                product = product,
                                binName = binName
                            )
                        )
                    }

                    // Если разрешены товары не из плана, добавляем строки факта, которых нет в плане
                    if (task.allowProductsNotInPlan) {
                        task.factLines
                            .filter { factLine -> task.planLines.none { it.productId == factLine.productId } }
                            .forEach { factLine ->
                                val product = products[factLine.productId]

                                // Создаем фиктивную строку плана для отображения
                                val dummyPlanLine = TaskPlanLine(
                                    id = "dummy-${factLine.id}",
                                    taskId = factLine.taskId,
                                    productId = factLine.productId,
                                    quantity = 0f  // Нулевое плановое количество
                                )

                                taskLines.add(
                                    TaskLineItem(
                                        planLine = dummyPlanLine,
                                        factLine = factLine,
                                        product = product
                                    )
                                )
                            }
                    }

                    // Проверяем, доступно ли редактирование задания
                    val isEditable = task.status == TaskStatus.IN_PROGRESS

                    updateState { state ->
                        state.copy(
                            task = taskWithType,
                            taskLines = taskLines,
                            isLoading = false,
                            isEditable = isEditable,
                            error = null
                        )
                    }

                    if (isEditable) {
                        // Запускаем цикл сканирования в соответствии с настройками порядка
                        initFactLineInput()
                    }
                } else {
                    updateState { state ->
                        state.copy(
                            isLoading = false,
                            error = "Задание не найдено"
                        )
                    }
                    sendEvent(TaskDetailEvent.ShowSnackbar("Задание не найдено"))
                    sendEvent(TaskDetailEvent.NavigateBack)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading task")
                updateState { state ->
                    state.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка загрузки задания"
                    )
                }
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка загрузки задания: ${e.message}"))
            }
        }
    }

    private fun initBinSettings() {
        launchIO {
            // Загружаем шаблон ячейки и порядок сканирования из настроек
            val pattern = settingsUseCases.binCodePattern.first()
            binValidator = BinValidator(pattern)
            binFormatter = BinFormatter(pattern)

            _scanOrder.value = settingsUseCases.scanOrder.first()

            // Устанавливаем начальное состояние сканирования в зависимости от порядка
            resetScanBarcodeDialogState()
        }
    }

    fun initFactLineInput() {
        launchIO {
            val task = uiState.value.task ?: return@launchIO

            // Используем taskType из задания, если он есть
            val taskType = task.taskType ?: taskTypeUseCases.getTaskTypeById(task.taskTypeId)

            if (taskType != null) {
                // Сортируем действия по порядку
                val sortedActions = taskType.factLineActions.sortedBy { it.order }

                // Важное исправление: сбрасываем все временные данные
                updateState { it.copy(
                    factLineActions = sortedActions,
                    factLineActionIndex = 0,
                    currentFactLineAction = sortedActions.firstOrNull(),
                    temporaryProductId = null,
                    temporaryProduct = null,
                    temporaryBinCode = null,
                    formattedBinName = null,
                    temporaryQuantity = null,
                    isValidProduct = true,
                    isValidBin = true,
                    searchQuery = ""  // Очищаем поле ввода
                ) }

                // Обновляем подсказку для текущего действия
                updateScanHint()
            } else {
                sendEvent(TaskDetailEvent.ShowSnackbar("Не удалось получить тип задания"))
            }
        }
    }

    // Обновление подсказки для текущего действия
    private fun updateScanHint() {
        val state = uiState.value
        val action = state.currentFactLineAction

        if (action != null) {
            updateState { it.copy(
                currentScanHint = action.promptText
            ) }
        }
    }

    // Переход к следующему действию
    fun moveToNextFactLineAction() {
        val state = uiState.value
        val nextIndex = state.factLineActionIndex + 1

        if (nextIndex < state.factLineActions.size) {
            // Есть следующее действие, переходим к нему
            val nextAction = state.factLineActions[nextIndex]

            updateState { it.copy(
                factLineActionIndex = nextIndex,
                currentFactLineAction = nextAction
            )}

            // Очищаем поле ввода для следующего действия
            clearSearchQuery()

            // Уведомляем пользователя о следующем действии
            sendEvent(TaskDetailEvent.ShowSnackbar(nextAction.promptText))
        } else {
            // Все действия выполнены, сохраняем строку факта
            completeFactLineInput()
        }
    }

    // Обработка сканирования для текущего действия
// Оставить только этот метод, удалив processScanResult()
    fun processScanResultForCurrentAction(barcode: String) {
        val action = uiState.value.currentFactLineAction ?: return

        // Добавить обработку для отсутствия действия
        if (action == null) {
            initFactLineInput()
            return
        }

        when (action.type) {
            FactLineActionType.ENTER_PRODUCT_ANY,
            FactLineActionType.ENTER_PRODUCT_FROM_PLAN -> processProductBarcode(barcode, action.type)

            FactLineActionType.ENTER_BIN_ANY,
            FactLineActionType.ENTER_BIN_FROM_PLAN -> processBinBarcode(barcode, action.type)

            FactLineActionType.ENTER_QUANTITY -> {
                try {
                    val quantity = barcode.toFloatOrNull()
                    if (quantity != null && quantity > 0) {
                        updateState { it.copy(temporaryQuantity = quantity) }
                        moveToNextFactLineAction()
                        soundService.playSuccessSound()
                    } else {
                        sendEvent(TaskDetailEvent.ShowSnackbar("Необходимо ввести положительное число"))
                        soundService.playErrorSound()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing quantity from barcode")
                    sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка ввода количества"))
                    soundService.playErrorSound()
                }
            }
        }

        clearSearchQuery()
    }

    /**
     * Обрабатывает ввод в поле поиска товаров
     */
    fun onSearchQueryChanged(query: String) {
        updateState { it.copy(searchQuery = query) }

        // Обработка специальных команд
        when (query) {
            "0" -> {
                // Переход к списку товаров
                sendEvent(TaskDetailEvent.NavigateToProductsList)
                updateState { it.copy(searchQuery = "") }
                return
            }
            // Можно добавить другие специальные команды при необходимости
        }

        if (query.length >= MIN_BARCODE_LENGTH) {
            processScanResultForCurrentAction(query)
        }
    }

    private fun processBinInput(binCode: String) {
        Timber.d("Processing bin input: $binCode")

        // Пропускаем проверку формата ячейки, если binValidator не инициализирован
        val isValidBin = binValidator?.isValidBin(binCode) ?: true

        if (!isValidBin) {
            Timber.d("Invalid bin format: $binCode")
            sendEvent(TaskDetailEvent.ShowSnackbar("Неверный формат ячейки"))
            soundService.playErrorSound()
            return
        }

        // Форматируем имя ячейки
        val formattedBin = binFormatter?.formatBinName(binCode) ?: binCode

        Timber.d("Valid bin: $binCode, formatted: $formattedBin")

        // Обновляем состояние
        updateState { state ->
            state.copy(
                temporaryBinCode = binCode,
                formattedBinName = formattedBin,
                currentScanHint = "Отсканируйте или введите товар"
            )
        }

        // Оповещаем пользователя
        sendEvent(TaskDetailEvent.ShowSnackbar("Ячейка ${formattedBin} принята"))
        soundService.playSuccessSound()

        // Если товар уже введен, переходим к вводу количества
        if (uiState.value.temporaryProductId != null) {
            prepareForQuantityInput()
        }
    }

    private fun processProductInput(productQuery: String) {
        Timber.d("Processing product input: $productQuery")

        launchIO {
            try {
                // Пытаемся найти товар по штрихкоду
                var product = productUseCases.findProductByBarcode(productQuery)

                // Если не нашли по штрихкоду, пробуем найти по ID
                if (product == null) {
                    product = productUseCases.getProductById(productQuery)
                }

                if (product != null) {
                    Timber.d("Product found: ${product.id}, ${product.name}")

                    // Обновляем состояние
                    updateState { state ->
                        state.copy(
                            temporaryProductId = product.id,
                            temporaryProduct = product,
                            currentScanHint = if (state.temporaryBinCode != null)
                                "Введите количество" else "Отсканируйте или введите ячейку"
                        )
                    }

                    // Оповещаем пользователя
                    sendEvent(TaskDetailEvent.ShowSnackbar("Товар принят: ${product.name}"))
                    soundService.playSuccessSound()

                    // Если ячейка уже введена, переходим к вводу количества
                    if (uiState.value.temporaryBinCode != null) {
                        prepareForQuantityInput()
                    }
                } else {
                    Timber.d("Product not found: $productQuery")
                    sendEvent(TaskDetailEvent.ShowSnackbar("Товар не найден: $productQuery"))
                    soundService.playErrorSound()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing product input")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
            }
        }
    }

    companion object {
        private const val MIN_BARCODE_LENGTH = 8 // Минимальная длина штрихкода для автоматического поиска
    }

    fun startTask() {
        val task = uiState.value.task ?: return

        launchIO {
            updateState { it.copy(isProcessing = true) }

            try {
                // Получаем текущего пользователя
                val currentUser = userUseCases.getCurrentUser().first()

                if (currentUser != null) {
                    val result = taskUseCases.startTask(task.id, currentUser.id)

                    if (result.isSuccess) {
                        loadTask()
                        initFactLineInput()
                        sendEvent(TaskDetailEvent.ShowSnackbar("Задание успешно начато"))
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                        sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: $error"))
                    }
                } else {
                    sendEvent(TaskDetailEvent.ShowSnackbar("Необходимо авторизоваться"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting task")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
            } finally {
                updateState { it.copy(isProcessing = false) }
            }
        }
    }

    fun showCompleteConfirmation() {
        updateState { it.copy(isCompleteConfirmationVisible = true) }
    }

    fun completeTask() {
        val task = uiState.value.task ?: return

        launchIO {
            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskUseCases.completeTask(task.id)

                if (result.isSuccess) {
                    loadTask()
                    sendEvent(TaskDetailEvent.ShowSnackbar("Задание успешно завершено"))
                    sendEvent(TaskDetailEvent.CloseDialog)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: $error"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error completing task")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
            } finally {
                updateState {
                    it.copy(
                        isProcessing = false,
                        isCompleteConfirmationVisible = false
                    )
                }
            }
        }
    }

    // в com.synngate.synnframe.presentation.ui.tasks.TaskDetailViewModel
    fun uploadTask() {
        val task = uiState.value.task ?: return

        launchIO {
            updateState { it.copy(isProcessing = true) }

            try {
                val result = taskUseCases.uploadTask(task.id)

                if (result.isSuccess) {
                    loadTask()
                    sendEvent(TaskDetailEvent.ShowSnackbar("Задание успешно выгружено"))
                } else {
                    val error = result.exceptionOrNull()
                    val errorMsg = error?.message ?: "Неизвестная ошибка"

                    // Логируем более подробную информацию, включая стек вызовов
                    Timber.e(error, "Detailed error during task upload")

                    // Показываем пользователю сокращенное, но более информативное сообщение
                    val userErrorMsg = when {
                        errorMsg.contains("Connection refused") -> "Ошибка соединения с сервером"
                        errorMsg.contains("timeout") -> "Превышено время ожидания ответа от сервера"
                        errorMsg.contains("404") -> "Сервер не нашел указанный ресурс (404)"
                        errorMsg.contains("401") || errorMsg.contains("403") -> "Ошибка авторизации (401/403)"
                        errorMsg.contains("500") -> "Внутренняя ошибка сервера (500)"
                        else -> "Ошибка выгрузки: $errorMsg"
                    }

                    sendEvent(TaskDetailEvent.ShowSnackbar(userErrorMsg))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error uploading task")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка выгрузки: ${e.message}"))
            } finally {
                updateState { it.copy(isProcessing = false) }
            }
        }
    }

    fun showDeleteConfirmation() {
        updateState { it.copy(showDeleteConfirmation = true) }
        sendEvent(TaskDetailEvent.ShowDeleteConfirmation)
    }

    fun hideDeleteConfirmation() {
        updateState { it.copy(showDeleteConfirmation = false) }
        sendEvent(TaskDetailEvent.HideDeleteConfirmation)
    }

    fun deleteTask() {
        launchIO {
            updateState { it.copy(isDeleting = true) }

            val result = taskUseCases.deleteTask(taskId)

            if (result.isSuccess) {
                sendEvent(TaskDetailEvent.ShowSnackbar("Задание успешно удалено"))
                // Переходим назад к списку заданий
                sendEvent(TaskDetailEvent.NavigateBack)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка удаления: $error"))
                updateState { it.copy(isDeleting = false, showDeleteConfirmation = false) }
            }
        }
    }

    // Используем существующий метод uploadTask с контекстом повторной выгрузки
    fun reuploadTask() {
        launchIO {
            updateState { it.copy(isReuploading = true) }

            // Используем существующий метод uploadTask
            val result = taskUseCases.uploadTask(taskId)

            if (result.isSuccess) {
                loadTask() // Перезагружаем детали задания для обновления UI
                sendEvent(TaskDetailEvent.ShowSnackbar("Задание успешно выгружено повторно"))
            } else {
                val error = result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка выгрузки: $error"))
            }

            updateState { it.copy(isReuploading = false) }
        }
    }

    /**
     * Обработка выбора товара из списка
     */
    fun handleSelectedProduct(product: Product) {
        Timber.d("Product selected from list: ${product.id}, name: ${product.name}")
        handleProductInput(product)
    }

    /**
     * Метод для ручного открытия диалога строки факта для существующей строки
     */
    fun showFactLineDialog(productId: String) {
        val state = uiState.value
        val task = state.task ?: return

        // Сохраняем ID товара в состоянии
        updateState { it.copy(temporaryProductId = productId) }

        // Находим товар по ID
        launchIO {
            try {
                val product = productUseCases.getProductById(productId)
                if (product != null) {
                    updateState { it.copy(temporaryProduct = product) }

                    // Находим строку факта или создаем новую
                    val factLine = task.factLines.find { it.productId == productId }

                    if (factLine != null) {
                        // Если строка факта существует, используем ее код ячейки в состоянии
                        updateState { it.copy(temporaryBinCode = factLine.binCode) }

                        if (factLine.binCode != null) {
                            val formattedBin = binFormatter?.formatBinName(factLine.binCode) ?: factLine.binCode
                            updateState { it.copy(formattedBinName = formattedBin) }
                        }
                    }

                    // Открываем диалог ввода количества
                    prepareForQuantityInput()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading product by ID")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка загрузки товара: ${e.message}"))
            }
        }
    }

    /**
     * Обрабатывает выбранный товар по ID со страницы выбора товаров
     */
    fun handleSelectedProductById(productId: String) {
        launchIO {
            try {
                // Загружаем продукт по ID
                val product = productUseCases.getProductById(productId)

                if (product != null) {
                    // Обрабатываем найденный продукт
                    handleSelectedProduct(product)
                } else {
                    // Если продукт не найден, показываем сообщение
                    sendEvent(TaskDetailEvent.ShowSnackbar("Товар с ID $productId не найден"))

                    // Воспроизводим звук ошибки
                    soundService.playErrorSound()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading product by ID $productId")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка загрузки товара: ${e.message}"))

                // Воспроизводим звук ошибки
                soundService.playErrorSound()
            }
        }
    }

    /**
     * Очищает состояние диалога строки факта
     */
    fun resetFactLineDialogState() {
        updateState { it.copy(
            factLineDialogState = FactLineDialogState()
        ) }
    }

    /**
     * Выполняет пакетное обновление строк факта
     */
    fun batchUpdateFactLines(updates: List<Pair<TaskFactLine, Float>>) {
        if (updates.isEmpty()) return

        launchIO {
            updateState { it.copy(isProcessing = true) }

            try {
                // Обновляем все строки факта одной транзакцией
                updates.forEach { (factLine, newQuantity) ->
                    val updatedFactLine = factLine.copy(quantity = newQuantity)
                    taskUseCases.updateTaskFactLine(updatedFactLine)
                }

                // Перезагружаем задание после всех обновлений
                loadTask()

                sendEvent(TaskDetailEvent.UpdateSuccess)
            } catch (e: Exception) {
                Timber.e(e, "Error batch updating task fact lines")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка обновления: ${e.message}"))
            } finally {
                updateState { it.copy(isProcessing = false) }
            }
        }
    }

    // В TaskDetailViewModel добавить метод для обработки пакетного сканирования
    // Затем можно добавить кнопку "Пакетное сканирование" в TaskDetailScreen и запускать этот процесс.
    fun processBatchScanResults(results: List<ScanResult>) {
        if (results.isEmpty()) return

        // Формируем список обновлений для строк факта
        val updates = mutableListOf<Pair<TaskFactLine, Float>>()

        results.forEach { result ->
            if (result.product != null) {
                val task = uiState.value.task ?: return
                val isInPlan = task.isProductInPlan(result.product.id)

                if (isInPlan) {
                    // Находим или создаем строку факта
                    val factLine = task.getFactLineByProductId(result.product.id)
                        ?: TaskFactLine(
                            id = UUID.randomUUID().toString(),
                            taskId = task.id,
                            productId = result.product.id,
                            quantity = 0f
                        )

                    // Добавляем к существующему количеству 1 (или другое значение)
                    val newQuantity = factLine.quantity + 1f
                    updates.add(Pair(factLine, newQuantity))
                }
            }
        }

        // Применяем все обновления за одну операцию
        if (updates.isNotEmpty()) {
            batchUpdateFactLines(updates)
        }
    }

    /**
     * Обновляет значение дополнительного количества в диалоге сканирования
     */
    fun updateScanDialogAdditionalQuantity(value: String) {
        updateState { it.copy(
            scanBarcodeDialogState = it.scanBarcodeDialogState.copy(
                additionalQuantity = value,
                isError = false
            )
        ) }
    }

    /**
     * Устанавливает состояние ошибки ввода количества в диалоге сканирования
     */
    fun setScanDialogInputError(isError: Boolean) {
        updateState { it.copy(
            scanBarcodeDialogState = it.scanBarcodeDialogState.copy(isError = isError)
        ) }
    }

    /**
     * Обновляет последний отсканированный штрихкод
     */
    fun updateLastScannedBarcode(barcode: String?) {
        updateState { it.copy(
            scanBarcodeDialogState = it.scanBarcodeDialogState.copy(lastScannedBarcode = barcode)
        ) }
    }

    /**
     * Сбрасывает состояние диалога сканирования
     */
    fun resetScanBarcodeDialogState() {
        updateState { it.copy(
            scanBarcodeDialogState = ScanBarcodeDialogState()
        ) }
    }

    fun navigateToProductList() {
        sendEvent(TaskDetailEvent.NavigateToProductsList)
    }

    /**
     * Возвращается на предыдущий экран
     */
    fun navigateBack() {
        sendEvent(TaskDetailEvent.NavigateBack)
    }

    /**
     * Обработка введенного товара (из любого источника)
     */
    fun handleProductInput(product: Product) {
        // Сохраняем введенный товар в состоянии
        updateState { it.copy(
            temporaryProductId = product.id,
            temporaryProduct = product,
            isValidProduct = true,
            searchQuery = "" // Очищаем поле ввода
        ) }

        // Вместо установки scanningState переходим к следующему действию
        // Это автоматически обновит подсказку из следующего действия
        moveToNextFactLineAction()

        // Воспроизводим звук успешного ввода
        soundService.playSuccessSound()
    }

    /**
     * Подготовка к вводу количества, когда есть и товар и ячейка
     */
    private fun prepareForQuantityInput() {
        val state = uiState.value

        if (state.temporaryProductId == null) {
            Timber.d("Cannot prepare for quantity input: missing product ID")
            return
        }

        val task = state.task ?: return

        Timber.d("Preparing for quantity input: product=${state.temporaryProductId}, bin=${state.temporaryBinCode}")

        // Находим или создаем строку факта
        val factLine = task.factLines.find { it.productId == state.temporaryProductId }
            ?: TaskFactLine(
                id = UUID.randomUUID().toString(),
                taskId = task.id,
                productId = state.temporaryProductId,
                quantity = 0f,
                binCode = state.temporaryBinCode  // Важно передать код ячейки
            )

        // Находим плановое количество
        val planLine = task.planLines.find { it.productId == state.temporaryProductId }
        val planQuantity = planLine?.quantity ?: 0f

        Timber.d("Opening quantity dialog with factLine=$factLine, binCode=${state.temporaryBinCode}")

        // Открываем диалог ввода количества
        updateState {
            it.copy(
                selectedFactLine = factLine,
                isFactLineDialogVisible = true,
                factLineDialogState = FactLineDialogState(),
                selectedPlanQuantity = planQuantity
            )
        }

        sendEvent(TaskDetailEvent.ShowFactLineDialog(factLine))
    }

    // Обработка штрихкода товара
    private fun processProductBarcode(barcode: String, actionType: FactLineActionType) {
        launchIO {
            updateState { it.copy(isProcessing = true) }

            try {
                // Ищем товар по штрихкоду
                val product = productUseCases.findProductByBarcode(barcode)

                if (product != null) {
                    val task = uiState.value.task

                    if (task != null) {
                        // Проверяем, соответствует ли товар требованиям действия
                        val isInPlan = task.isProductInPlan(product.id)
                        val isValid = when (actionType) {
                            FactLineActionType.ENTER_PRODUCT_ANY -> true
                            FactLineActionType.ENTER_PRODUCT_FROM_PLAN -> isInPlan
                            else -> false
                        }

                        if (isValid) {
                            // Товар подходит, сохраняем и переходим к следующему действию
                            updateState { it.copy(
                                temporaryProductId = product.id,
                                temporaryProduct = product,
                                isValidProduct = true,
                                searchQuery = "" // Важное исправление: очищаем поле ввода
                            ) }

                            moveToNextFactLineAction()
                            soundService.playSuccessSound()
                        } else {
                            // Товар не соответствует требованиям
                            updateState { it.copy(
                                temporaryProductId = product.id,
                                temporaryProduct = product,
                                isValidProduct = false,
                                searchQuery = "" // Важное исправление: очищаем поле ввода
                            ) }

                            sendEvent(TaskDetailEvent.ShowSnackbar("Товар не входит в план задания"))
                            soundService.playErrorSound()
                        }
                    }
                } else {
                    // Товар не найден
                    sendEvent(TaskDetailEvent.ShowSnackbar("Товар не найден"))
                    soundService.playErrorSound()
                }

                clearSearchQuery()
            } catch (e: Exception) {
                Timber.e(e, "Error processing product barcode")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
            } finally {
                updateState { it.copy(isProcessing = false) }
            }
        }
    }

    // Обработка штрихкода ячейки
    private fun processBinBarcode(barcode: String, actionType: FactLineActionType) {
        launchIO {
            updateState { it.copy(isProcessing = true) }

            try {
                // Проверяем соответствие штрихкода шаблону ячейки
                val isValidBin = binValidator?.isValidBin(barcode) ?: false

                if (isValidBin) {
                    // Форматируем имя ячейки для отображения
                    val formattedBin = binFormatter?.formatBinName(barcode) ?: barcode

                    // Проверяем, соответствует ли ячейка требованиям действия
                    val task = uiState.value.task
                    val temporaryProductId = uiState.value.temporaryProductId

                    var isValid = true

                    if (task != null && temporaryProductId != null &&
                        actionType == FactLineActionType.ENTER_BIN_FROM_PLAN) {
                        // Если требуется ячейка из плана, проверяем соответствие
                        val planLine = task.planLines.find { it.productId == temporaryProductId }

                        if (planLine?.binCode != null && planLine.binCode != barcode) {
                            isValid = false
                        }
                    }

                    if (isValid) {
                        // Ячейка подходит, сохраняем и переходим к следующему действию
                        updateState { it.copy(
                            temporaryBinCode = barcode,
                            formattedBinName = formattedBin,
                            isValidBin = true,
                            searchQuery = "" // Важное исправление: очищаем поле ввода
                        ) }

                        moveToNextFactLineAction()
                        soundService.playSuccessSound()
                    } else {
                        // Ячейка не соответствует требованиям
                        updateState { it.copy(
                            temporaryBinCode = barcode,
                            formattedBinName = formattedBin,
                            isValidBin = false,
                            searchQuery = "" // Важное исправление: очищаем поле ввода
                        ) }

                        sendEvent(TaskDetailEvent.ShowSnackbar("Ячейка не соответствует плану"))
                        soundService.playErrorSound()
                    }
                } else {
                    // Штрихкод не соответствует формату ячейки
                    sendEvent(TaskDetailEvent.ShowSnackbar("Неверный формат ячейки"))
                    soundService.playErrorSound()
                }

                clearSearchQuery()
            } catch (e: Exception) {
                Timber.e(e, "Error processing bin barcode")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
            } finally {
                updateState { it.copy(isProcessing = false) }
            }
        }
    }

    private fun showQuantityInputDialog(productId: String) {
        val state = uiState.value
        val task = state.task ?: return

        // Получаем информацию о штрихкоде ячейки и временном товаре
        val binCode = state.temporaryBinCode
        val temporaryProduct = state.temporaryProduct

        Timber.d("Showing quantity dialog: productId=$productId, binCode=$binCode, product=$temporaryProduct")

        // Находим строку факта для указанного товара или создаем новую с учетом кода ячейки
        val factLine = task.factLines.find { it.productId == productId }
            ?: TaskFactLine(
                id = UUID.randomUUID().toString(),
                taskId = task.id,
                productId = productId,
                quantity = 0f,
                binCode = binCode // Важно: здесь передаем код ячейки
            )

        // Находим плановое количество для этого товара
        val planLine = task.planLines.find { it.productId == productId }
        val planQuantity = planLine?.quantity ?: 0f

        Timber.d("Created fact line: $factLine with binCode=$binCode")

        // Обновляем состояние и показываем диалог
        updateState {
            it.copy(
                selectedFactLine = factLine,
                isFactLineDialogVisible = true,
                factLineDialogState = FactLineDialogState(),
                selectedPlanQuantity = planQuantity
            )
        }

        sendEvent(TaskDetailEvent.ShowFactLineDialog(factLine))
    }

    // Завершение ввода строки факта и сохранение
// Оставить только этот метод, удалив completeFactLine()
    fun completeFactLineInput() {
        val state = uiState.value
        val productId = state.temporaryProductId
        val quantity = state.temporaryQuantity
        val binCode = state.temporaryBinCode

        if (productId != null && quantity != null && quantity > 0) {
            // Создаем итоговую строку факта
            val taskId = state.task?.id ?: ""
            val factLineId = UUID.randomUUID().toString()

            val factLine = TaskFactLine(
                id = factLineId,
                taskId = taskId,
                productId = productId,
                quantity = quantity,
                binCode = binCode
            )

            // Сохраняем строку факта
            launchIO {
                try {
                    taskUseCases.updateTaskFactLine(factLine)

                    // Обновляем задание
                    loadTask()

                    // Сбрасываем состояние ввода
                    resetFactLineInputState()

                    sendEvent(TaskDetailEvent.UpdateSuccess)
                    soundService.playSuccessSound()
                } catch (e: Exception) {
                    Timber.e(e, "Error saving fact line")
                    sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка сохранения: ${e.message}"))
                    soundService.playErrorSound()
                }
            }
        } else {
            // Сообщаем об ошибке, если данные неполные
            sendEvent(TaskDetailEvent.ShowSnackbar("Неполные данные для сохранения"))
            soundService.playErrorSound()
        }
    }

    // Сброс состояния ввода строки факта
    fun resetFactLineInputState() {
        updateState { it.copy(
            factLineActionIndex = 0,
            factLineActions = emptyList(),
            currentFactLineAction = null,
            temporaryProductId = null,
            temporaryProduct = null,
            temporaryBinCode = null,
            temporaryQuantity = null,
            isValidProduct = true,
            isValidBin = true
        ) }
    }

    /**
     * Показывает диалог сканирования штрихкодов
     */
    fun showScanDialog() {
        // Сбрасываем состояние диалога сканирования
        updateState { it.copy(
            isScanDialogVisible = true,
            scanBarcodeDialogState = ScanBarcodeDialogState(),
            scannedBarcode = null
        ) }
        sendEvent(TaskDetailEvent.ShowScanDialog)
    }

    /**
     * Закрывает все диалоги
     */
    fun closeDialog() {
        updateState {
            it.copy(
                isScanDialogVisible = false,
                isFactLineDialogVisible = false,
                isCompleteConfirmationVisible = false,
                selectedFactLine = null,
                factLineDialogState = FactLineDialogState()
            )
        }

        sendEvent(TaskDetailEvent.CloseDialog)
    }

    /**
     * Обновляет сообщение диалога сканирования
     */
    fun updateScannerMessage(message: String?) {
        updateState { it.copy(
            scanBarcodeDialogState = it.scanBarcodeDialogState.copy(
                scannerMessage = message
            )
        ) }
    }

    /**
     * Обновляет дополнительное количество в диалоге ввода количества
     */
    fun updateFactLineAdditionalQuantity(value: String) {
        updateState { it.copy(
            factLineDialogState = it.factLineDialogState.copy(
                additionalQuantity = value,
                isError = false
            )
        ) }
    }

    /**
     * Устанавливает состояние ошибки ввода количества
     */
    fun setFactLineInputError(isError: Boolean) {
        updateState { it.copy(
            factLineDialogState = it.factLineDialogState.copy(isError = isError)
        ) }
    }

    fun applyQuantityChange(factLine: TaskFactLine, additionalQuantity: String) {
        try {
            val addValue = additionalQuantity.toFloatOrNull() ?: 0f
            if (addValue == 0f) {
                return
            }

            val updatedQuantity = factLine.quantity + addValue

            // Обновление состояния для завершения ввода
            updateState { it.copy(
                temporaryQuantity = updatedQuantity,
                // Очистить поле ввода
                searchQuery = ""
            )}

            // Закрыть диалог
            closeDialog()

            // Если все данные собраны, завершить ввод
            if (uiState.value.temporaryProductId != null &&
                uiState.value.temporaryQuantity != null) {
                completeFactLineInput()
            } else {
                // Иначе продолжить с текущим действием
                moveToNextFactLineAction()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing quantity")
            sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка ввода количества"))
        }
    }

    /**
     * Активирует/деактивирует сканер в диалоге
     */
    fun toggleScannerActive(active: Boolean) {
        updateState { it.copy(
            scanBarcodeDialogState = it.scanBarcodeDialogState.copy(isScannerActive = active)
        ) }
    }

    fun clearSearchQuery() {
        updateState { it.copy(searchQuery = "") }
    }

    fun canCompleteFactLineInput(state: TaskDetailState): Boolean {
        return state.temporaryProductId != null &&
                state.temporaryQuantity != null &&
                state.temporaryQuantity > 0f
    }
}