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
import com.synngate.synnframe.presentation.ui.tasks.model.EntryStep
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
import kotlinx.coroutines.delay
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

    fun handleSelectedProductById(productId: String) {
        launchIO {
            Timber.d("Loading product by ID: $productId")
            val product = productUseCases.getProductById(productId)
            if (product != null) {
                Timber.d("Product found: ${product.name}, handling selection")
                // Если продукт найден - обрабатываем его
                handleSelectedProduct(product)
            } else {
                Timber.d("Product not found by ID: $productId")
                sendEvent(TaskDetailEvent.ShowSnackbar("Товар не найден"))
            }
        }
    }

    fun navigateBack() {
        sendEvent(TaskDetailEvent.NavigateBack)
    }

    fun showProductSelectionDialog() {
        launchIO {
            updateState { it.copy(isProductsLoading = true) }

            try {
                // Загружаем список товаров для отображения
                val products = loadProductsForSelection()

                // Проверяем, требуется ли ограничение по товарам из плана
                val task = uiState.value.task

                // Получаем идентификаторы товаров из плана, если задание имеет план
                // и ограничено выбором только из плана
                val planProductIds = if (task != null &&
                    task.taskType?.factLineActions?.any {
                        it.type == FactLineActionType.ENTER_PRODUCT_FROM_PLAN
                    } == true) {
                    task.planLines.map { it.productId }.toSet()
                } else {
                    null // Нет ограничений, можно выбрать любой товар
                }

                updateState { state ->
                    state.copy(
                        isProductSelectionDialogVisible = true,
                        filteredProducts = products,
                        productSelectionFilter = "",
                        isProductsLoading = false,
                        planProductIds = planProductIds // Сохраняем идентификаторы товаров из плана
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка загрузки товаров для диалога")
                updateState { it.copy(isProductsLoading = false) }
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка загрузки товаров: ${e.message}"))
            }
        }
    }

    // Метод для скрытия диалога выбора товара
    fun hideProductSelectionDialog() {
        updateState { it.copy(
            isProductSelectionDialogVisible = false,
            productSelectionFilter = "",
            filteredProducts = emptyList(),
            planProductIds = null
        )}
    }

    // Метод для обновления фильтра товаров
    fun updateProductFilter(filter: String) {
        updateState { it.copy(productSelectionFilter = filter) }

        // Применяем фильтр с задержкой для избежания частых запросов при быстром вводе
        launchIO {
            delay(300) // Задержка в 300 мс

            // Проверяем, что фильтр не изменился за время задержки
            if (uiState.value.productSelectionFilter == filter) {
                filterProducts(filter)
            }
        }
    }

    // Метод для фильтрации товаров по имени или штрихкоду
    private suspend fun filterProducts(filter: String) {
        try {
            updateState { it.copy(isProductsLoading = true) }

            var products = if (filter.isBlank()) {
                // Если фильтр пуст, загружаем общий список товаров
                loadProductsForSelection()
            } else {
                // Сначала пробуем найти по имени или артикулу
                val byNameOrArticle = productUseCases.getProductsByNameFilter(filter).first()

                // Если не нашли достаточно товаров, пробуем поискать по штрихкоду
                if (byNameOrArticle.size < 5 && filter.length >= 3) {
                    // Просматриваем все товары и ищем совпадения в штрихкодах
                    val allProducts = productUseCases.getProducts().first()
                    val byBarcode = allProducts.filter { product ->
                        product.units.any { unit ->
                            unit.mainBarcode.contains(filter, ignoreCase = true) ||
                                    unit.barcodes.any { it.contains(filter, ignoreCase = true) }
                        }
                    }

                    // Объединяем результаты и удаляем дубликаты
                    (byNameOrArticle + byBarcode).distinctBy { it.id }
                } else {
                    byNameOrArticle
                }
            }

            // Применяем фильтр по плану, если нужно
            val planProductIds = uiState.value.planProductIds
            if (planProductIds != null) {
                products = products.filter { product -> planProductIds.contains(product.id) }
            }

            updateState { it.copy(
                filteredProducts = products,
                isProductsLoading = false
            )}
        } catch (e: Exception) {
            Timber.e(e, "Ошибка фильтрации товаров")
            updateState { it.copy(isProductsLoading = false) }
        }
    }

    // Вспомогательный метод для загрузки товаров
    private suspend fun loadProductsForSelection(): List<Product> {
        // Загружаем актуальный список товаров
        return productUseCases.getProducts().first()
    }

    fun closeDialog() {
        Timber.d("Closing dialog")
        updateState {
            it.copy(
                isScanDialogVisible = false,
                isFactLineDialogVisible = false,
                isCompleteConfirmationVisible = false,
                selectedFactLine = null,
                factLineDialogState = FactLineDialogState()
            )
        }
    }

    fun startFactLineEntry() {
        val task = uiState.value.task ?: return
        val taskType = task.taskType ?: return

        // Если нет действий для строки факта, используем стандартный подход
        if (taskType.factLineActions.isEmpty()) {
            updateState { it.copy(
                isEntryActive = true,
                entryStep = EntryStep.ENTER_BIN,
                entryBinCode = null,
                entryBinName = null,
                entryProduct = null,
                entryQuantity = null
            )}
            return
        }

        // Определяем последовательность шагов ввода на основе настроек задания
        val entrySequence = mutableListOf<EntryStep>()

        taskType.factLineActions.sortedBy { it.order }.forEach { action ->
            when (action.type) {
                FactLineActionType.ENTER_BIN_ANY,
                FactLineActionType.ENTER_BIN_FROM_PLAN -> entrySequence.add(EntryStep.ENTER_BIN)

                FactLineActionType.ENTER_PRODUCT_ANY,
                FactLineActionType.ENTER_PRODUCT_FROM_PLAN -> entrySequence.add(EntryStep.ENTER_PRODUCT)

                FactLineActionType.ENTER_QUANTITY -> entrySequence.add(EntryStep.ENTER_QUANTITY)

                else -> {} // Другие типы действий пропускаем
            }
        }

        // Начинаем новый цикл с первого действия
        if (entrySequence.isNotEmpty()) {
            updateState { it.copy(
                isEntryActive = true,
                entrySequence = entrySequence,
                entryStepIndex = 0,
                entryStep = entrySequence.first(),
                entryBinCode = null,
                entryBinName = null,
                entryProduct = null,
                entryQuantity = null
            )}
        }
    }

    // Обработка сканированного штрихкода
    fun processScanResult(barcode: String) {
        Timber.d("Processing scan result: $barcode for step: ${uiState.value.entryStep}")
        when (uiState.value.entryStep) {
            EntryStep.ENTER_BIN -> processBinCode(barcode)
            EntryStep.ENTER_PRODUCT -> processProductBarcode(barcode)
            EntryStep.ENTER_QUANTITY -> processQuantity(barcode)
            EntryStep.NONE -> { /* Ничего не делаем */ }
        }
    }

    fun processScanDialogResult(barcode: String) {
        Timber.d("Processing scan dialog result: $barcode for step: ${uiState.value.entryStep}")

        // Закрываем диалог сканирования сразу
        updateState { it.copy(
            isScanDialogVisible = false,
            scanBarcodeDialogState = it.scanBarcodeDialogState.copy(
                scannerMessage = null,
                isScannerActive = false
            )
        )}

        // Обработка в зависимости от текущего шага ввода
        when (uiState.value.entryStep) {
            EntryStep.ENTER_BIN -> {
                processBinCodeWithDelay(barcode)
            }
            EntryStep.ENTER_PRODUCT -> {
                processProductBarcodeOnly(barcode)
            }
            else -> {
                sendEvent(TaskDetailEvent.ShowSnackbar("Неожиданный этап ввода для сканирования"))
            }
        }
    }

    // Метод для обработки ячейки с задержкой перед сменой состояния
    private fun processBinCodeWithDelay(barcode: String) {
        launchIO {
            try {
                // Валидация кода ячейки
                val isValid = binValidator?.isValidBin(barcode) ?: true

                if (isValid) {
                    // Форматирование имени ячейки
                    val binName = binFormatter?.formatBinName(barcode) ?: barcode

                    // Обновляем состояние с ячейкой но НЕ меняем шаг пока
                    updateState { it.copy(
                        entryBinCode = barcode,
                        entryBinName = binName
                    )}

                    // Звуковое подтверждение успеха
                    soundService.playSuccessSound()

                    // Добавляем задержку перед сменой состояния
                    delay(500) // Задержка в 500 мс

                    // Теперь меняем шаг
                    updateState { it.copy(entryStep = EntryStep.ENTER_PRODUCT) }
                } else {
                    // Сообщение об ошибке
                    sendEvent(TaskDetailEvent.ShowSnackbar("Неверный формат ячейки"))
                    soundService.playErrorSound()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing bin code: ${e.message}")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка обработки ячейки: ${e.message}"))
                soundService.playErrorSound()
            }
        }
    }

    // Метод только для обработки штрихкода товара
    private fun processProductBarcodeOnly(barcode: String) {
        launchIO {
            try {
                // Поиск товара по штрихкоду
                val product = productUseCases.findProductByBarcode(barcode)

                if (product != null) {
                    Timber.d("Product found: ${product.name}")

                    // Если товар найден - сохраняем его
                    updateState { it.copy(
                        entryProduct = product,
                        entryStep = EntryStep.ENTER_QUANTITY
                    )}

                    // Показываем диалог ввода количества
                    showQuantityInputDialog()

                    // Звуковое подтверждение успеха
                    soundService.playSuccessSound()
                } else {
                    Timber.d("Product not found for barcode: $barcode")
                    sendEvent(TaskDetailEvent.ShowSnackbar("Товар не найден по штрихкоду: $barcode"))
                    soundService.playErrorSound()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing product barcode")
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
                soundService.playErrorSound()
            }
        }
    }

    // Обработка введенной ячейки
// Обработка введенной ячейки
    private fun processBinCode(code: String) {
        launchIO {
            Timber.d("Processing bin code: $code")
            updateState { it.copy(isProcessing = true) }

            try {
                // Валидация кода ячейки
                val isValid = binValidator?.isValidBin(code) ?: true

                if (isValid) {
                    // Форматирование имени ячейки
                    val binName = binFormatter?.formatBinName(code) ?: code

                    // Обновление состояния
                    updateState { it.copy(
                        entryBinCode = code,
                        entryBinName = binName,
                        isProcessing = false
                    )}

                    // Переход к следующему шагу
                    moveToNextStep()

                    // Звуковое подтверждение успеха
                    soundService.playSuccessSound()
                } else {
                    // Сообщение об ошибке
                    updateState { it.copy(isProcessing = false) }
                    sendEvent(TaskDetailEvent.ShowSnackbar("Неверный формат ячейки"))
                    soundService.playErrorSound()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing bin code: ${e.message}")
                updateState { it.copy(isProcessing = false) }
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка обработки ячейки: ${e.message}"))
                soundService.playErrorSound()
            }
        }
    }

    private fun processProductBarcode(barcode: String) {
        launchIO {
            Timber.d("Processing product barcode: $barcode")
            // Отключаем принудительный переход к вводу количества
            updateState { it.copy(isProcessing = true) }

            try {
                // Поиск товара по штрихкоду
                val product = productUseCases.findProductByBarcode(barcode)

                if (product != null) {
                    Timber.d("Product found: ${product.name}")
                    // Если товар найден - сохраняем его
                    updateState { it.copy(
                        entryProduct = product,
                        entryStep = EntryStep.ENTER_QUANTITY,
                        isProcessing = false
                    )}

                    // Показываем диалог ввода количества
                    showQuantityInputDialog()

                    // Звуковое подтверждение успеха
                    soundService.playSuccessSound()
                } else {
                    Timber.d("Product not found for barcode: $barcode")
                    // Сообщение об ошибке
                    updateState { it.copy(isProcessing = false) }
                    sendEvent(TaskDetailEvent.ShowSnackbar("Товар не найден"))
                    soundService.playErrorSound()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing product barcode")
                updateState { it.copy(isProcessing = false) }
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: ${e.message}"))
                soundService.playErrorSound()
            }
        }
    }

    // Обработка введенного количества
    private fun processQuantity(quantityStr: String) {
        try {
            val quantity = quantityStr.toFloatOrNull()

            if (quantity != null && quantity > 0) {
                // Сохраняем количество
                updateState { it.copy(entryQuantity = quantity) }

                // Завершаем ввод строки факта
                completeFactLineEntry()

                // Звуковое подтверждение успеха
                soundService.playSuccessSound()
            } else {
                // Сообщение об ошибке
                sendEvent(TaskDetailEvent.ShowSnackbar("Введите положительное число"))
                soundService.playErrorSound()
            }
        } catch (e: Exception) {
            sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка ввода количества"))
            soundService.playErrorSound()
        }
    }

    private fun moveToNextStep() {
        val state = uiState.value
        val entrySequence = state.entrySequence

        Timber.d("Moving to next step from ${state.entryStep}")

        // Если последовательность пуста, используем стандартный подход
        if (entrySequence.isEmpty()) {
            val currentStep = state.entryStep
            val nextStep = when (currentStep) {
                EntryStep.ENTER_BIN -> EntryStep.ENTER_PRODUCT
                EntryStep.ENTER_PRODUCT -> EntryStep.ENTER_QUANTITY
                EntryStep.ENTER_QUANTITY -> EntryStep.NONE
                EntryStep.NONE -> EntryStep.NONE
            }

            Timber.d("Moving from step $currentStep to $nextStep (standard approach)")

            // Обновляем состояние шага
            updateState { it.copy(entryStep = nextStep) }
        } else {
            // Используем последовательность действий из настроек
            val currentIndex = state.entryStepIndex

            if (currentIndex < entrySequence.size - 1) {
                // Переходим к следующему шагу в последовательности
                val nextIndex = currentIndex + 1
                val nextStep = entrySequence[nextIndex]

                Timber.d("Moving from step ${entrySequence[currentIndex]} to $nextStep (index $nextIndex)")

                updateState { it.copy(
                    entryStep = nextStep,
                    entryStepIndex = nextIndex
                )}
            } else {
                // Это был последний шаг
                Timber.d("Reached end of entry sequence")

                updateState { it.copy(
                    entryStep = EntryStep.NONE,
                    entryStepIndex = 0
                )}
            }
        }

        // Если следующий шаг - ввод количества и у нас есть выбранный товар
        // Важно: проверяем обновленное состояние, а не предыдущее!
        val updatedState = uiState.value
        if (updatedState.entryStep == EntryStep.ENTER_QUANTITY && updatedState.entryProduct != null) {
            Timber.d("Next step is quantity input and product is selected, showing dialog")
            showQuantityInputDialog()
        } else if (updatedState.entryStep == EntryStep.NONE && canCompleteEntry()) {
            Timber.d("Entry process complete, saving fact line")
            completeFactLineEntry()
        }

        // Очищаем поле ввода при переходе к следующему шагу
        updateState { it.copy(searchQuery = "") }
    }
    // Улучшенный диалог ввода количества
    private fun showQuantityInputDialog() {
        val state = uiState.value
        val product = state.entryProduct

        if (product == null) {
            Timber.e("Cannot show quantity dialog: no product selected")
            sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: товар не выбран"))
            return
        }

        Timber.d("Opening quantity input dialog for product: ${product.name}")

        // Получаем ID задания
        val taskId = state.task?.id
        if (taskId == null) {
            Timber.e("Cannot show quantity dialog: no task ID")
            sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка: ID задания отсутствует"))
            return
        }

        // Проверяем, существует ли уже строка факта для этого товара
        val existingFactLine = state.task.factLines.find { it.productId == product.id }

        val factLine = existingFactLine ?: TaskFactLine(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            productId = product.id,
            quantity = 0f,
            binCode = state.entryBinCode
        )

        // Находим плановое количество, если есть
        val planLine = state.task.planLines.find { it.productId == product.id }
        val planQuantity = planLine?.quantity ?: 0f

        // Показываем диалог с гарантированно пустым значением в поле ввода
        updateState { it.copy(
            selectedFactLine = factLine,
            isFactLineDialogVisible = true,
            factLineDialogState = FactLineDialogState(
                additionalQuantity = "",  // Гарантированно пустая строка
                isError = false
            ),
            selectedPlanQuantity = planQuantity
        )}
    }

    // Проверка возможности завершения ввода
    private fun canCompleteEntry(): Boolean {
        val state = uiState.value
        return state.entryProduct != null && state.entryQuantity != null && state.entryQuantity > 0
    }

    private fun completeFactLineEntry() {
        val state = uiState.value

        if (!canCompleteEntry()) {
            Timber.d("Cannot complete entry: Product or quantity is missing")
            return
        }

        launchIO {
            updateState { it.copy(isProcessing = true) }

            try {
                val product = state.entryProduct!!
                val quantity = state.entryQuantity!!
                val binCode = state.entryBinCode

                val taskId = state.task?.id ?: return@launchIO

                // Проверяем, существует ли уже строка факта
                val existingFactLine = state.task.factLines.find { it.productId == product.id }

                val factLine = if (existingFactLine != null) {
                    // Обновляем существующую строку
                    existingFactLine.copy(
                        quantity = quantity,
                        binCode = binCode ?: existingFactLine.binCode
                    )
                } else {
                    // Создаем новую строку
                    TaskFactLine(
                        id = UUID.randomUUID().toString(),
                        taskId = taskId,
                        productId = product.id,
                        quantity = quantity,
                        binCode = binCode
                    )
                }

                // Сохраняем строку факта
                taskUseCases.updateTaskFactLine(factLine)

                // Воспроизводим звук успеха
                soundService.playSuccessSound()

                // Обновляем задание
                loadTask()

                // Полный сброс состояния ввода
                resetEntryState()

                // Уведомляем о успешном сохранении
                sendEvent(TaskDetailEvent.UpdateSuccess)

            } catch (e: Exception) {
                Timber.e(e, "Error saving fact line: ${e.message}")
                updateState { it.copy(isProcessing = false) }
                sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка сохранения: ${e.message}"))
                soundService.playErrorSound()
            }
        }

        resetEntryState()
    }

    // Отмена ввода строки факта
    fun cancelFactLineEntry() {
        resetEntryState()
    }

    // Обработка ввода из поля ввода
    fun onSearchQueryChanged(query: String) {
        updateState { it.copy(searchQuery = query) }

        // Обработка специальных команд
        when (query) {
            "0" -> {
                showProductSelectionDialog()
                updateState { it.copy(searchQuery = "") }
            }
            // Если достаточно длинная строка, обрабатываем как штрихкод
            else -> if (query.length >= MIN_BARCODE_LENGTH) {
                processScanResult(query)
                // После обработки очищаем поле ввода
                updateState { it.copy(searchQuery = "") }
            }
        }
    }

    // Обработка выбора товара из списка
    fun handleSelectedProduct(product: Product) {
        Timber.d("Selected product: ${product.name}")
        // Сохраняем выбранный товар и устанавливаем следующий шаг
        updateState { it.copy(
            entryProduct = product,
            entryStep = EntryStep.ENTER_QUANTITY,
            searchQuery = ""
        )}

        // Показываем диалог ввода количества
        showQuantityInputDialog()
    }

    // Обработчик изменения значения в поле ввода количества
    fun onQuantityChange(value: String) {
        updateState { it.copy(
            factLineDialogState = it.factLineDialogState.copy(
                additionalQuantity = value,
                isError = false
            )
        )}
    }

    // Обработчик ошибки в поле ввода количества
    fun onQuantityError(isError: Boolean) {
        updateState { it.copy(
            factLineDialogState = it.factLineDialogState.copy(
                isError = isError
            )
        )}
    }

    // Обработчик применения нового количества
    fun applyQuantityChange(factLine: TaskFactLine, additionalQuantity: String) {
        try {
            val addValue = additionalQuantity.toFloatOrNull() ?: 0f
            if (addValue == 0f) {
                updateState { it.copy(
                    factLineDialogState = it.factLineDialogState.copy(
                        isError = true
                    )
                )}
                return
            }

            // Рассчитываем новое количество
            val updatedQuantity = if (factLine.quantity > 0) {
                factLine.quantity + addValue
            } else {
                addValue
            }

            // Обновляем временное значение количества
            updateState { it.copy(
                entryQuantity = updatedQuantity
            )}

            // Закрываем диалог
            closeDialog()

            // Сохраняем строку факта
            completeFactLineEntry()
        } catch (e: Exception) {
            Timber.e(e, "Error applying quantity change")
            updateState { it.copy(
                factLineDialogState = it.factLineDialogState.copy(
                    isError = true
                )
            )}
        }
    }

    fun showScanDialog() {
        updateState { it.copy(isScanDialogVisible = true) }
    }

    /**
     * Метод для обновления состояния активности сканера
     */
    fun updateScannerActiveState(active: Boolean) {
        updateState { it.copy(
            scanBarcodeDialogState = it.scanBarcodeDialogState.copy(
                isScannerActive = active
            )
        )}
    }

    // Полный сброс состояния ввода
    private fun resetEntryState() {
        Timber.d("Fully resetting entry state")
        updateState { it.copy(
            isEntryActive = false,
            entryStep = EntryStep.NONE,
            entryBinCode = null,
            entryBinName = null,
            entryProduct = null,
            entryQuantity = null,
            searchQuery = "",
            isProcessing = false,
            selectedFactLine = null,
            factLineDialogState = FactLineDialogState(),
            scanBarcodeDialogState = ScanBarcodeDialogState(),
            isScanDialogVisible = false,
            isFactLineDialogVisible = false
        )}
    }
}