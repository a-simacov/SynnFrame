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
import com.synngate.synnframe.presentation.ui.tasks.model.ScanningState
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
                        initScanningCycle()
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

    // Инициализация цикла ввода
    fun initScanningCycle() {
        launchIO {
            try {
                // Определяем начальный шаг в зависимости от настроек
                val scanOrder = settingsUseCases.scanOrder.first()
                val initialState = if (scanOrder == ScanOrder.PRODUCT_FIRST) {
                    ScanningState.SCAN_PRODUCT
                } else {
                    ScanningState.SCAN_BIN
                }

                // Устанавливаем начальное состояние
                updateState { it.copy(
                    scanningState = initialState,
                    currentScanHint = getHintForState(initialState),
                    // Сбрасываем временные значения
                    temporaryProductId = null,
                    temporaryProduct = null,
                    temporaryBinCode = null,
                    formattedBinName = null,
                    temporaryQuantity = null
                ) }

                // Также используем подсказки из типа задания, если они доступны
                val taskType = uiState.value.task?.taskType
                if (taskType != null && taskType.factLineActions.isNotEmpty()) {
                    val firstAction = taskType.factLineActions.minByOrNull { it.order }
                    if (firstAction != null) {
                        // Используем подсказку из первого действия типа задания
                        updateState { it.copy(
                            currentScanHint = firstAction.promptText
                        ) }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error initializing scanning cycle")
            }
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

                updateState { it.copy(
                    factLineActions = sortedActions,
                    factLineActionIndex = 0,
                    currentFactLineAction = sortedActions.firstOrNull(),
                    temporaryProductId = null,
                    temporaryProduct = null,
                    temporaryBinCode = null,
                    temporaryQuantity = null
                ) }

                // Обновляем подсказку для текущего действия
                updateScanHint()
            } else {
                // Если тип задания не найден, показываем сообщение об ошибке
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
            updateState { it.copy(
                factLineActionIndex = nextIndex,
                currentFactLineAction = state.factLineActions[nextIndex]
            ) }

            // Обновляем подсказку
            updateScanHint()
        } else {
            // Все действия выполнены, сохраняем строку факта
            completeFactLineInput()
        }
    }

    // Обработка сканирования для текущего действия
    fun processScanResultForCurrentAction(barcode: String) {
        val action = uiState.value.currentFactLineAction ?: return

        when (action.type) {
            FactLineActionType.ENTER_PRODUCT_ANY,
            FactLineActionType.ENTER_PRODUCT_FROM_PLAN -> processProductBarcode(barcode)

            FactLineActionType.ENTER_BIN_ANY,
            FactLineActionType.ENTER_BIN_FROM_PLAN -> processBinBarcode(barcode)

            FactLineActionType.ENTER_QUANTITY -> {
                // Обычно количество вводится не через сканер, а через диалог
                // Здесь обработка если все же пришел штрихкод
                try {
                    val quantity = barcode.toFloatOrNull()
                    if (quantity != null && quantity > 0) {
                        updateState { it.copy(temporaryQuantity = quantity) }
                        moveToNextFactLineAction()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error parsing quantity from barcode")
                }
            }
        }
    }

    // Получение подсказки для текущего состояния
    private fun getHintForState(state: ScanningState): String {
        return when (state) {
            ScanningState.SCAN_BIN -> "Отсканируйте или введите ячейку"
            ScanningState.SCAN_PRODUCT -> "Отсканируйте или введите товар"
            ScanningState.ENTER_QUANTITY -> "Введите количество"
            ScanningState.CONFIRM -> "Подтвердите введенные данные"
            ScanningState.IDLE -> "Готов к сканированию"
        }
    }

    // Сброс цикла ввода
    fun resetScanningCycle() {
        updateState { it.copy(
            scanningState = ScanningState.IDLE,
            currentScanHint = "",
            temporaryProductId = null,
            temporaryProduct = null,
            temporaryBinCode = null,
            formattedBinName = null,
            temporaryQuantity = null
        ) }
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

//        // Если введено достаточно символов для поиска, обрабатываем как штрихкод
//        if (query.length >= MIN_BARCODE_LENGTH) {
//            processScanResult(query)
//        }
    }

    fun onSearchEnterPressed() {
        val query = uiState.value.searchQuery.trim()
        if (query.isEmpty()) return

        Timber.d("Search enter pressed with query: $query")

        // Очищаем поле ввода
        updateState { it.copy(searchQuery = "") }

        // Обрабатываем в зависимости от текущего состояния
        when (uiState.value.scanningState) {
            ScanningState.SCAN_BIN -> {
                // Обрабатываем как ввод ячейки
                Timber.d("Processing as bin input: $query")
                processBinInput(query)
            }
            ScanningState.SCAN_PRODUCT -> {
                // Пытаемся найти товар по этому значению
                Timber.d("Processing as product input: $query")
                processProductInput(query)
            }
            else -> {
                // В других состояниях пробуем универсальную обработку
                Timber.d("Processing as generic input: $query")
                processScanResult(query)
            }
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
                // Переходим к вводу товара
                scanningState = ScanningState.SCAN_PRODUCT,
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
                            // Переходим к вводу ячейки или количества
                            scanningState = if (state.temporaryBinCode != null)
                                ScanningState.ENTER_QUANTITY else ScanningState.SCAN_BIN,
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
     * Обрабатывает результат сканирования штрихкода
     */
// Обработка результата сканирования или ручного ввода
    /**
     * Универсальный метод обработки ввода штрихкода - через сканер или ручной ввод.
     * Определяет тип данных (товар или ячейка) и делегирует обработку соответствующему методу.
     */
    fun processScanResult(barcode: String) {
        if (barcode.isEmpty()) return

        Timber.d("Processing input: barcode=$barcode, currentState=${uiState.value.scanningState}")

        // Сохраняем для диагностики
        updateState { it.copy(scannedBarcode = barcode) }

        launchIO {
            try {
                // Проверяем, является ли штрихкод ячейкой
                val isValidBin = binValidator?.isValidBin(barcode) ?: false

                // Пытаемся найти товар по штрихкоду
                val product = productUseCases.findProductByBarcode(barcode)

                when {
                    // Если это ячейка и мы ожидаем ввод ячейки или находимся в нейтральном состоянии
                    isValidBin && (uiState.value.scanningState == ScanningState.SCAN_BIN ||
                            uiState.value.scanningState == ScanningState.IDLE) -> {
                        handleBinInput(barcode)
                    }

                    // Если это товар и мы ожидаем ввод товара или находимся в нейтральном состоянии
                    product != null && (uiState.value.scanningState == ScanningState.SCAN_PRODUCT ||
                            uiState.value.scanningState == ScanningState.IDLE) -> {
                        handleProductInput(product)
                    }

                    // Если это ячейка, но мы ожидаем товар - предложим сменить последовательность
                    isValidBin && uiState.value.scanningState == ScanningState.SCAN_PRODUCT -> {
                        Timber.d("Bin scanned but product expected, switching sequence")
                        handleBinInput(barcode)
                        sendEvent(TaskDetailEvent.ShowSnackbar("Ячейка распознана. Теперь отсканируйте товар"))
                    }

                    // Если это товар, но мы ожидаем ячейку - предложим сменить последовательность
                    product != null && uiState.value.scanningState == ScanningState.SCAN_BIN -> {
                        Timber.d("Product scanned but bin expected, switching sequence")
                        handleProductInput(product)
                        sendEvent(TaskDetailEvent.ShowSnackbar("Товар распознан. Теперь отсканируйте ячейку"))
                    }

                    // Не смогли распознать ни как товар, ни как ячейку
                    else -> {
                        Timber.d("Could not recognize input: $barcode")
                        soundService.playErrorSound()
                        sendEvent(TaskDetailEvent.ShowSnackbar("Не удалось распознать: $barcode"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing input")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка обработки ввода: ${e.message}"))
            }
        }
    }

    /**
     * Обработка введенной ячейки (из любого источника)
     */
    private fun handleBinInput(binCode: String) {
        // Форматируем имя ячейки для отображения
        val formattedBin = binFormatter?.formatBinName(binCode) ?: binCode

        Timber.d("Handling bin input: $binCode, formatted: $formattedBin")

        // Сохраняем в состоянии и переходим к следующему шагу
        updateState { state ->
            state.copy(
                temporaryBinCode = binCode,
                formattedBinName = formattedBin,
                // После ввода ячейки всегда переходим к вводу товара
                scanningState = ScanningState.SCAN_PRODUCT,
                // Обновляем текст подсказки для следующего шага
                currentScanHint = getHintForState(ScanningState.SCAN_PRODUCT)
            )
        }

        soundService.playSuccessSound()

        // Показываем подсказку о следующем шаге
        sendEvent(TaskDetailEvent.ShowSnackbar("Ячейка ${formattedBin} принята. Отсканируйте товар"))

        // Если уже есть товар, переходим к вводу количества
        if (uiState.value.temporaryProductId != null) {
            prepareForQuantityInput()
        }
    }

    /**
     * Обработка введенного товара (из любого источника)
     */
    private fun handleProductInput(product: Product) {
        Timber.d("Handling product input: ${product.id}, name: ${product.name}")

        // Сохраняем в состоянии
        updateState { state ->
            state.copy(
                temporaryProductId = product.id,
                temporaryProduct = product,
                // После ввода товара определяем следующий шаг
                scanningState = if (state.temporaryBinCode != null) {
                    ScanningState.ENTER_QUANTITY
                } else {
                    ScanningState.SCAN_BIN
                },
                // Обновляем текст подсказки
                currentScanHint = getHintForState(
                    if (state.temporaryBinCode != null) ScanningState.ENTER_QUANTITY
                    else ScanningState.SCAN_BIN
                )
            )
        }

        soundService.playSuccessSound()

        // Показываем подсказку о следующем шаге
        if (uiState.value.temporaryBinCode == null) {
            sendEvent(TaskDetailEvent.ShowSnackbar("Товар ${product.name} принят. Отсканируйте ячейку"))
        } else {
            // Если уже есть ячейка, переходим к вводу количества
            prepareForQuantityInput()
        }
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
                selectedPlanQuantity = planQuantity,
                scanningState = ScanningState.ENTER_QUANTITY
            )
        }

        sendEvent(TaskDetailEvent.ShowFactLineDialog(factLine))
    }

    // Обработка штрихкода товара
    private fun processProductBarcode(barcode: String) {
        launchIO {
            try {
                // Ищем товар по штрихкоду
                val product = productUseCases.findProductByBarcode(barcode)

                if (product != null) {
                    Timber.d("Product found by barcode: ${product.id}, name: ${product.name}")

                    // Сохраняем товар в состоянии
                    updateState { state ->
                        state.copy(
                            temporaryProductId = product.id,
                            temporaryProduct = product,
                            isValidProduct = true,
                            // Если ячейка ещё не отсканирована, переходим к её сканированию
                            scanningState = if (state.temporaryBinCode == null)
                                ScanningState.SCAN_BIN else ScanningState.ENTER_QUANTITY
                        )
                    }

                    soundService.playSuccessSound()

                    // Если товар найден и мы уже имеем ячейку, показываем диалог ввода количества
                    if (uiState.value.temporaryBinCode != null) {
                        showQuantityInputDialog(product.id)
                    } else {
                        sendEvent(TaskDetailEvent.ShowSnackbar("Отсканируйте ячейку"))
                    }
                } else {
                    Timber.d("Product not found by barcode: $barcode")
                    soundService.playErrorSound()
                    sendEvent(TaskDetailEvent.ShowSnackbar("Товар не найден по штрихкоду: $barcode"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing product barcode")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка обработки товара: ${e.message}"))
            }
        }
    }

    // Обработка штрихкода ячейки
    private fun processBinBarcode(barcode: String) {
        launchIO {
            try {
                // Проверяем соответствие штрихкода шаблону ячейки
                val isValidBin = binValidator?.isValidBin(barcode) ?: true // По умолчанию принимаем любой

                if (isValidBin) {
                    // Форматируем имя ячейки для отображения
                    val formattedBin = binFormatter?.formatBinName(barcode) ?: barcode

                    Timber.d("Valid bin code scanned: $barcode, formatted: $formattedBin")

                    // Сохраняем значения в состоянии
                    updateState { it.copy(
                        temporaryBinCode = barcode,
                        formattedBinName = formattedBin,
                        isValidBin = true,
                        scanningState = ScanningState.SCAN_PRODUCT // Переходим к сканированию продукта
                    ) }

                    soundService.playSuccessSound()
                    sendEvent(TaskDetailEvent.ShowSnackbar("Ячейка распознана: $formattedBin"))
                } else {
                    Timber.d("Invalid bin format: $barcode")
                    soundService.playErrorSound()
                    sendEvent(TaskDetailEvent.ShowSnackbar("Неверный формат ячейки"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing bin barcode")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка обработки ячейки: ${e.message}"))
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
                selectedPlanQuantity = planQuantity,
                scanningState = ScanningState.ENTER_QUANTITY
            )
        }

        sendEvent(TaskDetailEvent.ShowFactLineDialog(factLine))
    }

    // Завершение ввода строки факта и сохранение
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

    // Подготовка к вводу количества
    private fun prepareQuantityInput(product: Product) {
        // Подготавливаем строку факта для диалога
        val taskId = uiState.value.task?.id ?: ""
        val factLineId = UUID.randomUUID().toString()

        val newFactLine = TaskFactLine(
            id = factLineId,
            taskId = taskId,
            productId = product.id,
            quantity = 0f,
            binCode = uiState.value.temporaryBinCode
        )

        // Находим плановое количество
        val planQuantity = uiState.value.task?.planLines
            ?.find { it.productId == product.id }?.quantity ?: 0f

        // Показываем диалог ввода количества
        updateState { it.copy(
            selectedFactLine = newFactLine,
            isFactLineDialogVisible = true,
            selectedPlanQuantity = planQuantity,
            scanningState = ScanningState.ENTER_QUANTITY,
            currentScanHint = getHintForState(ScanningState.ENTER_QUANTITY)
        ) }

        soundService.playSuccessSound()
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

    /**
     * Применяет изменения количества в строке факта
     */
    // Этот метод вызывается из диалога ввода количества
    /**
     * Применение изменения количества из диалога
     */
    fun applyQuantityChange(factLine: TaskFactLine, additionalQuantity: String) {
        try {
            val addValue = additionalQuantity.toFloatOrNull() ?: 0f
            if (addValue == 0f) {
                return
            }

            val updatedQuantity = factLine.quantity + addValue
            val state = uiState.value

            // Используем код ячейки из строки факта или из состояния
            val binCode = factLine.binCode ?: state.temporaryBinCode

            Timber.d("Applying quantity: +$addValue = $updatedQuantity, using binCode=$binCode")

            val updatedFactLine = factLine.copy(
                quantity = updatedQuantity,
                binCode = binCode
            )

            // Локально обновляем UI
            updateLocalTaskLines(updatedFactLine)

            // Закрываем диалог
            closeDialog()

            // Сохраняем в БД
            launchIO {
                try {
                    Timber.d("Saving fact line to DB: $updatedFactLine with binCode=$binCode")
                    taskUseCases.updateTaskFactLine(updatedFactLine)

                    sendEvent(TaskDetailEvent.UpdateSuccess)
                    soundService.playSuccessSound()

                    initScanningCycle()
                } catch (e: Exception) {
                    Timber.e(e, "Error saving fact line: ${e.message}")
                    sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка сохранения: ${e.message}"))
                    soundService.playErrorSound()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing quantity")
            sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка обработки количества"))
        }
    }

    fun resetScanningState() {
        Timber.d("Resetting scanning state")

        // Получаем начальное состояние в зависимости от настроек
        launchIO {
            val scanOrder = try {
                settingsUseCases.scanOrder.first()
            } catch (e: Exception) {
                ScanOrder.PRODUCT_FIRST // По умолчанию
            }

            val initialState = if (scanOrder == ScanOrder.PRODUCT_FIRST) {
                ScanningState.SCAN_PRODUCT
            } else {
                ScanningState.SCAN_BIN
            }

            val initialHint = when(initialState) {
                ScanningState.SCAN_PRODUCT -> "Отсканируйте или введите товар"
                ScanningState.SCAN_BIN -> "Отсканируйте или введите ячейку"
                else -> ""
            }

            // Сбрасываем временные данные и устанавливаем начальное состояние
            updateState { it.copy(
                scanningState = initialState,
                currentScanHint = initialHint,
                temporaryProductId = null,
                temporaryProduct = null,
                temporaryBinCode = null,
                formattedBinName = null,
                temporaryQuantity = null,
                isValidProduct = true,
                isValidBin = true
            ) }

            sendEvent(TaskDetailEvent.ShowSnackbar("Ввод сброшен"))
        }
    }

    // Этот метод локально обновляет строки задания без перезагрузки
    /**
     * Локальное обновление строк задания без обращения к БД
     */
    private fun updateLocalTaskLines(updatedFactLine: TaskFactLine) {
        val state = uiState.value
        val task = state.task ?: return
        val taskLines = state.taskLines.toMutableList()

        // Форматируем имя ячейки, если есть
        val binName = if (updatedFactLine.binCode != null) {
            binFormatter?.formatBinName(updatedFactLine.binCode) ?: updatedFactLine.binCode
        } else null

        Timber.d("Updating local task line: factLine=$updatedFactLine, binName=$binName")

        val lineIndex = taskLines.indexOfFirst { it.planLine.productId == updatedFactLine.productId }

        if (lineIndex >= 0) {
            // Обновляем существующую строку
            val currentLine = taskLines[lineIndex]
            taskLines[lineIndex] = currentLine.copy(
                factLine = updatedFactLine,
                binName = binName
            )

            Timber.d("Updated existing line at index $lineIndex")
        } else if (task.allowProductsNotInPlan) {
            // Создаем новую строку для товара не из плана
            val product = state.temporaryProduct

            val dummyPlanLine = TaskPlanLine(
                id = "dummy-${updatedFactLine.id}",
                taskId = updatedFactLine.taskId,
                productId = updatedFactLine.productId,
                quantity = 0f
            )

            val newLine = TaskLineItem(
                planLine = dummyPlanLine,
                factLine = updatedFactLine,
                product = product,
                binName = binName
            )

            taskLines.add(newLine)
            Timber.d("Added new line for product not in plan")
        }

        // Обновляем состояние
        updateState { it.copy(taskLines = taskLines) }
    }

    // Завершение строки факта и сохранение
    fun completeFactLine() {
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

                    // Сбрасываем цикл ввода
                    resetScanningCycle()

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

    /**
     * Активирует/деактивирует сканер в диалоге
     */
    fun toggleScannerActive(active: Boolean) {
        updateState { it.copy(
            scanBarcodeDialogState = it.scanBarcodeDialogState.copy(isScannerActive = active)
        ) }
    }
}