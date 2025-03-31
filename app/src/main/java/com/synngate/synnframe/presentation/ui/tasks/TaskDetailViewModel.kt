package com.synngate.synnframe.presentation.ui.tasks

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskPlanLine
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.service.SoundService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.settings.SettingsUseCases
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
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
                            task = task,
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
        // Определяем начальное состояние в зависимости от настроек порядка
        val scanOrder = _scanOrder.value
        val initialState = if (scanOrder == ScanOrder.PRODUCT_FIRST) {
            ScanningState.SCAN_PRODUCT
        } else {
            ScanningState.SCAN_BIN
        }

        // Обновляем состояние
        updateState { it.copy(
            scanningState = initialState,
            currentScanHint = getHintForState(initialState),
            temporaryProductId = null,
            temporaryProduct = null,
            temporaryBinCode = null,
            formattedBinName = null,
            temporaryQuantity = null,
            isValidProduct = true,
            isValidBin = true
        ) }
    }

    // Получение подсказки для текущего состояния
    private fun getHintForState(state: ScanningState): String {
        return when(state) {
            ScanningState.SCAN_PRODUCT -> "Введите или отсканируйте штрихкод товара"
            ScanningState.SCAN_BIN -> "Введите или отсканируйте штрихкод ячейки"
            ScanningState.ENTER_QUANTITY -> "Введите количество товара"
            ScanningState.CONFIRM -> "Подтвердите введенные данные"
            else -> ""
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

        // Если введено достаточно символов для поиска, обрабатываем как штрихкод
        if (query.length >= MIN_BARCODE_LENGTH) {
            processScanResult(query)
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

    fun handleSelectedProduct(product: Product) {
        launchIO {
            val task = uiState.value.task ?: return@launchIO

            // Проверяем, есть ли товар в плане задания, или разрешено использовать товары не из плана
            val isInPlan = task.planLines.any { it.productId == product.id }
            val canUseProductNotInPlan = task.allowProductsNotInPlan

            if (isInPlan || canUseProductNotInPlan) {
                // Находим или создаем строку факта
                val factLine = task.factLines.find { it.productId == product.id }
                    ?: TaskFactLine(
                        id = UUID.randomUUID().toString(),
                        taskId = task.id,
                        productId = product.id,
                        quantity = 0f
                    )

                // Находим плановое количество для этого товара
                val planLine = task.planLines.find { it.productId == product.id }
                val planQuantity = planLine?.quantity ?: 0f

                // Обновляем состояние и открываем диалог ввода количества
                updateState { state ->
                    state.copy(
                        scannedProduct = product,
                        selectedFactLine = factLine,
                        isFactLineDialogVisible = true,
                        selectedPlanQuantity = planQuantity
                    )
                }

                // Воспроизводим звук успешного сканирования
                soundService.playSuccessSound()
            } else {
                // Если товара нет в плане и не разрешено использовать товары не из плана, показываем сообщение
                sendEvent(TaskDetailEvent.ShowSnackbar(
                    "Товар '${product.name}' не входит в план задания"
                ))

                // Воспроизводим звук ошибки
                soundService.playErrorSound()
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
    fun processScanResult(barcode: String) {
        if (barcode.isEmpty()) return

        // Определяем действие по текущему состоянию
        val currentState = uiState.value.scanningState

        when(currentState) {
            ScanningState.SCAN_PRODUCT -> processProductBarcode(barcode)
            ScanningState.SCAN_BIN -> processBinBarcode(barcode)
            ScanningState.IDLE -> {
                // Инициализация нового цикла, если еще не начат
                initScanningCycle()
                // Повторяем обработку с учетом начального состояния
                processScanResult(barcode)
            }
            else -> {
                // Для других состояний просто выводим информацию
                sendEvent(TaskDetailEvent.ShowSnackbar(
                    "Завершите текущий шаг: ${getHintForState(currentState)}"
                ))
            }
        }
    }

    // Обработка штрихкода товара
    private fun processProductBarcode(barcode: String) {
        launchIO {
            updateState { it.copy(isProcessing = true) }

            try {
                // Ищем товар по штрихкоду
                val product = productUseCases.findProductByBarcode(barcode)

                if (product != null) {
                    // Товар найден
                    val task = uiState.value.task

                    if (task != null) {
                        // Проверяем, есть ли товар в плане или разрешены товары не из плана
                        val isInPlan = task.isProductInPlan(product.id)
                        val canUseProductNotInPlan = task.allowProductsNotInPlan

                        if (isInPlan || canUseProductNotInPlan) {
                            // Сохраняем данные о товаре
                            updateState { it.copy(
                                temporaryProductId = product.id,
                                temporaryProduct = product,
                                isValidProduct = true
                            ) }

                            // Переходим к следующему шагу
                            prepareQuantityInput(product)
                        } else {
                            // Товар не в плане и не разрешен
                            updateState { it.copy(
                                temporaryProductId = product.id,
                                temporaryProduct = product,
                                isValidProduct = false
                            ) }

                            sendEvent(TaskDetailEvent.ShowSnackbar("Товар не входит в план задания"))
                            soundService.playErrorSound()
                        }
                    }
                } else {
                    // Проверяем, может это штрихкод ячейки
                    if (binValidator?.isValidBin(barcode) == true) {
                        // Если это ячейка, перенаправляем на обработку ячейки
                        processBinBarcode(barcode)
                    } else {
                        // Товар не найден и это не ячейка
                        sendEvent(TaskDetailEvent.ShowSnackbar("Товар не найден"))
                        soundService.playErrorSound()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing product barcode")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
            } finally {
                updateState { it.copy(isProcessing = false) }
            }
        }
    }

    // Обработка штрихкода ячейки
    private fun processBinBarcode(barcode: String) {
        launchIO {
            updateState { it.copy(isProcessing = true) }

            try {
                // Проверяем соответствие штрихкода шаблону ячейки
                val isValidBin = binValidator?.isValidBin(barcode) ?: false

                if (isValidBin) {
                    // Форматируем имя ячейки для отображения
                    val formattedBin = binFormatter?.formatBinName(barcode) ?: barcode

                    // Проверяем соответствие ячейки плану (если применимо)
                    val task = uiState.value.task
                    val tempProductId = uiState.value.temporaryProductId

                    var binMatchesPlan = true

                    if (task != null && tempProductId != null) {
                        // Если есть товар, проверяем соответствие ячейки плану
                        val planLine = task.planLines.find { it.productId == tempProductId }

                        if (planLine?.binCode != null && planLine.binCode != barcode) {
                            binMatchesPlan = false
                        }
                    }

                    // Сохраняем данные о ячейке
                    updateState { it.copy(
                        temporaryBinCode = barcode,
                        formattedBinName = formattedBin,
                        isValidBin = binMatchesPlan
                    ) }

                    // Определяем следующий шаг
                    if (_scanOrder.value == ScanOrder.BIN_FIRST) {
                        // Если сначала ячейка, переходим к вводу товара
                        updateState { it.copy(
                            scanningState = ScanningState.SCAN_PRODUCT,
                            currentScanHint = getHintForState(ScanningState.SCAN_PRODUCT)
                        ) }
                        soundService.playSuccessSound()
                    } else {
                        // Если сначала товар, проверяем наличие товара
                        if (uiState.value.temporaryProduct != null) {
                            // Если товар уже введен, переходим к подтверждению
                            updateState { it.copy(
                                scanningState = ScanningState.CONFIRM,
                                currentScanHint = getHintForState(ScanningState.CONFIRM)
                            ) }
                            soundService.playSuccessSound()
                        } else {
                            // Иначе сообщаем, что нужно сначала ввести товар
                            sendEvent(TaskDetailEvent.ShowSnackbar("Сначала введите товар"))
                            soundService.playErrorSound()
                        }
                    }
                } else {
                    // Если это не ячейка, проверяем не товар ли это
                    if (_scanOrder.value == ScanOrder.PRODUCT_FIRST) {
                        // Если сначала товар, пробуем обработать как товар
                        processProductBarcode(barcode)
                    } else {
                        // Если сначала ячейка, сообщаем об ошибке формата
                        sendEvent(TaskDetailEvent.ShowSnackbar("Неверный формат ячейки"))
                        soundService.playErrorSound()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing bin barcode")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
            } finally {
                updateState { it.copy(isProcessing = false) }
            }
        }
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
     * Показывает диалог ввода количества для указанного товара
     */
    fun showFactLineDialog(productId: String) {
        val state = uiState.value
        val task = state.task ?: return

        // Находим строку факта для указанного товара или создаем новую
        val factLine = task.factLines.find { it.productId == productId }
            ?: TaskFactLine(
                id = UUID.randomUUID().toString(),
                taskId = task.id,
                productId = productId,
                quantity = 0f
            )

        // Находим плановое количество для этого товара
        val planLine = task.planLines.find { it.productId == productId }
        val planQuantity = planLine?.quantity ?: 0f

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
    fun applyQuantityChange(factLine: TaskFactLine, additionalQuantity: String) {
        try {
            val addValue = additionalQuantity.toFloatOrNull() ?: 0f
            if (addValue == 0f) {
                return
            }

            val updatedQuantity = factLine.quantity + addValue

            // Сохраняем временное количество
            updateState { it.copy(
                temporaryQuantity = updatedQuantity
            ) }

            // Закрываем диалог
            closeDialog()

            // Определяем следующий шаг
            if (_scanOrder.value == ScanOrder.PRODUCT_FIRST) {
                if (uiState.value.temporaryBinCode == null) {
                    // Если сначала товар и ячейка еще не введена, переходим к вводу ячейки
                    updateState { it.copy(
                        scanningState = ScanningState.SCAN_BIN,
                        currentScanHint = getHintForState(ScanningState.SCAN_BIN)
                    ) }
                } else {
                    // Если ячейка уже введена, переходим к подтверждению
                    updateState { it.copy(
                        scanningState = ScanningState.CONFIRM,
                        currentScanHint = getHintForState(ScanningState.CONFIRM)
                    ) }
                }
            } else {
                // Если сначала ячейка, переходим к подтверждению
                updateState { it.copy(
                    scanningState = ScanningState.CONFIRM,
                    currentScanHint = getHintForState(ScanningState.CONFIRM)
                ) }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error parsing quantity")
            sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка ввода количества"))
        }
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