package com.synngate.synnframe.presentation.ui.tasks

import com.synngate.synnframe.domain.entity.Product
import com.synngate.synnframe.domain.entity.TaskFactLine
import com.synngate.synnframe.domain.entity.TaskPlanLine
import com.synngate.synnframe.domain.entity.TaskStatus
import com.synngate.synnframe.domain.service.SoundService
import com.synngate.synnframe.domain.usecase.product.ProductUseCases
import com.synngate.synnframe.domain.usecase.task.TaskUseCases
import com.synngate.synnframe.domain.usecase.user.UserUseCases
import com.synngate.synnframe.presentation.ui.products.components.ScanResult
import com.synngate.synnframe.presentation.ui.tasks.model.FactLineDialogState
import com.synngate.synnframe.presentation.ui.tasks.model.ScanBarcodeDialogState
import com.synngate.synnframe.presentation.ui.tasks.model.TaskDetailEvent
import com.synngate.synnframe.presentation.ui.tasks.model.TaskDetailState
import com.synngate.synnframe.presentation.ui.tasks.model.TaskLineItem
import com.synngate.synnframe.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.UUID

class TaskDetailViewModel(
    private val taskId: String,
    private val taskUseCases: TaskUseCases,
    private val productUseCases: ProductUseCases,
    private val userUseCases: UserUseCases,
    private val soundService: SoundService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<TaskDetailState, TaskDetailEvent>(TaskDetailState(taskId = taskId)) {

    private val scannedBarcodeCache = mutableMapOf<String, Product?>()

    init {
        loadTask()
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

                        taskLines.add(
                            TaskLineItem(
                                planLine = planLine,
                                factLine = factLine,
                                product = product
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

    // Добавьте следующие методы и измените существующие методы в TaskDetailViewModel.kt

    /**
     * Обрабатывает результат сканирования штрихкода
     */
    fun processScanResult(barcode: String) {
        // Зафиксируем время начала сканирования для расчета длительности операции
        val startTime = System.currentTimeMillis()

        launchIO {
            updateState { it.copy(isProcessing = true, scannedBarcode = barcode) }

            try {
                // Проверяем кэш перед поиском в базе
                val product = if (scannedBarcodeCache.containsKey(barcode)) {
                    // Используем кэшированное значение
                    scannedBarcodeCache[barcode]
                } else {
                    // Ищем товар по штрихкоду и кэшируем результат
                    val foundProduct = productUseCases.findProductByBarcode(barcode)
                    scannedBarcodeCache[barcode] = foundProduct
                    foundProduct
                }

                if (product != null) {
                    val task = uiState.value.task
                    if (task == null) {
                        updateState { state ->
                            state.copy(
                                scanBarcodeDialogState = state.scanBarcodeDialogState.copy(
                                    scannerMessage = "Задание не найдено"
                                ),
                                isProcessing = false
                            )
                        }
                        sendEvent(TaskDetailEvent.ShowSnackbar("Задание не найдено"))
                        return@launchIO
                    }

                    // Проверяем, есть ли товар в плане задания, или разрешено использовать товары не из плана
                    val isInPlan = task.isProductInPlan(product.id)
                    val canUseProductNotInPlan = task.allowProductsNotInPlan

                    if (isInPlan || canUseProductNotInPlan) {
                        // Находим или создаем строку факта
                        val factLine = task.getFactLineByProductId(product.id)
                            ?: TaskFactLine(
                                id = UUID.randomUUID().toString(),
                                taskId = task.id,
                                productId = product.id,
                                quantity = 0f
                            )

                        // Находим плановое количество для этого товара
                        val planLine = task.planLines.find { it.productId == product.id }
                        val planQuantity = planLine?.quantity ?: 0f

                        // Закрываем диалог сканирования
                        updateState { state ->
                            state.copy(
                                isScanDialogVisible = false,
                                isProcessing = false
                            )
                        }

                        // Открываем диалог ввода количества
                        updateState { state ->
                            state.copy(
                                scannedProduct = product,
                                selectedFactLine = factLine,
                                isFactLineDialogVisible = true,
                                selectedPlanQuantity = planQuantity
                            )
                        }

                        // Звуковое оповещение об успешном сканировании
                        soundService.playSuccessSound()
                    } else {
                        // Если товара нет в плане и не разрешено использовать товары не из плана,
                        // показываем сообщение в диалоге сканирования
                        updateState { state ->
                            state.copy(
                                scanBarcodeDialogState = state.scanBarcodeDialogState.copy(
                                    scannerMessage = "Товар '${product.name}' не входит в план задания"
                                ),
                                isProcessing = false
                            )
                        }

                        // Звуковое оповещение об ошибке
                        soundService.playErrorSound()
                    }
                } else {
                    // Если товар не найден, показываем сообщение в диалоге сканирования
                    updateState { state ->
                        state.copy(
                            scanBarcodeDialogState = state.scanBarcodeDialogState.copy(
                                scannerMessage = "Товар со штрихкодом '$barcode' не найден"
                            ),
                            isProcessing = false
                        )
                    }

                    // Звуковое оповещение об ошибке
                    soundService.playErrorSound()
                }

                // Логируем длительность операции сканирования для аналитики
                val duration = System.currentTimeMillis() - startTime
                Timber.d("Scan processing took $duration ms")

            } catch (e: Exception) {
                Timber.e(e, "Error processing barcode")
                updateState { state ->
                    state.copy(
                        scanBarcodeDialogState = state.scanBarcodeDialogState.copy(
                            scannerMessage = "Ошибка: ${e.message}"
                        ),
                        isProcessing = false
                    )
                }
            }
        }
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
        val task = uiState.value.task ?: return

        try {
            val addValue = additionalQuantity.toFloatOrNull() ?: 0f
            if (addValue == 0f) {
                return
            }

            val updatedQuantity = factLine.quantity + addValue
            val updatedFactLine = factLine.copy(quantity = updatedQuantity)

            // Сразу закрываем диалог, не дожидаясь результата асинхронной операции
            closeDialog()

            launchIO {
                try {
                    taskUseCases.updateTaskFactLine(updatedFactLine)

                    // Обновляем задание
                    loadTask()

                    sendEvent(TaskDetailEvent.UpdateSuccess)
                } catch (e: Exception) {
                    Timber.e(e, "Error updating task fact line")
                    sendEvent(TaskDetailEvent.ShowSnackbar("Ошибка обновления: ${e.message}"))
                }
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
}